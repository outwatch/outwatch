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

private[outwatch] object SnabbdomHooks {

  private def createPostPatchHook(hooks: SeparatedHooks): Hooks.HookPairFn = {
    import hooks._

    { (oldProxy: VNodeProxy, proxy: VNodeProxy) =>
      if (proxy.outwatchState.map(_.id) != oldProxy.outwatchState.map(_.id)) {
        oldProxy.outwatchState.foreach(_.domUnmountHook.foreach(_(oldProxy)))
        domMountHook.foreach(_(proxy))
      } else {
        domUpdateHook.foreach(_(proxy))
      }
      postPatchHook.foreach(_(oldProxy, proxy))
    }
  }

  private def mergeHooksSingle[T](prevHook: js.UndefOr[Hooks.HookSingleFn], newHook: js.UndefOr[Hooks.HookSingleFn]): js.UndefOr[Hooks.HookSingleFn] = {
    prevHook.map { prevHook =>
      newHook.fold[Hooks.HookSingleFn](prevHook) { newHook => e =>
        prevHook(e)
        newHook(e)
      }
    }.orElse(newHook)
  }
  private def mergeHooksPair[T](prevHook: js.UndefOr[Hooks.HookPairFn], newHook: js.UndefOr[Hooks.HookPairFn]): js.UndefOr[Hooks.HookPairFn] = {
    prevHook.map { prevHook =>
      newHook.fold[Hooks.HookPairFn](prevHook) { newHook => (e1, e2) =>
        prevHook(e1, e2)
        newHook(e1, e2)
      }
    }.orElse(newHook)
  }

  def toOutwatchState(hooks: SeparatedHooks, vNodeId: Int): js.UndefOr[OutwatchState] = {
    import hooks._

    if (domMountHook.isEmpty && domUnmountHook.isEmpty && domUpdateHook.isEmpty) js.undefined
    else OutwatchState(vNodeId, domUnmountHook)
  }

  def toSnabbdom(hooks: SeparatedHooks): Hooks = {
    import hooks._
    val insert = mergeHooksSingle(domMountHook, insertHook)
    val destroy = mergeHooksSingle(domUnmountHook, destroyHook)
    val postPatch: js.UndefOr[Hooks.HookPairFn] = createPostPatchHook(hooks)

    Hooks(insert, prePatchHook, updateHook, postPatch, destroy)
  }
}

private[outwatch] object SnabbdomEmitters {
  def combineEmitters(triggers: js.Array[js.Function1[dom.Event, Unit]]): js.Function1[dom.Event, Unit] = { event =>
    triggers.foreach(_(event))
  }

  def toSnabbdom(emitters: js.Dictionary[js.Array[js.Function1[dom.Event, Unit]]]): js.Dictionary[js.Function1[dom.Event, Unit]] = {
    val newEmitters = js.Dictionary[js.Function1[dom.Event, Unit]]()
    emitters.foreach { case (k, v) =>
      newEmitters(k) = combineEmitters(v)
    }

    newEmitters
  }
}

private[outwatch] object SnabbdomModifiers {

  private def createDataObject(modifiers: SeparatedModifiers): DataObject = {
    import modifiers._

    DataObject(
      attributes.attrs, attributes.props, attributes.styles,
      SnabbdomEmitters.toSnabbdom(emitters),
      SnabbdomHooks.toSnabbdom(hooks),
      keyOption
    )
  }

  // This is called initially once the VNode is constructed. Every time a
  // dynamic modifier of this node yields a new VNodeState, this new state with
  // new modifiers and attributes needs to be applied to the current
  // VNodeProxy.
  private[outwatch] def createProxy(nodeType: String, state: js.UndefOr[OutwatchState], dataObject: DataObject, children: js.Array[ChildVNode], hasVTree: Boolean)(implicit scheduler: Scheduler): VNodeProxy = {
    val proxy = if (children.isEmpty) {
      hFunction(nodeType, dataObject)
    } else if (hasVTree) {
      val childProxies = children.collect { case s: StaticVNode => s.toSnabbdom }
      hFunction(nodeType, dataObject, childProxies)
    } else {
      val textChildren = children.collect { case s: StringVNode => s.string }
      hFunction(nodeType, dataObject, textChildren.mkString)
    }

    proxy.outwatchState = state
    proxy
  }

  private[outwatch] def updateSnabbdom(modifiersArray: js.Array[Modifier], nodeType: String, vNodeId: Int, initialModifiers: SeparatedModifiers)(implicit scheduler: Scheduler): VNodeProxy = {

    val newModifiers = SeparatedModifiers.fromWithoutChildren(initialModifiers)
    modifiersArray.foreach(newModifiers.append)

    // Updates never contain streamable content and therefore we do not need to
    // handle them with Receivers.

    val dataObject = createDataObject(newModifiers)
    val state = SnabbdomHooks.toOutwatchState(newModifiers.hooks, vNodeId)

    createProxy(nodeType, state, dataObject, newModifiers.children.nodes, newModifiers.children.hasVTree)
  }

  private[outwatch] def toSnabbdom(modifiersArray: js.Array[Modifier], nodeType: String)(implicit scheduler: Scheduler): VNodeProxy = {
    val modifiers = SeparatedModifiers.from(modifiersArray)
    import modifiers._

    val vNodeId = modifiers.hashCode

    // if there is streamable content, we update the initial proxy with
    // subscribe and unsubscribe callbakcs.  additionally we update it with the
    // initial state of the obseravbles.
    if (children.hasStream) {
      // if there is streamable content and static nodes, we give keys to all static nodes
      val childrenWithKey = if (children.hasVTree) children.nodes.map {
        case vtree: VTree => vtree.copy(modifiers = Key(vtree.hashCode) +: vtree.modifiers)
        case other => other
      } else children.nodes

      val receivers = new Receivers(childrenWithKey)

      // needs var for forward referencing
      var proxy: VNodeProxy = null

      def toProxy(newState: js.Array[Modifier]): VNodeProxy = {
        updateSnabbdom(newState, nodeType, vNodeId, modifiers)
      }
      def subscribe(): Cancelable = {
        receivers.observable
          .map(toProxy)
          .scan(proxy) { case (old, crt) =>
            OutwatchTracing.patchSubject.onNext((old, crt))
            val next = patch(old, crt)
            proxy.sel = next.sel
            proxy.data = next.data
            proxy.children = next.children
            proxy.elm = next.elm
            proxy.text = next.text
            proxy.key = next.key
            next
          }.subscribe(
          _ => Continue,
          error => dom.console.error(error.getMessage + "\n" + error.getStackTrace.mkString("\n"))
        )
      }

      // add a key if not existing. we want to have a key for each streaming
      // node so it can be patched efficiently by snabbdom
      if (keyOption.isEmpty) {
        modifiers.append(Key(vNodeId))
      }

      // hooks for subscribing and unsubscribing the streamable content
      val cancelable = new QueuedCancelable()
      modifiers.append(DomMountHook(_ => cancelable.enqueue(subscribe())))
      modifiers.append(DomUnmountHook(_ => cancelable.dequeue().cancel()))

      // create initial proxy
      proxy = updateSnabbdom(receivers.initialState, nodeType, vNodeId, modifiers)
      proxy
    } else {
      val state = SnabbdomHooks.toOutwatchState(hooks, vNodeId)
      val dataObject = createDataObject(modifiers)
      createProxy(nodeType, state, dataObject, children.nodes, children.hasVTree)
    }
  }
}
