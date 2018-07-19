package outwatch.dom.helpers

import monix.reactive.subjects.PublishSubject
import monix.execution.Ack.Continue
import monix.execution.Scheduler
import monix.execution.cancelables.SingleAssignCancelable
import monix.reactive.Observable
import org.scalajs.dom
import outwatch.dom._
import snabbdom._

import scala.collection.breakOut
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

object OutwatchTracing {
  private[outwatch] val patchSubject = PublishSubject[(VNodeProxy, VNodeProxy)]()
  def patch: Observable[(VNodeProxy, VNodeProxy)] = patchSubject
}

private[outwatch] object DictionaryOps {
  def merge[T](first: js.Dictionary[T], second: js.Dictionary[T]) = {
    val result = js.Dictionary.empty[T]
    first.foreach { case (key, value) => result(key) = value }
    second.foreach { case (key, value) => result(key) = value }
    result
  }

  def mergeWithAccum[T](first: js.Dictionary[T], second: js.Dictionary[T], keys: Set[String], accum: (T,T) => T) = {
    val result = js.Dictionary.empty[T]
    first.foreach { case (key, value) => result(key) = value }
    second.foreach {
      case (key, value) if keys(key) => first.get(key) match {
        case Some(prev) => result(key) = accum(prev, value)
        case None => result(key) = value
      }
      case (key, value) => result(key) = value
    }
    result
  }
}

private[outwatch] trait SnabbdomStyles { self: SeparatedStyles =>
  def toSnabbdom: js.Dictionary[Style.Value] = {
    val styleDict = js.Dictionary[Style.Value]()

    val delayedDict = js.Dictionary[String]()
    val removeDict = js.Dictionary[String]()
    val destroyDict = js.Dictionary[String]()

    styles.foreach {
      case s: BasicStyle => styleDict(s.title) = s.value
      case s: DelayedStyle => delayedDict(s.title) = s.value
      case s: RemoveStyle => removeDict(s.title) = s.value
      case s: DestroyStyle => destroyDict(s.title) = s.value
      case a: AccumStyle =>
        styleDict(a.title) = styleDict.get(a.title).map(s =>
          a.accum(s.asInstanceOf[String], a.value): Style.Value
        ).getOrElse(a.value)

    }

    if (delayedDict.nonEmpty) styleDict("delayed") = delayedDict : Style.Value
    if (removeDict.nonEmpty) styleDict("remove") = removeDict : Style.Value
    if (destroyDict.nonEmpty) styleDict("destroy") = destroyDict : Style.Value

    styleDict
  }
}

private[outwatch] trait SnabbdomAttributes { self: SeparatedAttributes =>

  type jsDict[T] = js.Dictionary[T]

  def toSnabbdom: (jsDict[Attr.Value], jsDict[Prop.Value], jsDict[Style.Value]) = {
    val attrsDict = js.Dictionary[Attr.Value]()
    val propsDict = js.Dictionary[Prop.Value]()

    attrs.foreach {
      case a: BasicAttr => attrsDict(a.title) = a.value
      case a: AccumAttr => attrsDict(a.title) = attrsDict.get(a.title).map(a.accum(_, a.value)).getOrElse(a.value)
    }
    props.foreach { p => propsDict(p.title) = p.value }

    (attrsDict, propsDict, styles.toSnabbdom)
  }
}

