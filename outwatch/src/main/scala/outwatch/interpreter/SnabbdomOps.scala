package outwatch.interpreter

import outwatch._
import outwatch.helpers._
import colibri._
import snabbdom._

import scala.scalajs.js
import scala.annotation.tailrec

private[outwatch] object SnabbdomOps {
  // currently async patching is disabled, because it yields flickering render results.
  // We would like to evaluate whether async patching can make sense and whether some kind
  // sync/async batching like monix-scheduler makes sense for us.
  var asyncPatchEnabled = false

   @inline def toSnabbdom(node: RVNode[Any]): VNodeProxy = toSnabbdom[Any](node, ())
   def toSnabbdom[Env](node: RVNode[Env], env: Env): VNodeProxy = node match {
     case node: RBasicVNode[Env] =>
       toRawSnabbdomProxy(node, env)
     case node: RConditionalVNode[Env] =>
       thunk.conditional(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toRawSnabbdomProxy(node.baseNode(node.renderFn(), Key(node.key)), env), node.shouldRender)
     case node: RThunkVNode[Env] =>
       thunk(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toRawSnabbdomProxy(node.baseNode(node.renderFn(), Key(node.key)), env), node.arguments)
   }

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

   private def getNamespace(node: RBasicVNode[Nothing]): js.UndefOr[String] = node match {
    case _: RSvgVNode[_] => "http://www.w3.org/2000/svg": js.UndefOr[String]
    case _ => js.undefined
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
     if (js.typeOf(js.Dynamic.global.setImmediate) != "undefined")
        (js.Dynamic.global.setImmediate.bind(NativeHelpers.globalObject).asInstanceOf[SetImmediate], js.Dynamic.global.clearImmediate.bind(NativeHelpers.globalObject).asInstanceOf[ClearImmediate])
      else
        (js.Dynamic.global.setTimeout.bind(NativeHelpers.globalObject).asInstanceOf[SetImmediate], js.Dynamic.global.clearTimeout.bind(NativeHelpers.globalObject).asInstanceOf[ClearImmediate])
    }

    // we are mutating the initial proxy with VNodeProxy.copyInto, because parents of this node have a reference to this proxy.
    // if we are changing the content of this proxy via a stream, the parent will not see this change.
    // if now the parent is rerendered because a sibiling of the parent triggers an update, the parent
    // renders its children again. But it would not have the correct state of this proxy. Therefore,
    // we mutate the initial proxy and thereby mutate the proxy the parent knows.
   private def toRawSnabbdomProxy[Env](node: RBasicVNode[Env], env: Env): VNodeProxy = {

    val vNodeNS = getNamespace(node)
    val vNodeId: Int = newNodeId()

    val nativeModifiers = NativeModifiers.from(node.modifiers, env)

    if (nativeModifiers.subscribables.isEmpty) {
      // if no dynamic/subscribable content, then just create a simple proxy
      createProxy(SeparatedModifiers.from(nativeModifiers.modifiers), node.nodeType, vNodeId, vNodeNS)
    } else if (nativeModifiers.hasStream) {
      // if there is streamable content, we update the initial proxy with
      // in subscribe and unsubscribe callbacks. We subscribe and unsubscribe
      // based in dom events.

      var proxy: VNodeProxy = null
      var nextModifiers: js.UndefOr[js.Array[StaticModifier]] = js.undefined
      var _prependModifiers: js.UndefOr[js.Array[StaticModifier]] = js.undefined
      var lastTimeout: js.UndefOr[Int] = js.undefined
      var isActive: Boolean = false

      var patchIsRunning = false
      var patchIsNeeded = false

      @tailrec
      def doPatch(): Unit = {
        patchIsRunning = true
        patchIsNeeded = false

        // update the current proxy with the new state
        val separatedModifiers = SeparatedModifiers.from(nativeModifiers.modifiers, prependModifiers = _prependModifiers, appendModifiers = nextModifiers)
        nextModifiers = separatedModifiers.nextModifiers
        val newProxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)
        newProxy._update = proxy._update
        newProxy._args = proxy._args

        // call the snabbdom patch method to update the dom
        OutwatchTracing.patchSubject.onNext(newProxy)
        patch(proxy, newProxy)

        patchIsRunning = false
        if (patchIsNeeded) doPatch()
      }

      def resetTimeout(): Unit = {
        lastTimeout.foreach(clearImmediateRef)
        lastTimeout = js.undefined
      }

      def asyncDoPatch(): Unit = {
        resetTimeout()
        lastTimeout = setImmediateRef(() => doPatch())
      }

      def invokeDoPatch(async: Boolean): Unit = if (isActive) {
        if (patchIsRunning) {
          patchIsNeeded = true
        } else {
          if (async) asyncDoPatch()
          else doPatch()
        }
      }

      val patchSink = Observer.create[Unit](
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

      // hooks for subscribing and unsubscribing the streamable content
      _prependModifiers = js.Array[StaticModifier](
        InsertHook { p =>
          VNodeProxy.copyInto(p, proxy)
          isActive = true
          start()
        },
        PostPatchHook { (o, p) =>
          VNodeProxy.copyInto(p, proxy)
          proxy._update.foreach(_(proxy))
          if (!NativeModifiers.equalsVNodeIds(o._id, p._id)) {
            isActive = true
            start()
          } else if (isActive) {
            start()
          } else {
            stop()
          }
        },
        DomUnmountHook { _ =>
          isActive = false
          stop()
        }
      )

      // premature subcription: We will now subscribe, eventhough the node is not yet mounted
      // but we try to get the initial values from the observables synchronously and that
      // is only possible if we subscribe before rendering.  Succeeding supscriptions will then
      // soley be handle by mount/unmount hooks.  And every node within this method is going to
      // be mounted one way or another and this method is guarded by an effect in the public api.
      start()

      // create initial proxy, we want to apply the initial state of the
      // receivers to the node
      val separatedModifiers = SeparatedModifiers.from(nativeModifiers.modifiers, prependModifiers = _prependModifiers)
      nextModifiers = separatedModifiers.nextModifiers
      proxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)

      proxy
    } else {
      // simpler version with only subscriptions, no streams.
      val sink = Observer.empty
      var isActive = false

      def start(): Unit = if (!isActive) {
        isActive = true
        nativeModifiers.subscribables.foreach { subscribable =>
          subscribable.subscribe(sink)
        }
      }

      def stop(): Unit = if (isActive) {
        isActive = false
        nativeModifiers.subscribables.foreach(_.unsubscribe())
      }

      // hooks for subscribing and unsubscribing the streamable content
      val prependModifiers = js.Array[StaticModifier](
        InsertHook { _ =>
          start()
        },
        PostPatchHook { (o, p) =>
          if (!NativeModifiers.equalsVNodeIds(o._id, p._id)) {
            start()
          }
        },
        DomUnmountHook { _ =>
          stop()
        }
      )

      // premature subcription
      start()

      // create the proxy from the modifiers
      val separatedModifiers = SeparatedModifiers.from(nativeModifiers.modifiers, prependModifiers = prependModifiers)
      createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)
    }
  }
}
