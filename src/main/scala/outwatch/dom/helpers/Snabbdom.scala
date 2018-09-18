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

private[outwatch] object DictionaryOps {
  def mergeArrayIntoSecond[T](first: js.Dictionary[T], second: js.Dictionary[js.Array[T]]) = {
    first.foreach { case (key, value) =>
      if (!second.contains(key)) {
        second(key) = new js.Array[T]
      }
      second(key) += value
    }
    second
  }
  def mergeIntoSecond[T](first: js.Dictionary[T], second: js.Dictionary[T]) = {
    first.foreach { case (key, value) if second.get(key).isEmpty => second(key) = value }
    second
  }

  def mergeAccumIntoSecond[T](first: js.Dictionary[T], second: js.Dictionary[T], keys: Set[String], accum: (T,T) => T) = {
    first.foreach {
      case (key, value) if keys(key) => second.get(key) match {
        case Some(next) => second(key) = accum(value, next)
        case None => second(key) = value
      }
      case (key, value) => second(key) = value
    }
    second
  }
}

private[outwatch] object SnabbdomHooks {

  private def createPostPatchHookWithUnmount(hooks: SeparatedHooks): Hooks.HookPairFn = {
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

  private def createPostPatchHook(hooks: SeparatedHooks): js.UndefOr[Hooks.HookPairFn] = {
    import hooks._

    if (domMountHook.isEmpty && domUpdateHook.isEmpty && postPatchHook.isEmpty) js.undefined
    else { (oldProxy: VNodeProxy, proxy: VNodeProxy) =>
      if (proxy.outwatchState.map(_.id) != oldProxy.outwatchState.map(_.id)) {
        oldProxy.outwatchState.foreach(_.domUnmountHook.foreach(_(oldProxy)))
        domMountHook.foreach(_(proxy))
      } else {
        domUpdateHook.foreach(_(proxy))
      }
      postPatchHook.foreach(_(oldProxy, proxy))
    }: Hooks.HookPairFn
  }

  def mergeHooksSingle[T](prevHook: js.UndefOr[Hooks.HookSingleFn], newHook: js.UndefOr[Hooks.HookSingleFn]): js.UndefOr[Hooks.HookSingleFn] = {
    prevHook.map { prevHook =>
      newHook.fold[Hooks.HookSingleFn](prevHook) { newHook => e =>
        prevHook(e)
        newHook(e)
      }
    }.orElse(newHook)
  }
  def mergeHooksPair[T](prevHook: js.UndefOr[Hooks.HookPairFn], newHook: js.UndefOr[Hooks.HookPairFn]): js.UndefOr[Hooks.HookPairFn] = {
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

  def toSnabbdom(hooks: SeparatedHooks, unmount: Boolean): Hooks = {
    import hooks._
    val insert = mergeHooksSingle(domMountHook, insertHook)
    val destroy = mergeHooksSingle(domUnmountHook, destroyHook)
    val postPatch: js.UndefOr[Hooks.HookPairFn] = if (unmount) createPostPatchHookWithUnmount(hooks) else createPostPatchHook(hooks)

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

  private def createDataObject(modifiers: SeparatedModifiers, vNodeId: Int): DataObject = {
    import modifiers._

    val key: js.UndefOr[Key.Value] = if (children.hasStream) keyOption.orElse(vNodeId) else keyOption

    DataObject(
      attributes.attrs, attributes.props, attributes.styles,
      SnabbdomEmitters.toSnabbdom(emitters),
      SnabbdomHooks.toSnabbdom(hooks, unmount = true),
      key
    )
  }

  private def updateDataObject(modifiers: SeparatedModifiers, previousData: DataObject)(implicit scheduler: Scheduler): DataObject = {
    import modifiers._

    val snabbdomHooks = SnabbdomHooks.toSnabbdom(hooks, unmount = false)

    DataObject(
      //TODO this is a workaround for streaming accum attributes. the fix only
      //handles "class"-attributes, because it is the only default accum
      //attribute.  But this should for work for all accum attributes.
      //Therefore we need to have all accum keys for initial attrs and styles
      //whith their accum function here.
      attrs = DictionaryOps.mergeAccumIntoSecond(previousData.attrs, attributes.attrs, Set("class"), (p, n) => p.toString + " " + n.toString),
      props = DictionaryOps.mergeIntoSecond(previousData.props, attributes.props),
      style = DictionaryOps.mergeIntoSecond(previousData.style, attributes.styles),
      on = SnabbdomEmitters.toSnabbdom(DictionaryOps.mergeArrayIntoSecond(previousData.on, emitters)),
      hook = Hooks(
        SnabbdomHooks.mergeHooksSingle(previousData.hook.insert, snabbdomHooks.insert),
        SnabbdomHooks.mergeHooksPair(previousData.hook.prepatch, snabbdomHooks.prepatch),
        SnabbdomHooks.mergeHooksPair(previousData.hook.update, snabbdomHooks.update),
        SnabbdomHooks.mergeHooksPair(previousData.hook.postpatch, snabbdomHooks.postpatch),
        SnabbdomHooks.mergeHooksSingle(previousData.hook.destroy, snabbdomHooks.destroy)
      ),
      key = keyOption orElse previousData.key
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

  private[outwatch] def updateSnabbdom(modifiers: SeparatedModifiers, previousProxy: VNodeProxy)(implicit scheduler: Scheduler): VNodeProxy = {
    import modifiers._

    // Updates never contain streamable content and therefore we do not need to
    // handle them with Receivers.
    val dataObject = updateDataObject(modifiers, previousProxy.data)

    createProxy(previousProxy.sel, previousProxy.outwatchState, dataObject, children.nodes, children.hasVTree)
  }

  private[outwatch] def toSnabbdom(modifiers: SeparatedModifiers, nodeType: String)(implicit scheduler: Scheduler): VNodeProxy = {
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
      var initialProxy: VNodeProxy = null
      var proxy: VNodeProxy = null

      def toProxy(modifiers: js.Array[Modifier]): VNodeProxy = {
        SnabbdomModifiers.updateSnabbdom(SeparatedModifiers.from(modifiers), initialProxy)
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

      // hooks for subscribing and unsubscribing
      val cancelable = new QueuedCancelable()
      modifiers.append(DomMountHook(_ => cancelable.enqueue(subscribe())))
      modifiers.append(DomUnmountHook(_ => cancelable.dequeue().cancel()))

      // create initial proxy
      val state = SnabbdomHooks.toOutwatchState(hooks, vNodeId)
      val dataObject = createDataObject(modifiers, vNodeId)
      initialProxy = createProxy(nodeType, state, dataObject, childrenWithKey, children.hasVTree)

      // directly update this dataobject with default values from the receivers
      proxy = updateSnabbdom(SeparatedModifiers.from(receivers.initialState), initialProxy)

      proxy
    } else {
      val state = SnabbdomHooks.toOutwatchState(hooks, vNodeId)
      val dataObject = createDataObject(modifiers, vNodeId)
      createProxy(nodeType, state, dataObject, children.nodes, children.hasVTree)
    }
  }
}
