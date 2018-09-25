package outwatch.dom.helpers

import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.observables.ConnectableObservable
import org.scalajs.dom
import outwatch.dom._
import snabbdom.{Hooks, OutwatchState, VNodeProxy}

import scala.scalajs.js

private[outwatch] object SeparatedModifiers {
  def from(modifiers: js.Array[VDomModifier])(implicit scheduler: Scheduler) = {
    val m = new SeparatedModifiers()
    modifiers.foreach { mod =>
      val nativeMod = ModifierOps.toNativeModifier(mod)
      m.append(nativeMod)
    }
    m
  }


  def fromWithoutChildren(that: SeparatedModifiers)(implicit scheduler: Scheduler) = {
    val m = new SeparatedModifiers()
    import m._

    that.emitters.foreach { case (k, v) =>
      emitters(k) = v
    }

    // keep out children intentionally

    hooks.insertHook = that.hooks.insertHook
    hooks.prePatchHook = that.hooks.prePatchHook
    hooks.updateHook = that.hooks.updateHook
    hooks.postPatchHook = that.hooks.postPatchHook
    hooks.destroyHook = that.hooks.destroyHook
    hooks.domUnmountHook = that.hooks.domUnmountHook

    that.attributes.attrs.foreach { case (k, v) =>
      attributes.attrs(k) = v
    }
    that.attributes.props.foreach { case (k, v) =>
      attributes.props(k) = v
    }
    that.attributes.styles.foreach { case (k, v) =>
      if (k == StyleKey.delayed || k == StyleKey.destroy || k == StyleKey.remove) {
        val dict = js.Dictionary[String]()
        attributes.styles(k) = dict: Style.Value
        v.asInstanceOf[js.Dictionary[String]].foreach { case (k, v) =>
          dict(k) = v
        }
      } else {
        attributes.styles(k) = v
      }
    }

    keyOption = that.keyOption

    outwatchState = that.outwatchState

    m
  }
}

