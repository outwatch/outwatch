package outwatch.dom.helpers

import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import monix.reactive.observers.Subscriber
import org.scalajs.dom
import outwatch.dom._
import snabbdom.{Hooks, VNodeProxy}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSBracketAccess

@js.native
private trait DictionaryRawApply[A] extends js.Object {
  @JSBracketAccess
  def apply(key: String): js.UndefOr[A] = js.native
}
private object DictionaryRawApply {
  implicit class WithRaw[A](val dict: js.Dictionary[A]) extends AnyVal {
    def raw: DictionaryRawApply[A] = dict.asInstanceOf[DictionaryRawApply[A]]
  }
}
import outwatch.dom.helpers.DictionaryRawApply._

private[outwatch] object SeparatedModifiers {
  def from(modifiers: js.Array[StaticVDomModifier])(implicit scheduler: Scheduler): SeparatedModifiers = {
    var proxies: js.UndefOr[js.Array[VNodeProxy]] = js.undefined
    var attrs: js.UndefOr[js.Dictionary[Attr.Value]] = js.undefined
    var props: js.UndefOr[js.Dictionary[Prop.Value]] = js.undefined
    var styles: js.UndefOr[js.Dictionary[Style.Value]] = js.undefined
    var emitters: js.UndefOr[js.Dictionary[js.Function1[dom.Event, Unit]]] = js.undefined
    var keyOption: js.UndefOr[Key.Value] = js.undefined
    var insertHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
    var prePatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var updateHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var postPatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var destroyHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
    var domUnmountHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined

    @inline def assureProxies(): Unit = if (proxies.isEmpty) proxies = new js.Array[VNodeProxy]()
    @inline def assureEmitters(): Unit = if (emitters.isEmpty) emitters = js.Dictionary[js.Function1[dom.Event, Unit]]()
    @inline def assureAttrs(): Unit = if (attrs.isEmpty) attrs = js.Dictionary[Attr.Value]()
    @inline def assureProps(): Unit = if (props.isEmpty) props = js.Dictionary[Prop.Value]()
    @inline def assureStyles(): Unit = if (styles.isEmpty) styles = js.Dictionary[Style.Value]()
    @inline def setSpecialStyle(styleName: String)(title: String, value: String): Unit = {
      assureStyles()
      val style = styles.get.raw(styleName)
      if (style.isEmpty) {
        styles.get(styleName) = js.Dictionary[String](title -> value): Style.Value
      } else {
        style.get.asInstanceOf[js.Dictionary[String]](title) = value
      }
    }
    @inline def createProxyHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: (VNodeProxy, VNodeProxy) => Unit): Hooks.HookPairFn =
      if (current.isEmpty) hook
      else { (o,p) => current.get(o, p); hook(o, p) }
    @inline def createHooksSingle(current: js.UndefOr[Hooks.HookSingleFn], hook: dom.Element => Unit): Hooks.HookSingleFn =
      if (current.isEmpty) { p => p.elm.foreach(hook) }
      else { p => current.get(p); p.elm.foreach(hook) }
    @inline def createHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: ((dom.Element, dom.Element)) => Unit): Hooks.HookPairFn =
      if (current.isEmpty) { (o,p) => for { oe <- o.elm; pe <- p.elm } hook((oe, pe)) }
      else { (o,p) => current.get(o, p); for { oe <- o.elm; pe <- p.elm } hook((oe, pe)) }: Hooks.HookPairFn
    @inline def createHooksPairOption(current: js.UndefOr[Hooks.HookPairFn], hook: ((Option[dom.Element], Option[dom.Element])) => Unit): Hooks.HookPairFn =
      if (current.isEmpty) { (o,p) => hook((o.elm.toOption, p.elm.toOption)) }
      else { (o,p) => current.get(o, p); hook((o.elm.toOption, p.elm.toOption)) }


    // append unmount hook for when patching a different proxy out of the dom.
    // the proxies will then have different OutwatchStates and we then need to
    // call the unmount hook of the oldProxy.
    postPatchHook = { (oldProxy, proxy) =>
      if (proxy.outwatchId != oldProxy.outwatchId) {
        oldProxy.outwatchDomUnmountHook.foreach(_(oldProxy))
      }
    }: Hooks.HookPairFn

    // append modifiers
    modifiers.foreach {
      case VNodeProxyNode(proxy) =>
        assureProxies()
        proxies.get += proxy
      case a : BasicAttr =>
        assureAttrs()
        attrs.get(a.title) = a.value
      case a : AccumAttr =>
        assureAttrs()
        val attr = attrs.get.raw(a.title)
        if (attr.isEmpty) {
          attrs.get(a.title) = a.value
        } else {
          attrs.get(a.title) = a.accum(attr.get, a.value)
        }
      case p : Prop =>
        assureProps()
        props.get(p.title) = p.value
      case s: BasicStyle =>
        assureStyles()
        styles.get(s.title) = s.value
      case s: DelayedStyle =>
        setSpecialStyle(StyleKey.delayed)(s.title, s.value)
      case s: RemoveStyle =>
        setSpecialStyle(StyleKey.remove)(s.title, s.value)
      case s: DestroyStyle =>
        setSpecialStyle(StyleKey.destroy)(s.title, s.value)
      case a: AccumStyle =>
        assureStyles()
        val stylesDict = styles.get
        val style = stylesDict.raw(a.title)
        if (style.isEmpty) {
          stylesDict(a.title) = a.value
        } else {
          stylesDict(a.title) = a.accum(style.get.asInstanceOf[String], a.value): Style.Value
        }
      case k: Key =>
        keyOption = k.value
      case e: Emitter =>
        assureEmitters()
        val emittersDict = emitters.get
        val emitter = emittersDict.raw(e.eventType)
        if (emitter.isEmpty) {
          emittersDict(e.eventType) = e.trigger
        } else {
          emittersDict(e.eventType) = { ev => emitter.get(ev); e.trigger(ev) }
        }
      case h: DomMountHook =>
        insertHook = createHooksSingle(insertHook, h.trigger)
        postPatchHook = createProxyHooksPair(postPatchHook, { (oldProxy, proxy) =>
          if (proxy.outwatchId != oldProxy.outwatchId) {
            proxy.elm.foreach(h.trigger)
          }
        })
      case h: DomUnmountHook =>
        destroyHook = createHooksSingle(destroyHook, h.trigger)
        domUnmountHook = createHooksSingle(domUnmountHook, h.trigger)
      case h: DomUpdateHook =>
        postPatchHook = createProxyHooksPair(postPatchHook, { (oldproxy, proxy) =>
          if (proxy.outwatchId == oldproxy.outwatchId) {
            proxy.elm.foreach(h.trigger)
          }
        })
      case h: DomPreUpdateHook =>
        prePatchHook = createProxyHooksPair(prePatchHook, { (oldproxy, proxy) =>
          if (proxy.outwatchId == oldproxy.outwatchId) {
            oldproxy.elm.foreach(h.trigger)
          }
        })
      case h: InsertHook =>
        insertHook = createHooksSingle(insertHook, h.trigger)
      case h: PrePatchHook =>
        prePatchHook = createHooksPairOption(prePatchHook, h.trigger)
      case h: UpdateHook =>
        updateHook = createHooksPair(updateHook, h.trigger)
      case h: PostPatchHook =>
        postPatchHook = createHooksPair(postPatchHook, h.trigger)
      case h: DestroyHook =>
        destroyHook = createHooksSingle(destroyHook, h.trigger)
    }

    new SeparatedModifiers(proxies, attrs, props, styles, keyOption, emitters, insertHook, prePatchHook, updateHook, postPatchHook, destroyHook, domUnmountHook)
  }
}

