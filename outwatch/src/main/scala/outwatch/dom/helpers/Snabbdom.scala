package outwatch.dom.helpers

import monix.execution.Ack.Continue
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom
import outwatch.dom._
import snabbdom._

import scala.scalajs.js

object OutwatchTracing {
  private[outwatch] val patchSubject = PublishSubject[VNodeProxy]()
  def patch: Observable[VNodeProxy] = patchSubject

  private[outwatch] val errorSubject = PublishSubject[Throwable]()
  def error: Observable[Throwable] = errorSubject
}

private[outwatch] object SnabbdomOps {
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

  @inline private def createProxy(modifiers: SeparatedModifiers, nodeType: String, vNodeId: js.UndefOr[Int], vNodeNS: js.UndefOr[String])(implicit scheduler: Scheduler): VNodeProxy = {
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

    // we are mutating the initial proxy with VNodeProxy.copyInto, because parents of this node have a reference to this proxy.
    // if we are changing the content of this proxy via a stream, the parent will not see this change.
    // if now the parent is rerendered because a sibiling of the parent triggers an update, the parent
    // renders its children again. But it would not have the correct state of this proxy. Therefore,
    // we mutate the initial proxy and thereby mutate the proxy the parent knows.
   def toSnabbdom(node: VNode)(implicit scheduler: Scheduler): VNodeProxy = node match {
     case node: BasicVNode =>
       toRawSnabbdomProxy(node)
     case node: ConditionalVNode =>
       thunk.conditional(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toRawSnabbdomProxy(node.baseNode(node.renderFn(), Key(node.key))), node.shouldRender)
     case node: ThunkVNode =>
       thunk(getNamespace(node.baseNode), node.baseNode.nodeType, node.key, () => toRawSnabbdomProxy(node.baseNode(node.renderFn(), Key(node.key))), node.arguments)
   }

   private def toRawSnabbdomProxy(node: BasicVNode)(implicit scheduler: Scheduler): VNodeProxy = {
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
      var isActive = false

      def subscribe(): Cancelable = {
        observable.asyncBoundary(OverflowStrategy.Unbounded).unsafeSubscribeFn(Sink.create[js.Array[StaticVDomModifier]](
          { newState =>
            // First check whether we are active, i.e., our subscription is not cancelled.
            // The obvious question of the reader might be: But then it is already cancelled?
            // The answer: While this is true, it somehow happens in certain cases that eventhough we just cancelled the subcription, the observer is called one last time. The isActive flag prevents this. There is also a test assuring this, see OutwatchDomSpec "Nested VNode * outdated patch".
            if (isActive) {
              // update the current proxy with the new state
              val separatedModifiers = SeparatedModifiers.from(nextModifiers.fold(newState)(newState ++ _))
              nextModifiers = separatedModifiers.nextModifiers
              val newProxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)
              newProxy._update = proxy._update
              newProxy._args = proxy._args

              // call the snabbdom patch method and get the resulting proxy
              OutwatchTracing.patchSubject.onNext(newProxy)
              patch(proxy, newProxy)

              Ack.Continue
            } else Ack.Stop
          },
          { error =>
            OutwatchTracing.errorSubject.onNext(error)
            dom.console.error(error.getMessage + "\n" + error.getStackTrace.mkString("\n"))
          }
        ))
      }

      // hooks for subscribing and unsubscribing the streamable content
      var cancelable: Cancelable = null
      streamableModifiers.modifiers += InsertHook { p =>
        VNodeProxy.copyInto(p, proxy)
        isActive = true
        cancelable = subscribe()
      }
      streamableModifiers.modifiers += PostPatchHook { (o, p) =>
        VNodeProxy.copyInto(p, proxy)
        proxy._update.foreach(_(proxy))
        if (o._id != p._id) {
          isActive = true
          cancelable = subscribe()
        }
      }
      streamableModifiers.modifiers += DomUnmountHook { _ =>
        isActive = false
        cancelable.cancel()
      }

      // create initial proxy, we want to apply the initial state of the
      // receivers to the node
      val separatedModifiers = SeparatedModifiers.from(streamableModifiers.modifiers)
      nextModifiers = separatedModifiers.nextModifiers
      proxy = createProxy(separatedModifiers, node.nodeType, vNodeId, vNodeNS)
      proxy
    }
  }
}
