package outwatch.interpreter

import outwatch._
import outwatch.helpers._
import colibri._
import snabbdom._

import scala.scalajs.js

private[outwatch] object SnabbdomOps {
  private val MicrotaskExecutor = scala.scalajs.concurrent.QueueExecutionContext.promises()

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

   def toSnabbdom(node: VNode, config: RenderConfig): VNodeProxy = node match {
     case node: BasicVNode =>
       toRawSnabbdomProxy(node, config)
     case node: ConditionalVNode =>
       thunk.conditional(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toRawSnabbdomProxy(node.baseNode(node.renderFn(), Key(node.key)), config), node.shouldRender)
     case node: ThunkVNode =>
       thunk(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toRawSnabbdomProxy(node.baseNode(node.renderFn(), Key(node.key)), config), node.arguments)
     case node: SyncEffectVNode =>
       toSnabbdom(node.unsafeRun(), config)
   }

   private val newNodeId: () => Int = {
     var vNodeIdCounter = 0
     () => {
      vNodeIdCounter += 1
      vNodeIdCounter
     }
   }

    // we are mutating the initial proxy with VNodeProxy.copyInto, because parents of this node have a reference to this proxy.
    // if we are changing the content of this proxy via a stream, the parent will not see this change.
    // if now the parent is rerendered because a sibiling of the parent triggers an update, the parent
    // renders its children again. But it would not have the correct state of this proxy. Therefore,
    // we mutate the initial proxy and thereby mutate the proxy the parent knows.
   private def toRawSnabbdomProxy(node: BasicVNode, config: RenderConfig): VNodeProxy = {

    val vNodeNS = getNamespace(node)
    val vNodeId: Int = newNodeId()

    val observer = new StatefulObserver[Unit]

    val nativeModifiers = NativeModifiers.from(node.modifiers, config, observer)

    if (nativeModifiers.subscribables.forall(_.isEmpty())) {
      // if no dynamic/subscribable content, then just create a simple proxy
      createProxy(SeparatedModifiers.from(nativeModifiers.modifiers), node.nodeType, vNodeId, vNodeNS)
    } else if (nativeModifiers.hasStream) {
      // if there is streamable content, we update the initial proxy with
      // in unsafeSubscribe and unsafeUnsubscribe callbacks. We unsafeSubscribe and unsafeUnsubscribe
      // based in dom events.

      var proxy: VNodeProxy = null
      var nextModifiers: js.UndefOr[js.Array[StaticVModifier]] = js.undefined
      var _prependModifiers: js.UndefOr[js.Array[StaticVModifier]] = js.undefined
      var isActive: Boolean = false

      var patchIsRunning = false

      val asyncCancelable = Cancelable.variable()

      def doPatch(): Unit = {
        patchIsRunning = true

        // update the current proxy with the new state
        val separatedModifiers = SeparatedModifiers.from(nativeModifiers.modifiers, prependModifiers = _prependModifiers, appendModifiers = nextModifiers)
        nextModifiers = separatedModifiers.nextModifiers
        val newProxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)
        newProxy._update = proxy._update
        newProxy._args = proxy._args

        // call the snabbdom patch method to update the dom
        OutwatchTracing.patchSubject.unsafeOnNext(newProxy)
        patch(proxy, newProxy)

        patchIsRunning = false
      }

      def cancelAsyncPatch(): Unit = {
        asyncCancelable.unsafeAddExisting(Cancelable.empty)
      }

      def asyncPatch(): Unit = if (isActive) {
        asyncCancelable.unsafeAdd { () =>
          var isCancel = false
          val cancelable = Cancelable(() => isCancel = true)
          MicrotaskExecutor.execute(() => if (!isCancel) doPatch())
          cancelable
        }
      }

      def start(): Unit = {
        cancelAsyncPatch()
        nativeModifiers.subscribables.foreach(_.unsafeSubscribe())
      }

      def stop(): Unit = {
        cancelAsyncPatch()
        nativeModifiers.subscribables.foreach(_.unsafeUnsubscribe())
      }

      // hooks for subscribing and unsubscribing the streamable content
      _prependModifiers = js.Array[StaticVModifier](
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

      // create initial proxy, we want to apply the initial state of the
      // receivers to the node
      val separatedModifiers = SeparatedModifiers.from(nativeModifiers.modifiers, prependModifiers = _prependModifiers)
      nextModifiers = separatedModifiers.nextModifiers
      proxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)

      // set the patch observer so on subscribable updates we get a patch call
      observer.set(Observer.create[Unit](
        _ => asyncPatch(),
        OutwatchTracing.errorSubject.unsafeOnNext
      ))

      proxy
    } else {
      // simpler version with only subscriptions, no streams.
      var isActive = true

      def start(): Unit = if (!isActive) {
        isActive = true
        nativeModifiers.subscribables.foreach(_.unsafeSubscribe())
      }

      def stop(): Unit = if (isActive) {
        isActive = false
        nativeModifiers.subscribables.foreach(_.unsafeUnsubscribe())
      }

      // hooks for subscribing and unsubscribing the streamable content
      val prependModifiers = js.Array[StaticVModifier](DomMountHook(_ => start()), DomUnmountHook(_ => stop()))

      // create the proxy from the modifiers
      val separatedModifiers = SeparatedModifiers.from(nativeModifiers.modifiers, prependModifiers = prependModifiers)
      createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)
    }
  }
}
