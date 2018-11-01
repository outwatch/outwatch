package outwatch.dom.helpers

import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import monix.reactive.observers.Subscriber
import org.scalajs.dom
import outwatch.dom._
import snabbdom.{DataObject, Hooks, VNodeProxy}

import scala.annotation.tailrec
import scala.scalajs.js

import NativeHelpers._

private[outwatch] object SeparatedModifiers {
  def from(modifiers: js.Array[StaticVDomModifier])(implicit scheduler: Scheduler): SeparatedModifiers = {
    var proxies: js.UndefOr[js.Array[VNodeProxy]] = js.undefined
    var attrs: js.UndefOr[js.Dictionary[DataObject.AttrValue]] = js.undefined
    var props: js.UndefOr[js.Dictionary[DataObject.PropValue]] = js.undefined
    var styles: js.UndefOr[js.Dictionary[DataObject.StyleValue]] = js.undefined
    var emitters: js.UndefOr[js.Dictionary[js.Function1[dom.Event, Unit]]] = js.undefined
    var keyOption: js.UndefOr[Key.Value] = js.undefined
    var insertHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
    var prePatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var updateHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var postPatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
    var destroyHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
    var domUnmountHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined

    @inline def assureProxies() = proxies getOrElse assign(new js.Array[VNodeProxy])(proxies = _)
    @inline def assureEmitters() = emitters getOrElse assign(js.Dictionary[js.Function1[dom.Event, Unit]]())(emitters = _)
    @inline def assureAttrs() = attrs getOrElse assign(js.Dictionary[DataObject.AttrValue]())(attrs = _)
    @inline def assureProps() = props getOrElse assign(js.Dictionary[DataObject.PropValue]())(props = _)
    @inline def assureStyles() = styles getOrElse assign(js.Dictionary[DataObject.StyleValue]())(styles = _)
    @inline def setSpecialStyle(styleName: String)(title: String, value: String): Unit = {
      val styles = assureStyles()
      styles.raw(styleName).fold {
        styles(styleName) = js.Dictionary[String](title -> value): DataObject.StyleValue
      } { style =>
        style.asInstanceOf[js.Dictionary[String]](title) = value
      }
    }
    @inline def createProxyHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: (VNodeProxy, VNodeProxy) => Unit): Hooks.HookPairFn =
      current.fold[Hooks.HookPairFn](hook)(current => { (o,p) => current(o, p); hook(o, p) })
    @inline def createHooksSingle(current: js.UndefOr[Hooks.HookSingleFn], hook: dom.Element => Unit): Hooks.HookSingleFn =
      current.fold[Hooks.HookSingleFn]({ p => p.elm.foreach(hook) })(current => { p => current(p); p.elm.foreach(hook) })
    @inline def createHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: ((dom.Element, dom.Element)) => Unit): Hooks.HookPairFn =
      current.fold[Hooks.HookPairFn]({ (o,p) => for { oe <- o.elm; pe <- p.elm } hook((oe, pe)) })(current => { (o,p) => current(o, p); for { oe <- o.elm; pe <- p.elm } hook((oe, pe)) })
    @inline def createHooksPairOption(current: js.UndefOr[Hooks.HookPairFn], hook: ((Option[dom.Element], Option[dom.Element])) => Unit): Hooks.HookPairFn =
      current.fold[Hooks.HookPairFn]({ (o,p) => hook((o.elm.toOption, p.elm.toOption)) })(current => { (o,p) => current(o, p); hook((o.elm.toOption, p.elm.toOption)) })


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
        val proxies = assureProxies()
        proxies += proxy
      case a : BasicAttr =>
        val attrs = assureAttrs()
        attrs(a.title) = a.value
      case a : AccumAttr =>
        val attrs = assureAttrs()
        val attr = attrs.raw(a.title)
        attr.fold {
          attrs(a.title) = a.value
        } { attr =>
          attrs(a.title) = a.accum(attr, a.value)
        }
      case p : Prop =>
        val props = assureProps()
        props(p.title) = p.value
      case s: BasicStyle =>
        val styles = assureStyles()
        styles(s.title) = s.value
      case s: DelayedStyle =>
        setSpecialStyle(StyleKey.delayed)(s.title, s.value)
      case s: RemoveStyle =>
        setSpecialStyle(StyleKey.remove)(s.title, s.value)
      case s: DestroyStyle =>
        setSpecialStyle(StyleKey.destroy)(s.title, s.value)
      case a: AccumStyle =>
        val styles = assureStyles()
        val style = styles.raw(a.title)
        style.fold {
          styles(a.title) = a.value
        } { style =>
          styles(a.title) = a.accum(style.asInstanceOf[String], a.value): DataObject.StyleValue
        }
      case k: Key =>
        keyOption = k.value
      case e: Emitter =>
        val emitters = assureEmitters()
        val emitter = emitters.raw(e.eventType)
        emitter.fold {
          emitters(e.eventType) = e.trigger
        } { emitter =>
          emitters(e.eventType) = { ev => emitter(ev); e.trigger(ev) }
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
  val attrs: js.UndefOr[js.Dictionary[DataObject.AttrValue]],
  val props: js.UndefOr[js.Dictionary[DataObject.PropValue]],
  val styles: js.UndefOr[js.Dictionary[DataObject.StyleValue]],
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

  def from(appendModifiers: js.Array[_ <: VDomModifier])(implicit scheduler: Scheduler): NativeModifiers = {
    var lengths: js.UndefOr[js.Array[js.UndefOr[Int]]] = js.undefined
    var updaterObservables: js.UndefOr[js.Array[Observable[js.Array[StaticVDomModifier]]]] = js.undefined
    val modifiers = new js.Array[StaticVDomModifier]()

    @inline def appendModifier(mod: StaticVDomModifier): Unit = {
      modifiers += mod
      lengths.foreach { _ += 1 }
    }
    @inline def appendStream(stream: ValueObservable[js.Array[StaticVDomModifier]]): Unit = {
      val lengthsArr = lengths getOrElse assign(new js.Array[js.UndefOr[Int]](modifiers.length))(lengths = _)
      val observables = updaterObservables getOrElse assign(new js.Array[Observable[js.Array[StaticVDomModifier]]]())(updaterObservables = _)
      val index = lengthsArr.length
      stream.value match {
        case Some(value) =>
          modifiers ++= value
          lengthsArr += value.length
        case None =>
          lengthsArr += 0
      }
      observables += stream.observable.map { mods =>
        var i = 0
        var lengthBefore = 0
        while (i < index) {
          lengthBefore += lengthsArr(i) getOrElse 1
          i += 1
        }
        modifiers.splice(lengthBefore, lengthsArr(index) getOrElse 1, mods: _*)
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

    appendModifiers.foreach(inner)

    new NativeModifiers(modifiers, updaterObservables.map(obs => new CollectionObservable[js.Array[StaticVDomModifier]](obs)))
  }

  private def flattenModifierStream(modStream: ValueObservable[VDomModifier])(implicit scheduler: Scheduler): ValueObservable[js.Array[StaticVDomModifier]] = {
    @tailrec def findObservable(modifier: VDomModifier): Observable[js.Array[StaticVDomModifier]] = modifier match {
      case mod: StaticVDomModifier => Observable.now(js.Array(mod))
      case EmptyModifier => Observable.now(js.Array())
      case child: VNode  => Observable.now(js.Array(VNodeProxyNode(SnabbdomOps.toSnabbdom(child))))
      case child: StringVNode  => Observable.now(js.Array(VNodeProxyNode(VNodeProxy.fromString(child.text))))
      case mods: CompositeModifier =>
        val nativeModifiers = from(mods.modifiers)
        nativeModifiers.observable.fold(Observable.now(nativeModifiers.modifiers)) { obs =>
          Observable.concat(Observable.now(nativeModifiers.modifiers), obs)
        }
      case m: ModifierStreamReceiver =>
        val stream = flattenModifierStream(m.stream)
        Observable.concat(Observable.now(stream.value.getOrElse(js.Array())), stream.observable)
      case m: EffectModifier => findObservable(m.effect.unsafeRunSync())
      case m: SchedulerAction => findObservable(m.action(scheduler))
    }
    val observable = modStream.observable.switchMap[js.Array[StaticVDomModifier]](findObservable)

    @tailrec def findDefaultObservable(modifier: VDomModifier): ValueObservable[js.Array[StaticVDomModifier]] = modifier match {
      case mod: StaticVDomModifier => ValueObservable(observable, js.Array(mod))
      case EmptyModifier => ValueObservable(observable, js.Array())
      case child: VNode  => ValueObservable(observable, js.Array(VNodeProxyNode(SnabbdomOps.toSnabbdom(child))))
      case child: StringVNode  => ValueObservable(observable, js.Array(VNodeProxyNode(VNodeProxy.fromString(child.text))))
      case mods: CompositeModifier =>
        val nativeModifiers = from(mods.modifiers)
        val initialObservable = nativeModifiers.observable.fold(observable) { obs =>
          observable.publishSelector { observable =>
            Observable.merge(obs.takeUntil(observable), observable)
          }
        }
        ValueObservable(initialObservable, nativeModifiers.modifiers)
      case m: ModifierStreamReceiver =>
        val stream = flattenModifierStream(m.stream)
        val initialObservable = observable.publishSelector { observable =>
          Observable.merge(stream.observable.takeUntil(observable), observable)
        }
        ValueObservable(initialObservable, stream.value.getOrElse(js.Array()))
      case m: EffectModifier => findDefaultObservable(m.effect.unsafeRunSync())
      case m: SchedulerAction => findDefaultObservable(m.action(scheduler))
    }
    modStream.value.fold[ValueObservable[js.Array[StaticVDomModifier]]](ValueObservable(observable, js.Array()))(findDefaultObservable)
  }
}

private object StyleKey {
  @inline def delayed = "delayed"
  @inline def remove = "remove"
  @inline def destroy = "destroy"
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