private[outwatch] class SeparatedModifiers {
  var emitters = js.Dictionary[js.Function1[dom.Event, Unit]]()
  val children = new Children
  val attributes = new SeparatedAttributes()
  val hooks = new SeparatedHooks()
  var keyOption: js.UndefOr[Key.Value] = js.undefined
  var outwatchState: js.UndefOr[OutwatchState] = js.undefined

  def append(modifier: NativeVDomModifier)(implicit scheduler: Scheduler): Unit = modifier match {
    case EmptyModifier =>
      ()
    case StaticCompositeModifier(modifiers) =>
      modifiers.foreach(append(_))
    case VNodeProxyNode(proxy) =>
      setProxy(proxy)
    case NativeModifierStreamReceiver(stream) =>
      setStream(stream)
    case a : BasicAttr =>
      attributes.attrs(a.title) = a.value
    case a : AccumAttr =>
      attributes.attrs(a.title) = attributes.attrs.get(a.title).fold(a.value)(a.accum(_, a.value))
    case p : Prop =>
      attributes.props(p.title) = p.value
    case s: BasicStyle =>
      attributes.styles(s.title) = s.value
    case s: DelayedStyle =>
      setSpecialStyle(StyleKey.delayed)(s.title, s.value)
    case s: RemoveStyle =>
      setSpecialStyle(StyleKey.remove)(s.title, s.value)
    case s: DestroyStyle =>
      setSpecialStyle(StyleKey.destroy)(s.title, s.value)
    case a: AccumStyle =>
      attributes.styles(a.title) = attributes.styles.get(a.title).fold[Style.Value](a.value)(s =>
        a.accum(s.asInstanceOf[String], a.value): Style.Value
      )
    case Key(value) =>
      keyOption = value
    case Emitter(eventType, trigger) =>
      if (!emitters.contains(eventType)) {
        emitters(eventType) = trigger
      } else {
        val prev = emitters(eventType)
        emitters(eventType) = { ev => prev(ev); trigger(ev) }
      }
    case DomMountHook(trigger) =>
      hooks.insertHook = createHooksSingle(hooks.insertHook, trigger)
      hooks.postPatchHook = createProxyHooksPair(hooks.postPatchHook, { (oldProxy, proxy) =>
        if (proxy.outwatchState.map(_.id) != oldProxy.outwatchState.map(_.id)) {
          proxy.elm.foreach(trigger)
        }
      })
      hooks.usesOutwatchState = true
    case DomUnmountHook(trigger) =>
      hooks.destroyHook = createHooksSingle(hooks.destroyHook, trigger)
      hooks.domUnmountHook = createHooksSingle(hooks.domUnmountHook, trigger)
    case DomUpdateHook(trigger) =>
      hooks.postPatchHook = createProxyHooksPair(hooks.postPatchHook, { (oldproxy, proxy) =>
        if (proxy.outwatchState.map(_.id) == oldproxy.outwatchState.map(_.id)) {
          proxy.elm.foreach(trigger)
        }
      })
      hooks.usesOutwatchState = true
    case InsertHook(trigger) =>
      hooks.insertHook = createHooksSingle(hooks.insertHook, trigger)
    case PrePatchHook(trigger) =>
      hooks.prePatchHook = createHooksPairOption(hooks.prePatchHook, trigger)
    case UpdateHook(trigger) =>
      hooks.updateHook = createHooksPair(hooks.updateHook, trigger)
    case PostPatchHook(trigger) =>
      hooks.postPatchHook = createHooksPair(hooks.postPatchHook, trigger)
    case DestroyHook(trigger) =>
      hooks.destroyHook = createHooksSingle(hooks.destroyHook, trigger)
  }

  def appendUnmountHook(): Unit = {
    hooks.postPatchHook = createProxyHooksPair(hooks.postPatchHook, { (oldProxy, proxy) =>
      if (proxy.outwatchState.map(_.id) != oldProxy.outwatchState.map(_.id)) {
        oldProxy.outwatchState.foreach(_.domUnmountHook.foreach(_(oldProxy)))
      }
    })
  }

  private def setProxy(child: VNodeProxy): Unit = {
    children.proxies += child
    if (children.streamable.isDefined) {
      children.streamable.get.modifiers += VNodeProxyNode(child)
    }
  }
  private def setStream(stream: ValueObservable[StaticVDomModifier])(implicit scheduler: Scheduler) = {
    if (children.streamable.isEmpty) {
      children.streamable = new ModifierOps
      children.proxies.foreach { proxy =>
        children.streamable.get.modifiers += VNodeProxyNode(proxy)
      }
    }
    children.streamable.get.appendStream(stream)
  }

  private def setSpecialStyle(styleName: String)(title: String, value: String): Unit =
    if (!attributes.styles.contains(styleName)) {
      attributes.styles(styleName) = js.Dictionary[String](title -> value): Style.Value
    } else {
      attributes.styles(styleName).asInstanceOf[js.Dictionary[String]](title) = value
    }

  private def createProxyHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: (VNodeProxy, VNodeProxy) => Unit): Hooks.HookPairFn =
    if (current.isEmpty) hook
    else { (o,p) => current.get(o, p); hook(o, p) }
  private def createHooksSingle(current: js.UndefOr[Hooks.HookSingleFn], hook: dom.Element => Unit): Hooks.HookSingleFn =
    if (current.isEmpty) { p => p.elm.foreach(hook) }
    else { p => current.get(p); p.elm.foreach(hook) }
  private def createHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: ((dom.Element, dom.Element)) => Unit): Hooks.HookPairFn =
    if (current.isEmpty) { (o,p) => for { oe <- o.elm; pe <- p.elm } hook((oe, pe)) }
    else { (o,p) => current.get(o, p); for { oe <- o.elm; pe <- p.elm } hook((oe, pe)) }: Hooks.HookPairFn
  private def createHooksPairOption(current: js.UndefOr[Hooks.HookPairFn], hook: ((Option[dom.Element], Option[dom.Element])) => Unit): Hooks.HookPairFn =
    if (current.isEmpty) { (o,p) => hook((o.elm.toOption, p.elm.toOption)) }
    else { (o,p) => current.get(o, p); hook((o.elm.toOption, p.elm.toOption)) }
}

private[outwatch] object StyleKey {
  def delayed = "delayed"
  def remove = "remove"
  def destroy = "destroy"
}

private[outwatch] class Children {
  val proxies = new js.Array[VNodeProxy]
  var streamable: js.UndefOr[ModifierOps] = js.undefined
}

private[outwatch] class SeparatedAttributes {
  val attrs = js.Dictionary[Attr.Value]()
  val props = js.Dictionary[Prop.Value]()
  val styles = js.Dictionary[Style.Value]()
}

private[outwatch] class SeparatedHooks {
  var usesOutwatchState: Boolean = false
  var insertHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var prePatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var updateHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var postPatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var destroyHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var domUnmountHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
}