private[outwatch] trait SnabbdomHooks { self: SeparatedHooks =>

  private def createHookSingle(hooks: Seq[Hook[dom.Element]]): js.UndefOr[Hooks.HookSingleFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookSingleFn](hooks =>
      (p: VNodeProxy) => for (e <- p.elm) hooks.foreach(_.observer.onNext(e))
    ).orUndefined
  }

  private def createHookPair(hooks: Seq[Hook[(dom.Element, dom.Element)]]): js.UndefOr[Hooks.HookPairFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookPairFn](hooks =>
      (old: VNodeProxy, cur: VNodeProxy) => for (o <- old.elm; c <- cur.elm) hooks.foreach(_.observer.onNext((o, c)))
    ).orUndefined
  }

  private def createHookPairOption(hooks: Seq[Hook[(Option[dom.Element], Option[dom.Element])]]
  ): js.UndefOr[Hooks.HookPairFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookPairFn](hooks =>
      (old: VNodeProxy, cur: VNodeProxy) => hooks.foreach(_.observer.onNext((old.elm.toOption, cur.elm.toOption)))
    ).orUndefined
  }

  def createInsertHook(receivers: Receivers,
    subscription: SingleAssignCancelable,
    hooks: Seq[Hooks.HookSingleFn],
    initialProxy: VNodeProxy
  )(implicit s: Scheduler): Hooks.HookSingleFn = (proxy: VNodeProxy) => {


    def toProxy(modifiers: Array[Modifier]): VNodeProxy = {
      SeparatedModifiers.from(modifiers).updateSnabbdom(initialProxy)
    }

    subscription := receivers.observable
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
      }
      .subscribe(
        _ => Continue,
        error => dom.console.error(error.getMessage + "\n" + error.getStackTrace.mkString("\n"))
      )

//    proxy.elm.foreach((e: dom.Element) => hooks.foreach(_.observer.onNext(e)))
    hooks.foreach(_(proxy))
  }


  private def createDestroyHook(
    subscription: SingleAssignCancelable, hooks: Seq[DestroyHook]
  ): Hooks.HookSingleFn = (proxy: VNodeProxy) => {
    proxy.elm.foreach((e: dom.Element) => hooks.foreach(_.observer.onNext(e)))
    subscription.cancel()
    ()
  }

  def toSnabbdomWithoutReceivers(implicit s: Scheduler): Hooks = {
    val insertHook = createHookSingle(insertHooks)
    val destroyHook = createHookSingle(destroyHooks)
    val prePatchHook = createHookPairOption(prePatchHooks)
    val updateHook = createHookPair(updateHooks)
    val postPatchHook = createHookPair(postPatchHooks)

    Hooks(insertHook, prePatchHook, updateHook, postPatchHook, destroyHook)
  }
  def toSnabbdom(receivers: Receivers, subscription: SingleAssignCancelable)(implicit s: Scheduler): Hooks = {
    val insertHook = createHookSingle(insertHooks)
    val destroyHook = if (receivers.nonEmpty) {
      val destroyHook: js.UndefOr[Hooks.HookSingleFn] = createDestroyHook(subscription, destroyHooks)
      destroyHook
    }
    else {
      val destroyHook = createHookSingle(destroyHooks)
      destroyHook
    }
    val prePatchHook = createHookPairOption(prePatchHooks)
    val updateHook = createHookPair(updateHooks)
    val postPatchHook = createHookPair(postPatchHooks)

    Hooks(insertHook, prePatchHook, updateHook, postPatchHook, destroyHook)
  }
}

private[outwatch] trait SnabbdomEmitters { self: SeparatedEmitters =>

  private def emittersToFunction(emitters: Seq[Emitter]): js.Function1[dom.Event, Unit] = {
    (event: dom.Event) => emitters.foreach(_.trigger(event))
  }

  def toSnabbdom: js.Dictionary[js.Function1[dom.Event, Unit]] = {
    emitters
      .groupBy(_.eventType)
      .mapValues(emittersToFunction)
      .toJSDictionary
  }
}