private[outwatch] class SeparatedModifiers(
  val proxies: js.UndefOr[js.Array[VNodeProxy]],
  val attrs: js.UndefOr[js.Dictionary[Attr.Value]],
  val props: js.UndefOr[js.Dictionary[Prop.Value]],
  val styles: js.UndefOr[js.Dictionary[Style.Value]],
  val keyOption: js.UndefOr[Key.Value],
  val emitters: js.UndefOr[js.Dictionary[js.Function1[dom.Event, Unit]]],
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
        val lengthsArr = new js.Array[Int](modifiers.length)
        var i = 0
        while (i < modifiers.length) {
          lengthsArr(i) = 1
          i += 1
        }
        lengths = lengthsArr
        updaterObservables = new js.Array[Observable[js.Array[StaticVDomModifier]]]()
      }

      val lengthsArr = lengths.get
      val index = lengthsArr.length
      stream.value match {
        case Some(value) =>
          modifiers ++= value
          lengthsArr += value.length
        case None =>
          lengthsArr += 0
      }
      updaterObservables.get += stream.observable.map { mods =>
        var i = 0
        var lengthBefore = 0
        while (i < index) {
          lengthBefore += lengthsArr(i)
          i += 1
        }
        modifiers.splice(lengthBefore, lengthsArr(index), mods: _*)
        lengthsArr(index) = mods.length
        modifiers
      }
    }

    def inner(modifier: VDomModifier): Unit = modifier match {
      case EmptyModifier => ()
      case c: CompositeModifier => c.modifiers.foreach(inner)
      case mod: StaticVDomModifier => appendModifier(mod)
      case child: VNode  => appendModifier(VNodeProxyNode(SnabbdomOps.toSnabbdom(child)))
      case child: StringVNode  => appendModifier(VNodeProxyNode(VNodeProxy.fromString(child.text)))
      case m: ModifierStreamReceiver => appendStream(flattenModifierStream(m.stream))
      case m: EffectModifier => inner(m.effect.unsafeRunSync())
      case m: SchedulerAction => inner(m.action(scheduler))
    }

    inner(modifier)

    new NativeModifiers(modifiers, updaterObservables.map(obs => new CollectionObservable[js.Array[StaticVDomModifier]](obs)))
  }

  private def flattenModifierStream(modStream: ValueObservable[VDomModifier])(implicit scheduler: Scheduler): ValueObservable[js.Array[StaticVDomModifier]] = {
    val observable = modStream.observable.switchMap[js.Array[StaticVDomModifier]] {
      case mod: StaticVDomModifier => Observable.now(js.Array(mod))
      case EmptyModifier => Observable.now(js.Array())
      case child: VNode  => Observable.now(js.Array(VNodeProxyNode(SnabbdomOps.toSnabbdom(child))))
      case child: StringVNode  => Observable.now(js.Array(VNodeProxyNode(VNodeProxy.fromString(child.text))))
      case mod =>
        val nativeModifiers = from(mod)
        if (nativeModifiers.observable.isEmpty) Observable.now(nativeModifiers.modifiers)
        else Observable.concat(Observable.now(nativeModifiers.modifiers), nativeModifiers.observable.get)
    }

    modStream.value.fold[ValueObservable[js.Array[StaticVDomModifier]]](ValueObservable(observable, js.Array())) {
      case defaultValue: StaticVDomModifier => ValueObservable(observable, js.Array(defaultValue))
      case EmptyModifier => ValueObservable(observable, js.Array())
      case child: VNode  => ValueObservable(observable, js.Array(VNodeProxyNode(SnabbdomOps.toSnabbdom(child))))
      case child: StringVNode  => ValueObservable(observable, js.Array(VNodeProxyNode(VNodeProxy.fromString(child.text))))
      case defaultValue =>
        val nativeModifiers = from(defaultValue)
        val initialObservable = if (nativeModifiers.observable.isEmpty) observable
        else observable.publishSelector { observable =>
          Observable.merge(nativeModifiers.observable.get.takeUntil(observable), observable)
        }

        ValueObservable(initialObservable, nativeModifiers.modifiers)
    }
  }
}

private object StyleKey {
  def delayed = "delayed"
  def remove = "remove"
  def destroy = "destroy"
}

private class CollectionObservable[A](observables: js.Array[Observable[A]]) extends Observable[A] {
  override def unsafeSubscribeFn(subscriber: Subscriber[A]): Cancelable = {
    val cancelable = CompositeCancelable()
    observables.foreach { observable =>
      // we only pass onNext and onError and not onComplete.
      cancelable += observable.unsafeSubscribeFn(Sink.create(subscriber.onNext, subscriber.onError))(subscriber.scheduler)
    }
    cancelable
  }
}