// StreamableModifiers takes a list of modifiers. It constructs an Observable
// of updates from dynamic modifiers in this list.
private[outwatch] class ModifierOps {
  import ModifierOps._

  // the nodes array has a fixed size - each static child node is one element
  // and the dynamic nodes can place one element on each update and start with
  // EmptyModifier, and we reserve an array element for each attribute
  // receiver.
  val modifiers = new js.Array[StaticVDomModifier](1) // 1 reserved for new one time modifiers
  modifiers(0) = EmptyModifier

  // for each node which might be dynamic, we have an Observable of Modifier updates
  private val updaterObservables = new js.Array[Observable[Unit]]

  // an observable representing the current state of this VNode. We take all
  // state update functions we have from dynamic modifiers and then scan over
  // them starting with the initial state. We do not actually, but mutate the
  // initial modifiers because of performance considerations.
  def observable: Observable[js.Array[StaticVDomModifier]] = Observable.merge(updaterObservables: _*).map(_ => modifiers)

  def appendStream(stream: ValueObservable[StaticVDomModifier])(implicit scheduler: Scheduler):Unit = {
    val index = modifiers.length
    val modStream = flattenModifierStream(stream)
    modifiers += modStream.value getOrElse EmptyModifier
    updaterObservables += modStream.observable.map { mod =>
      val composite = new js.Array[StaticVDomModifier]()
      modifiers(index) match {
        case DomUnmountHook(trigger) =>
          var done = false
          composite += DomUpdateHook { e => if (!done) trigger(e); done = true }
          composite += DomMountHook { e => done = true }
        case _ => ()
      }
      mod match {
        case DomMountHook(trigger) =>
          var done = false
          composite += DomUpdateHook { e => if (!done) trigger(e); done = true }
          composite += DomMountHook { e => done = true }
        case _ => ()
      }
      modifiers(0) = StaticCompositeModifier(composite)
      modifiers(index) = mod
    }
  }
}
private[outwatch] object ModifierOps {

  def toNativeModifier(modifier: VDomModifier)(implicit scheduler: Scheduler): NativeVDomModifier = modifier match {
    case ModifierStreamReceiver(modStream) => NativeModifierStreamReceiver(flattenModifierStream(modStream))
    case CompositeModifier(modifiers) => compositeToNativeModifier(modifiers)
    case child: VNode  => VNodeProxyNode(SnabbdomOps.toSnabbdom(child))
    case child: StringVNode  => VNodeProxyNode(VNodeProxy.fromString(child.text))
    case EffectModifier(effect) => toNativeModifier(effect.unsafeRunSync())
    case SchedulerAction(action) => toNativeModifier(action(scheduler))
    case mod: NativeVDomModifier => mod
  }

  def compositeToNativeModifier(modifiers: js.Array[_ <: VDomModifier])(implicit scheduler: Scheduler): NativeVDomModifier = {
    if (modifiers.isEmpty) EmptyModifier
    else {
      val streamableModifiers = new ModifierOps
      modifiers.foreach { modifier =>
        toNativeModifier(modifier) match {
          case NativeModifierStreamReceiver(modStream) => streamableModifiers.appendStream(modStream)
          case mod: StaticVDomModifier => streamableModifiers.modifiers += mod
        }
      }
      if (streamableModifiers.updaterObservables.isEmpty) {
        StaticCompositeModifier(streamableModifiers.modifiers)
      } else {
        NativeModifierStreamReceiver(ValueObservable(
          streamableModifiers.observable.map(StaticCompositeModifier(_)),
          StaticCompositeModifier(streamableModifiers.modifiers)
        ))
      }
    }
  }

  def flattenModifierStream(modStream: ValueObservable[VDomModifier])(implicit scheduler: Scheduler): ValueObservable[StaticVDomModifier] = {
    val observable = modStream.observable.switchMap[StaticVDomModifier] { mod =>
      toNativeModifier(mod) match {
        //TODO: why is startWith different and leaks a subscription? see tests with: stream.startWith(initialValue :: Nil)
        case NativeModifierStreamReceiver(stream) => Observable.concat(Observable.now(stream.value getOrElse EmptyModifier), stream.observable)
        case mod: StaticVDomModifier => Observable.now(mod)
      }
    }

    modStream.value.fold[ValueObservable[StaticVDomModifier]](ValueObservable(observable, EmptyModifier)) { defaultValue =>
      toNativeModifier(defaultValue) match {
        case NativeModifierStreamReceiver(stream) =>
          val combinedObservable = observable.publishSelector { observable =>
            Observable.merge(stream.observable.takeUntil(observable), observable)
          }
          ValueObservable(combinedObservable, stream.value getOrElse EmptyModifier)
        case mod: StaticVDomModifier =>
          ValueObservable(observable, mod)
      }
    }
  }
}
