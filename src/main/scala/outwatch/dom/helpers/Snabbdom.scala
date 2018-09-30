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
  private[outwatch] val patchSubject = PublishSubject[(VNodeProxy, VNodeProxy)]()
  def patch: Observable[(VNodeProxy, VNodeProxy)] = patchSubject
}

object SnabbdomOps {
  private def toOutwatchState(modifiers: SeparatedModifiers, vNodeId: Int): js.UndefOr[OutwatchState] =
    if (modifiers.usesOutwatchState || modifiers.domUnmountHook.nonEmpty) OutwatchState(vNodeId, modifiers.domUnmountHook)
    else js.undefined

  private def createDataObject(modifiers: SeparatedModifiers): DataObject =
    DataObject(
      modifiers.attrs, modifiers.props, modifiers.styles, modifiers.emitters,
      Hooks(modifiers.insertHook, modifiers.prePatchHook, modifiers.updateHook, modifiers.postPatchHook, modifiers.destroyHook),
      modifiers.keyOption
    )

  private def createProxy(modifiers: SeparatedModifiers, nodeType: String, vNodeId: Int)(implicit scheduler: Scheduler): VNodeProxy = {
    val dataObject = createDataObject(modifiers)
    val state = toOutwatchState(modifiers, vNodeId)

    val proxy = if (modifiers.proxies.isEmpty) {
      hFunction(nodeType, dataObject)
    } else {
      val copy = new js.Array[VNodeProxy]()
      copy ++= modifiers.proxies
      hFunction(nodeType, dataObject, copy)
    }

    proxy.outwatchState = state
    proxy
  }

  private def toSnabbdom(modifiersArray: js.Array[VDomModifier], nodeType: String)(implicit scheduler: Scheduler): VNodeProxy = {
    val streamableModifiers = NativeModifiers.from(modifiersArray)
    val vNodeId = streamableModifiers.hashCode()

    // if there is streamable content, we update the initial proxy with
    // subscribe and unsubscribe callbakcs.  additionally we update it with the
    // initial state of the obseravbles.
    if (streamableModifiers.observable.isEmpty) {
      createProxy(SeparatedModifiers.from(streamableModifiers.modifiers), nodeType, vNodeId)
    } else {
      // needs var for forward referencing
      var proxy: VNodeProxy = null

      def subscribe(): Cancelable = {
        var currentProxy: VNodeProxy = proxy
        streamableModifiers.observable.get.subscribe(
          { newState =>
            // update the current proxy with the new state
            val newProxy = createProxy(SeparatedModifiers.from(newState), nodeType, vNodeId)

            // call the snabbdom patch method and get the resulting proxy
            OutwatchTracing.patchSubject.onNext((currentProxy, newProxy))
            currentProxy = patch(currentProxy, newProxy)

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
            proxy.outwatchState = currentProxy.outwatchState
            proxy.asInstanceOf[js.Dynamic].listener = currentProxy.asInstanceOf[js.Dynamic].listener

            Continue
          },
          error => dom.console.error(error.getMessage + "\n" + error.getStackTrace.mkString("\n"))
        )
      }

      // hooks for subscribing and unsubscribing the streamable content
      val cancelable = new QueuedCancelable()
      streamableModifiers.modifiers += DomMountHook(_ => cancelable.enqueue(subscribe()))
      streamableModifiers.modifiers += DomUnmountHook(_ => cancelable.dequeue().cancel())

      // create initial proxy, we want to apply the initial state of the
      // receivers to the node
      proxy = createProxy(SeparatedModifiers.from(streamableModifiers.modifiers), nodeType, vNodeId)
      proxy
    }
  }

  private[outwatch] def toSnabbdom(vNode: VNode)(implicit scheduler: Scheduler): VNodeProxy = toSnabbdom(vNode.modifiers, vNode.nodeType)
}
