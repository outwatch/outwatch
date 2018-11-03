package outwatch.dom.helpers

import monix.execution.Ack.Continue
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom
import outwatch.dom._
import snabbdom._

import scala.scalajs.js

object OutwatchTracing {
  private[outwatch] val patchSubject = PublishSubject[VNodeProxy]()
  def patch: Observable[VNodeProxy] = patchSubject
}

object SnabbdomOps {
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

  @inline private def createProxy(modifiers: SeparatedModifiers, nodeType: String, vNodeId: Int, vNodeNS: js.UndefOr[String])(implicit scheduler: Scheduler): VNodeProxy = {
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

  private[outwatch] def getNamespace(node: BasicVNode): js.UndefOr[String] = node match {
    case _: SvgVNode => "http://www.w3.org/2000/svg": js.UndefOr[String]
    case _ => js.undefined
  }

  private[outwatch] def toSnabbdom(node: VNode)(implicit scheduler: Scheduler): VNodeProxy = node match {
    case node: BasicVNode => toSnabbdomProxy(node)
    case node: ConditionalVNode => toSnabbdomProxy(node)
    case node: ThunkVNode => toSnabbdomProxy(node)
  }

  @inline private def toSnabbdomProxy(node: ConditionalVNode)(implicit scheduler: Scheduler): VNodeProxy = {
    thunk.conditional(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toSnabbdomProxy(node.baseNode(node.renderFn())), node.shouldRender)
  }

  @inline private def toSnabbdomProxy(node: ThunkVNode)(implicit scheduler: Scheduler): VNodeProxy = {
    thunk(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toSnabbdomProxy(node.baseNode(node.renderFn())), node.arguments)
  }

  def toSnabbdomProxy(node: BasicVNode)(implicit scheduler: Scheduler): VNodeProxy = {
    val streamableModifiers = NativeModifiers.from(node.modifiers)
    val vNodeId = streamableModifiers.##
    val vNodeNS = getNamespace(node)

    // if there is streamable content, we update the initial proxy with
    // subscribe and unsubscribe callbakcs.  additionally we update it with the
    // initial state of the obseravbles.
    streamableModifiers.observable.fold {
      createProxy(SeparatedModifiers.from(streamableModifiers.modifiers), node.nodeType, vNodeId, vNodeNS)
    } { observable =>
      // needs var for forward referencing
      var proxy: VNodeProxy = null
      var nextModifiers: js.UndefOr[js.Array[StaticVDomModifier]] = null

      def subscribe(): Cancelable = {
        observable.unsafeSubscribeFn(Sink.create[js.Array[StaticVDomModifier]](
          { newState =>
            // update the current proxy with the new state
            val separatedModifiers = SeparatedModifiers.from(nextModifiers.fold(newState)(newState ++ _))
            nextModifiers = separatedModifiers.nextModifiers
            val newProxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)

            // call the snabbdom patch method and get the resulting proxy
            OutwatchTracing.patchSubject.onNext(newProxy)
            val currentProxy = patch(proxy, newProxy)

            // we are mutating the initial proxy, because parents of this node have a reference to this proxy.
            // if we are changing the content of this proxy via a stream, the parent will not see this change.
            // if now the parent is rerendered because a sibiling of the parent triggers an update, the parent
            // renders its children again. But it would not have the correct state of this proxy. Therefore,
            // we mutate the initial proxy and thereby mutate the proxy the parent knows.
            proxy.sel = currentProxy.sel
            proxy.data = currentProxy.data
            proxy.children = currentProxy.children
            proxy.elm = currentProxy.elm
            proxy.text = currentProxy.text
            proxy.key = currentProxy.key
            proxy._id = currentProxy._id
            proxy._unmount = currentProxy._unmount
            proxy.listener = currentProxy.listener

            Continue
          },
          error => dom.console.error(error.getMessage + "\n" + error.getStackTrace.mkString("\n"))
        ))
      }

      // hooks for subscribing and unsubscribing the streamable content
      var cancelable: Cancelable = null
      streamableModifiers.modifiers += InsertHook { p =>
        proxy.elm = p.elm
        cancelable = subscribe()
      }
      streamableModifiers.modifiers += PostPatchHook { (o, p) =>
        proxy.elm = p.elm
        if (o._id != p._id) {
          cancelable = subscribe()
        }
      }
      streamableModifiers.modifiers += DomUnmountHook(_ => cancelable.cancel())

      // create initial proxy, we want to apply the initial state of the
      // receivers to the node
      val separatedModifiers = SeparatedModifiers.from(streamableModifiers.modifiers)
      nextModifiers = separatedModifiers.nextModifiers
      proxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)
      proxy
    }
  }
}