private[outwatch] trait SnabbdomModifiers { self: SeparatedModifiers =>

  def createDataObject(receivers: Receivers, subscription: SingleAssignCancelable)(implicit s: Scheduler): DataObject = {
    val keyOption = properties.keys.lastOption
    val key = if (receivers.nonEmpty) {
      keyOption.fold[Key.Value](receivers.hashCode)(_.value): js.UndefOr[Key.Value]
    } else {
      keyOption.map(_.value).orUndefined
    }

    val hooks = properties.hooks.toSnabbdom(receivers, subscription)

    val (attrs, props, style) = properties.attributes.toSnabbdom
    DataObject(
      attrs, props, style, emitters.toSnabbdom, hooks, key
    )
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

   def updateDataObject(previousData: DataObject)(implicit scheduler: Scheduler): DataObject = {
    val (attrs, props, style) = properties.attributes.toSnabbdom
    val hooks = properties.hooks.toSnabbdomWithoutReceivers

    DataObject(
      //TODO this is a workaround for streaming accum attributes. the fix only
      //handles "class"-attributes, because it is the only default accum
      //attribute.  But this should for work for all accum attributes.
      //Therefore we need to have all accum keys for initial attrs and styles
      //whith their accum function here.
      attrs = DictionaryOps.mergeWithAccum(previousData.attrs, attrs, Set("class"), (p,n) => p.toString + " " + n.toString),
      props = DictionaryOps.merge(previousData.props, props),
      style = DictionaryOps.merge(previousData.style, style),
      on = DictionaryOps.merge(previousData.on, emitters.toSnabbdom),
      hook = Hooks(
        mergeHooksSingle(previousData.hook.insert, hooks.insert),
        mergeHooksPair(previousData.hook.prepatch, hooks.prepatch),
        mergeHooksPair(previousData.hook.update, hooks.update),
        mergeHooksPair(previousData.hook.postpatch, hooks.postpatch),
        mergeHooksSingle(previousData.hook.destroy, hooks.destroy)
      ),
      //TODO: it should not be possible to stream keys!
      key = previousData.key
    )
  }

  // This is called initially once the VNode is constructed. Every time a
  // dynamic modifier of this node yields a new VNodeState, this new state with
  // new modifiers and attributes needs to be applied to the current
  // VNodeProxy.
  private def toProxy(nodeType: String, previousProxy: Option[VNodeProxy])(implicit scheduler: Scheduler): VNodeProxy = {

    // if child streams exist, we want the static children in the same node to have keys
    // for efficient patching when the streams change
    val childrenWithKey = children.ensureKey

    // we only need receivers if this is the first call to toProxy. Updates
    // never contain streamable content and therefore we do not need to handle
    // them with Receivers.
    val subscription = SingleAssignCancelable()
    val (receivers, dataObject) = previousProxy.fold {
      val receivers = Receivers(childrenWithKey)
      (Option(receivers), createDataObject(receivers, subscription))
    } { p =>
      (None, updateDataObject(p.data))
    }

    val initialProxy = {
      if (childrenWithKey.nodes.isEmpty) {
        hFunction(nodeType, dataObject)
      } else if (childrenWithKey.hasVTree) {
        val childProxies: js.Array[VNodeProxy] = childrenWithKey.nodes.collect { case s: StaticVNode => s.toSnabbdom }(breakOut)
        hFunction(nodeType, dataObject, childProxies)
      } else {
        val textChildren: js.Array[String] = childrenWithKey.nodes.collect { case s: StringVNode => s.string }(breakOut)
        hFunction(nodeType, dataObject, textChildren.mkString)
      }
    }

    // we directly update this dataobject with default values from the receivers
    receivers.fold(initialProxy){ receivers =>
      if (receivers.nonEmpty) {
        initialProxy.data.hook.insert = properties.hooks.createInsertHook(receivers, subscription, initialProxy.data.hook.insert.toList, initialProxy)
        SeparatedModifiers.from(receivers.initialState).updateSnabbdom(initialProxy)
      }
      else initialProxy
  }
  }

  private[outwatch] def updateSnabbdom(previousProxy: VNodeProxy)(implicit scheduler: Scheduler): VNodeProxy = toProxy(previousProxy.sel, Some(previousProxy))
  private[outwatch] def toSnabbdom(nodeType: String)(implicit scheduler: Scheduler): VNodeProxy = toProxy(nodeType, None)
}
