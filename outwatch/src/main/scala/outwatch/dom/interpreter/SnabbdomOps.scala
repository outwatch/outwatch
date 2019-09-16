package outwatch.dom.interpreter

import outwatch.dom._
import outwatch.dom.helpers._
import outwatch.reactive._
import snabbdom._

import scala.scalajs.js

private[outwatch] object SnabbdomOps {
  // currently async patching is disabled, because it yields flickering render results.
  // We would like to evaluate whether async patching can make sense and whether some kind
  // sync/async batching like monix-scheduler makes sense for us.
  var asyncPatchEnabled = false

  @inline private def createDataObject(modifiers: SeparatedModifiers, vNodeNS: js.UndefOr[String]): DataObject =
    new DataObject {
      attrs = modifiers.attrs
      props = modifiers.props
      style = modifiers.styles
      on = modifiers.emitters
      hook = new Hooks {
        init = modifiers.initHook
        insert = modifiers.insertHook
        prepatch = modifiers.prePatchHook
        update = modifiers.updateHook
        postpatch = modifiers.postPatchHook
        destroy = modifiers.destroyHook
      }
      key = modifiers.keyOption
      ns = vNodeNS
    }

  private def createProxy(modifiers: SeparatedModifiers, nodeType: String, vNodeId: js.UndefOr[Int], vNodeNS: js.UndefOr[String]): VNodeProxy = {
    val dataObject = createDataObject(modifiers, vNodeNS)

    @inline def newProxy(childProxies: js.UndefOr[js.Array[VNodeProxy]], string: js.UndefOr[String]) = new VNodeProxy {
      sel = nodeType
      data = dataObject
      children = childProxies
      text = string
      key = modifiers.keyOption
      _id = vNodeId
      _unmount = modifiers.domUnmountHook
    }

    if (modifiers.hasOnlyTextChildren) {
      modifiers.proxies.fold(newProxy(js.undefined, js.undefined)) { proxies =>
        newProxy(js.undefined, proxies.foldLeft("")(_ + _.text))
      }
    } else newProxy(modifiers.proxies, js.undefined)
  }

   def getNamespace(node: BasicVNode): js.UndefOr[String] = node match {
    case _: SvgVNode => "http://www.w3.org/2000/svg": js.UndefOr[String]
    case _ => js.undefined
  }

   def toSnabbdom(node: VNode): VNodeProxy = node match {
     case node: BasicVNode =>
       toRawSnabbdomProxy(node)
     case node: ConditionalVNode =>
       thunk.conditional(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toRawSnabbdomProxy(node.baseNode(node.renderFn(), Key(node.key))), node.shouldRender)
     case node: ThunkVNode =>
       thunk(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toRawSnabbdomProxy(node.baseNode(node.renderFn(), Key(node.key))), node.arguments)
   }

   private val newNodeId: () => Int = {
     var vNodeIdCounter = 0
     () => {
      vNodeIdCounter += 1
      vNodeIdCounter
     }
   }

   type SetImmediate = js.Function1[js.Function0[Unit], Int]
   type ClearImmediate = js.Function1[Int, Unit]
   private val (setImmediateRef, clearImmediateRef): (SetImmediate, ClearImmediate) = {
      if (!js.isUndefined(js.Dynamic.global.setImmediate))
        (js.Dynamic.global.setImmediate.bind(js.Dynamic.global).asInstanceOf[SetImmediate], js.Dynamic.global.clearImmediate.bind(js.Dynamic.global).asInstanceOf[ClearImmediate])
      else
        (js.Dynamic.global.setTimeout.bind(js.Dynamic.global).asInstanceOf[SetImmediate], js.Dynamic.global.clearTimeout.bind(js.Dynamic.global).asInstanceOf[ClearImmediate])
    }

    // we are mutating the initial proxy with VNodeProxy.copyInto, because parents of this node have a reference to this proxy.
    // if we are changing the content of this proxy via a stream, the parent will not see this change.
    // if now the parent is rerendered because a sibiling of the parent triggers an update, the parent
    // renders its children again. But it would not have the correct state of this proxy. Therefore,
    // we mutate the initial proxy and thereby mutate the proxy the parent knows.
   private def toRawSnabbdomProxy(node: BasicVNode): VNodeProxy = {

    val vNodeNS = getNamespace(node)
    val vNodeId: Int = newNodeId()

    val nativeModifiers = NativeModifiers.from(node.modifiers)

    if (nativeModifiers.subscribables.isEmpty) {
      // if no dynamic/subscribable content, then just create a simple proxy
      createProxy(SeparatedModifiers.from(nativeModifiers.modifiers), node.nodeType, vNodeId, vNodeNS)
    } else {
      // if there is streamable content, we update the initial proxy with
      // in subscribe and unsubscribe callbacks. We subscribe and unsubscribe
      // based in dom events.

      var proxy: VNodeProxy = null
      var nextModifiers: js.UndefOr[js.Array[StaticVDomModifier]] = js.undefined
      var lastTimeout: Option[Int] = None
      var isActive: Boolean = false

      var patchIsRunning = false
      var patchIsNeeded = false

      def doPatch(): Unit = {
        patchIsRunning = true
        patchIsNeeded = false

        // update the current proxy with the new state
        val separatedModifiers = SeparatedModifiers.from(nativeModifiers.modifiers, nextModifiers)
        nextModifiers = separatedModifiers.nextModifiers
        val newProxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)
        newProxy._update = proxy._update
        newProxy._args = proxy._args

        // call the snabbdom patch method to update the dom
        OutwatchTracing.patchSubject.onNext(newProxy)
        patch(proxy, newProxy)

        patchIsRunning = false
        if (patchIsNeeded) invokeDoPatch(async = false)
      }

      def resetTimeout(): Unit = {
        lastTimeout.foreach(clearImmediateRef)
        lastTimeout = None
      }

      def asyncDoPatch(): Unit = {
        lastTimeout = Some(setImmediateRef(() => doPatch()))
      }

      def invokeDoPatch(async: Boolean): Unit = if (isActive) {
        resetTimeout()
        if (patchIsRunning) {
          patchIsNeeded = true
        } else {
          if (async) asyncDoPatch()
          else doPatch()
        }
      }

      val patchSink = SinkObserver.create[Unit](
        _ => invokeDoPatch(async = asyncPatchEnabled),
        OutwatchTracing.errorSubject.onNext
      )

      def start(): Unit = {
        resetTimeout()
        nativeModifiers.subscribables.foreach { subscribable =>
          subscribable.subscribe(patchSink)
        }
      }

      def stop(): Unit = {
        resetTimeout()
        nativeModifiers.subscribables.foreach(_.unsubscribe())
      }

      // move to one of the stream hooks
      // hooks for subscribing and unsubscribing the streamable content
      nativeModifiers.modifiers.push(InsertHook { p =>
        VNodeProxy.copyInto(p, proxy)
        isActive = true
        start()
      })
      nativeModifiers.modifiers.push(PostPatchHook { (o, p) =>
        VNodeProxy.copyInto(p, proxy)
        proxy._update.foreach(_(proxy))
        if (o._id != p._id) {
          isActive = true
          start()
        } else if (isActive) {
          start()
        } else {
          stop()
        }
      })
      nativeModifiers.modifiers.push(DomUnmountHook { _ =>
        isActive = false
        stop()
      })

      // premature subcription: We will now subscribe, eventhough the node is not yet mounted
      // but we try to get the initial values from the observables synchronously and that
      // is only possible if we subscribe before rendering.  Succeeding supscriptions will then
      // soley be handle by mount/unmount hooks.  And every node within this method is going to
      // be mounted one way or another and this method is guarded by an effect in the public api.
      start()

      // create initial proxy, we want to apply the initial state of the
      // receivers to the node
      val separatedModifiers = SeparatedModifiers.from(nativeModifiers.modifiers)
      nextModifiers = separatedModifiers.nextModifiers
      proxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)

      proxy
    }
  }
}
