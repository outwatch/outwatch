package outwatch.dom.helpers

import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import monix.reactive.observables.ConnectableObservable
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom
import outwatch.dom._
import snabbdom.{Hooks, OutwatchState, VNodeProxy}

import scala.scalajs.js

private[outwatch] object SeparatedModifiers {
  def from(modifiers: js.Array[StaticVDomModifier])(implicit scheduler: Scheduler): SeparatedModifiers = {
    val proxies = new js.Array[VNodeProxy]()
    val attrs = js.Dictionary[Attr.Value]()
    val props = js.Dictionary[Prop.Value]()
    val styles = js.Dictionary[Style.Value]()
    val emitters = js.Dictionary[js.Function1[dom.Event, Unit]]()
    var keyOption: js.UndefOr[Key.Value] = js.undefined
    var insertHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
    var prePatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var updateHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var postPatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var destroyHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
    var domUnmountHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
    var usesOutwatchState: Boolean = false

    def setSpecialStyle(styleName: String)(title: String, value: String): Unit =
      if (!styles.contains(styleName)) {
        styles(styleName) = js.Dictionary[String](title -> value): Style.Value
      } else {
        styles(styleName).asInstanceOf[js.Dictionary[String]](title) = value
      }
    def createProxyHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: (VNodeProxy, VNodeProxy) => Unit): Hooks.HookPairFn =
      if (current.isEmpty) hook
      else { (o,p) => current.get(o, p); hook(o, p) }
    def createHooksSingle(current: js.UndefOr[Hooks.HookSingleFn], hook: dom.Element => Unit): Hooks.HookSingleFn =
      if (current.isEmpty) { p => p.elm.foreach(hook) }
      else { p => current.get(p); p.elm.foreach(hook) }
    def createHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: ((dom.Element, dom.Element)) => Unit): Hooks.HookPairFn =
      if (current.isEmpty) { (o,p) => for { oe <- o.elm; pe <- p.elm } hook((oe, pe)) }
      else { (o,p) => current.get(o, p); for { oe <- o.elm; pe <- p.elm } hook((oe, pe)) }: Hooks.HookPairFn
    def createHooksPairOption(current: js.UndefOr[Hooks.HookPairFn], hook: ((Option[dom.Element], Option[dom.Element])) => Unit): Hooks.HookPairFn =
      if (current.isEmpty) { (o,p) => hook((o.elm.toOption, p.elm.toOption)) }
      else { (o,p) => current.get(o, p); hook((o.elm.toOption, p.elm.toOption)) }

    // append unmount hook for when patching a different proxy out of the dom.
    // the proxies will then have different OutwatchStates and we then need to
    // call the unmount hook of the oldProxy.
    postPatchHook = createProxyHooksPair(postPatchHook, { (oldProxy, proxy) =>
      if (proxy.outwatchState.map(_.id) != oldProxy.outwatchState.map(_.id)) {
        oldProxy.outwatchState.foreach(_.domUnmountHook.foreach(_(oldProxy)))
      }
    })

    // append modifiers
    modifiers.foreach {
      case VNodeProxyNode(proxy) =>
        proxies += proxy
      case a : BasicAttr =>
        attrs(a.title) = a.value
      case a : AccumAttr =>
        attrs(a.title) = attrs.get(a.title).fold(a.value)(a.accum(_, a.value))
      case p : Prop =>
        props(p.title) = p.value
      case s: BasicStyle =>
        styles(s.title) = s.value
      case s: DelayedStyle =>
        setSpecialStyle(StyleKey.delayed)(s.title, s.value)
      case s: RemoveStyle =>
        setSpecialStyle(StyleKey.remove)(s.title, s.value)
      case s: DestroyStyle =>
        setSpecialStyle(StyleKey.destroy)(s.title, s.value)
      case a: AccumStyle =>
        styles(a.title) = styles.get(a.title).fold[Style.Value](a.value)(s =>
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
        insertHook = createHooksSingle(insertHook, trigger)
        postPatchHook = createProxyHooksPair(postPatchHook, { (oldProxy, proxy) =>
          if (proxy.outwatchState.map(_.id) != oldProxy.outwatchState.map(_.id)) {
            proxy.elm.foreach(trigger)
          }
        })
        usesOutwatchState = true
      case DomUnmountHook(trigger) =>
        destroyHook = createHooksSingle(destroyHook, trigger)
        domUnmountHook = createHooksSingle(domUnmountHook, trigger)
      case DomUpdateHook(trigger) =>
        postPatchHook = createProxyHooksPair(postPatchHook, { (oldproxy, proxy) =>
          if (proxy.outwatchState.map(_.id) == oldproxy.outwatchState.map(_.id)) {
            proxy.elm.foreach(trigger)
          }
        })
        usesOutwatchState = true
      case InsertHook(trigger) =>
        insertHook = createHooksSingle(insertHook, trigger)
      case PrePatchHook(trigger) =>
        prePatchHook = createHooksPairOption(prePatchHook, trigger)
      case UpdateHook(trigger) =>
        updateHook = createHooksPair(updateHook, trigger)
      case PostPatchHook(trigger) =>
        postPatchHook = createHooksPair(postPatchHook, trigger)
      case DestroyHook(trigger) =>
        destroyHook = createHooksSingle(destroyHook, trigger)
    }

    new SeparatedModifiers(proxies, attrs, props, styles, keyOption, emitters, usesOutwatchState, insertHook, prePatchHook, updateHook, postPatchHook, destroyHook, domUnmountHook)
  }
}

