package outwatch.dom.helpers

import monix.execution.Ack.Continue
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import monix.reactive.observers.Subscriber
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
  private def createDataObject(modifiers: SeparatedModifiers, vNodeNS: js.UndefOr[String]): DataObject =
    DataObject(
      modifiers.attrs, modifiers.props, modifiers.styles, modifiers.emitters,
      Hooks(js.undefined, modifiers.insertHook, modifiers.prePatchHook, modifiers.updateHook, modifiers.postPatchHook, modifiers.destroyHook),
      modifiers.keyOption, vNodeNS)

  private def createProxy(modifiers: SeparatedModifiers, nodeType: String, vNodeId: Int, vNodeNS: js.UndefOr[String])(implicit scheduler: Scheduler): VNodeProxy = {
    val dataObject = createDataObject(modifiers, vNodeNS)

    VNodeProxy(
      sel = nodeType,
      data = dataObject,
      children = modifiers.proxies,
      key = modifiers.keyOption,
      outwatchId = vNodeId,
      outwatchDomUnmountHook = modifiers.domUnmountHook
    )
  }

  private[outwatch] def toSnabbdom(thunkNode: ThunkVNode[_])(implicit scheduler: Scheduler): VNodeProxy = {
    thunk(thunkNode.node.nodeType, thunkNode.renderFn, js.Array(thunkNode.argument))
  }

  private[outwatch] def toSnabbdom(node: VNode)(implicit scheduler: Scheduler): VNodeProxy = {
    val streamableModifiers = NativeModifiers.from(node.modifiers)
    val vNodeId = streamableModifiers.##
    val vNodeNS = node match {
      case _: SvgVNode => "http://www.w3.org/2000/svg": js.UndefOr[String]
      case _ => js.undefined
    }

    // if there is streamable content, we update the initial proxy with
    // subscribe and unsubscribe callbakcs.  additionally we update it with the
    // initial state of the obseravbles.
    streamableModifiers.observable.fold {
      createProxy(SeparatedModifiers.from(streamableModifiers.modifiers), node.nodeType, vNodeId, vNodeNS)
    } { observable =>
      // needs var for forward referencing
      var proxy: VNodeProxy = null

      def subscribe(): Cancelable = {
        observable.unsafeSubscribeFn(Sink.create[js.Array[StaticVDomModifier]](
          { newState =>
            // update the current proxy with the new state
            val newProxy = createProxy(SeparatedModifiers.from(newState), node.nodeType, vNodeId, vNodeNS)

            // call the snabbdom patch method and get the resulting proxy
            OutwatchTracing.patchSubject.onNext(newProxy)
            val currentProxy = patch(proxy, newProxy)

            // we are mutating the initial proxy, because parents of this node have a reference to this proxy.
            // if we are changing the content of this proxy via a stream, the parent will not see this change.
            // if now the parent is rerendered because a sibiling of the parent triggers an update, the parent
            // renders its children again. But it would not have the correct state of this proxy. Therefore,
            // we mutate the initial proxy and thereby mutate the proxy the parent knows.
            proxy.asInstanceOf[js.Dynamic].sel = currentProxy.sel
            proxy.asInstanceOf[js.Dynamic].data = currentProxy.data
            proxy.asInstanceOf[js.Dynamic].children = currentProxy.children
            proxy.asInstanceOf[js.Dynamic].elm = currentProxy.elm
            proxy.asInstanceOf[js.Dynamic].text = currentProxy.text
            proxy.asInstanceOf[js.Dynamic].key = currentProxy.key.asInstanceOf[js.Any]
            proxy.asInstanceOf[js.Dynamic].outwatchId = currentProxy.outwatchId
            proxy.asInstanceOf[js.Dynamic].outwatchDomUnmountHook = currentProxy.outwatchDomUnmountHook
            proxy.asInstanceOf[js.Dynamic].listener = currentProxy.asInstanceOf[js.Dynamic].listener

            Continue
          },
          error => dom.console.error(error.getMessage + "\n" + error.getStackTrace.mkString("\n"))
        ))
      }

      // hooks for subscribing and unsubscribing the streamable content
      var cancelable: Cancelable = null
      streamableModifiers.modifiers += DomMountHook(_ => cancelable = subscribe())
      streamableModifiers.modifiers += DomUnmountHook(_ => cancelable.cancel())

      // create initial proxy, we want to apply the initial state of the
      // receivers to the node
      proxy = createProxy(SeparatedModifiers.from(streamableModifiers.modifiers), node.nodeType, vNodeId, vNodeNS)
      proxy
    }
  }
}