private[outwatch] class SeparatedModifiers(
  val proxies: js.Array[VNodeProxy],
  val attrs: js.Dictionary[Attr.Value],
  val props: js.Dictionary[Prop.Value],
  val styles: js.Dictionary[Style.Value],
  val keyOption: js.UndefOr[Key.Value],
  val emitters: js.Dictionary[js.Function1[dom.Event, Unit]],
  val usesOutwatchState: Boolean,
  val insertHook: js.UndefOr[Hooks.HookSingleFn],
  val prePatchHook: js.UndefOr[Hooks.HookPairFn],
  val updateHook: js.UndefOr[Hooks.HookPairFn],
  val postPatchHook: js.UndefOr[Hooks.HookPairFn],
  val destroyHook: js.UndefOr[Hooks.HookSingleFn],
  val domUnmountHook: js.UndefOr[Hooks.HookSingleFn])


private[outwatch] class NativeModifiers(
  val modifiers: js.Array[StaticVDomModifier],
  val observable: js.UndefOr[Observable[js.Array[StaticVDomModifier]]]
)

private[outwatch] object NativeModifiers {
  def from(modifiers: js.Array[VDomModifier])(implicit scheduler: Scheduler): NativeModifiers = from(CompositeModifier(modifiers))

  def from(modifier: VDomModifier)(implicit scheduler: Scheduler): NativeModifiers = {
    var lengths: js.UndefOr[js.Array[Int]] = js.undefined
    var updaterObservables: js.UndefOr[js.Array[Observable[js.Array[StaticVDomModifier]]]] = js.undefined
    val modifiers = new js.Array[StaticVDomModifier]()

    def appendModifier(mod: StaticVDomModifier): Unit = {
      modifiers += mod
      if (lengths.isDefined) {
        lengths.get += 1
      }
    }
    def appendStream(stream: ValueObservable[js.Array[StaticVDomModifier]]): Unit = {
      if (lengths.isEmpty) {
        lengths = new js.Array[Int](modifiers.length)
        var i = 0
        while (i < modifiers.length) {
          lengths.get(i) = 1
          i += 1
        }
        updaterObservables = new js.Array[Observable[js.Array[StaticVDomModifier]]]()
      }

      val index = lengths.get.length
      stream.value match {
        case Some(value) =>
          modifiers ++= value
          lengths.get += value.length
        case None =>
          lengths.get += 0
      }
      updaterObservables.get += stream.observable.map { mods =>
        var i = 0
        var lengthBefore = 0
        while (i < index) {
          lengthBefore += lengths.get(i)
          i += 1
        }
        modifiers.splice(lengthBefore, lengths.get(index), mods: _*)
        lengths.get(index) = mods.length
        modifiers
      }
    }

    def inner(modifier: VDomModifier): Unit = modifier match {
      case EmptyModifier => ()
      case CompositeModifier(modifiers) => modifiers.foreach(inner)
      case mod: StaticVDomModifier => appendModifier(mod)
      case child: VNode  => appendModifier(VNodeProxyNode(SnabbdomOps.toSnabbdom(child)))
      case child: StringVNode  => appendModifier(VNodeProxyNode(VNodeProxy.fromString(child.text)))
      case ModifierStreamReceiver(modStream) => appendStream(flattenModifierStream(modStream))
      case EffectModifier(effect) => inner(effect.unsafeRunSync())
      case SchedulerAction(action) => inner(action(scheduler))
    }

    inner(modifier)

    new NativeModifiers(modifiers, updaterObservables.map(obs => Observable.merge(obs: _*)))
  }

  private def flattenModifierStream(modStream: ValueObservable[VDomModifier])(implicit scheduler: Scheduler): ValueObservable[js.Array[StaticVDomModifier]] = {
    val observable = modStream.observable.switchMap[js.Array[StaticVDomModifier]] {
      case mod: StaticVDomModifier => Observable.now(js.Array(mod))
      case EmptyModifier => Observable.now(js.Array())
      case mod =>
        val streamableModifiers = from(mod)
        if (streamableModifiers.observable.isEmpty) Observable.now(streamableModifiers.modifiers)
        else Observable.concat(Observable.now(streamableModifiers.modifiers), streamableModifiers.observable.get)
    }

    modStream.value.fold[ValueObservable[js.Array[StaticVDomModifier]]](ValueObservable(observable, js.Array())) {
      case defaultValue: StaticVDomModifier => ValueObservable(observable, js.Array(defaultValue))

      case EmptyModifier => ValueObservable(observable, js.Array())
      case defaultValue =>
        val streamableModifiers = from(defaultValue)
        val initialObservable = if (streamableModifiers.observable.isEmpty) observable
        else observable.publishSelector { observable =>
          Observable.merge(streamableModifiers.observable.get.takeUntil(observable), observable)
        }

        ValueObservable(initialObservable, streamableModifiers.modifiers)
    }
  }
}

private object StyleKey {
  def delayed = "delayed"
  def remove = "remove"
  def destroy = "destroy"
}
