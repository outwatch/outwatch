package outwatch.dom.helpers

import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalajs.dom
import outwatch.dom._
import snabbdom.{Hooks, OutwatchState, VNodeProxy}

import scala.scalajs.js

private[outwatch] object SeparatedModifiers {
  def from(modifiers: js.Array[_ <: VDomModifier])(implicit scheduler: Scheduler) = {
    val m = new SeparatedModifiers()
    modifiers.foreach(m.append)
    m
  }


  def fromWithoutChildren(that: SeparatedModifiers)(implicit scheduler: Scheduler) = {
    val m = new SeparatedModifiers()
    import m._

    if (that.emitters.isDefined) {
      emitters = js.Dictionary[js.Function1[dom.Event, Unit]]()
      that.emitters.get.foreach { case (k, v) =>
        emitters.get(k) = v
      }
    }

    // keep out children intentionally

    hooks.insertHook = that.hooks.insertHook
    hooks.prePatchHook = that.hooks.prePatchHook
    hooks.updateHook = that.hooks.updateHook
    hooks.postPatchHook = that.hooks.postPatchHook
    hooks.destroyHook = that.hooks.destroyHook
    hooks.domUnmountHook = that.hooks.domUnmountHook

    if (that.attributes.attrs.isDefined) {
      attributes.attrs = js.Dictionary[Attr.Value]()
      that.attributes.attrs.get.foreach { case (k, v) =>
        attributes.attrs.get(k) = v
      }
    }
    if (that.attributes.props.isDefined) {
      attributes.props = js.Dictionary[Prop.Value]()
      that.attributes.props.get.foreach { case (k, v) =>
        attributes.props.get(k) = v
      }
    }
    if (that.attributes.styles.isDefined) {
      attributes.styles = js.Dictionary[Style.Value]()
      that.attributes.styles.get.foreach { case (k, v) =>
        if (k == StyleKey.delayed || k == StyleKey.destroy || k == StyleKey.remove) {
          val dict = js.Dictionary[String]()
          attributes.styles.get(k) = dict: Style.Value
          v.asInstanceOf[js.Dictionary[String]].foreach { case (k, v) =>
            dict(k) = v
          }
        } else {
          attributes.styles.get(k) = v
        }
      }
    }

    keyOption = that.keyOption

    outwatchState = that.outwatchState

    m
  }
}

private[outwatch] class SeparatedModifiers {
  var emitters: js.UndefOr[js.Dictionary[js.Function1[dom.Event, Unit]]] = js.undefined
  val children = new Children
  val attributes = new SeparatedAttributes()
  val hooks = new SeparatedHooks()
  var keyOption: js.UndefOr[Key.Value] = js.undefined
  var outwatchState: js.UndefOr[OutwatchState] = js.undefined

  def append(modifier: VDomModifier)(implicit scheduler: Scheduler): Unit = modifier match {
    case EmptyModifier =>
      ()
    case cm: CompositeModifier =>
      cm.modifiers.foreach(append(_))
    case s: VNodeProxyNode =>
      setProxy(s.proxy)
      setNode(s)
    case s: VNode =>
      val proxy = SnabbdomModifiers.toSnabbdom(s)
      setProxy(proxy)
      setNode(VNodeProxyNode(proxy))
    case s: StringVNode =>
      val proxy = VNodeProxy.fromString(s.string)
      setProxy(proxy)
      setNode(VNodeProxyNode(proxy))
    case s: ModifierStreamReceiver =>
      setNode(s)
      children.hasStream = true
    case a : BasicAttr =>
      if (attributes.attrs.isEmpty) attributes.attrs = js.Dictionary(a.title -> a.value)
      else attributes.attrs.get(a.title) = a.value
    case a : AccumAttr =>
      if (attributes.attrs.isEmpty) attributes.attrs = js.Dictionary(a.title -> a.value)
      else attributes.attrs.get(a.title) = attributes.attrs.get.get(a.title).fold(a.value)(a.accum(_, a.value))
    case p : Prop =>
      if (attributes.props.isEmpty) attributes.props = js.Dictionary(p.title -> p.value)
      else attributes.props.get(p.title) = p.value
    case s: BasicStyle =>
      if (attributes.styles.isEmpty) attributes.styles = js.Dictionary[Style.Value](s.title -> s.value)
      else attributes.styles.get(s.title) = s.value
    case s: DelayedStyle =>
      setSpecialStyle(StyleKey.delayed)(s.title, s.value)
    case s: RemoveStyle =>
      setSpecialStyle(StyleKey.remove)(s.title, s.value)
    case s: DestroyStyle =>
      setSpecialStyle(StyleKey.destroy)(s.title, s.value)
    case a: AccumStyle =>
      if (attributes.styles.isEmpty) attributes.styles = js.Dictionary[Style.Value](a.title -> a.value)
      else attributes.styles.get(a.title) = attributes.styles.get.get(a.title).fold[Style.Value](a.value)(s =>
        a.accum(s.asInstanceOf[String], a.value): Style.Value
      )
    case key: Key =>
      keyOption = key.value
    case em: Emitter =>
      if (emitters.isEmpty) {
        emitters = js.Dictionary[js.Function1[dom.Event, Unit]](em.eventType -> em.trigger)
      } else if (!emitters.get.contains(em.eventType)) {
        emitters.get(em.eventType) = em.trigger
      } else {
        val prev = emitters.get(em.eventType)
        emitters.get(em.eventType) = { ev => prev(ev); em.trigger(ev) }
      }
    case h: DomMountHook =>
      hooks.insertHook = createHooksSingle(hooks.insertHook, h.trigger)
      hooks.postPatchHook = createProxyHooksPair(hooks.postPatchHook, { (oldProxy, proxy) =>
        if (proxy.outwatchState.map(_.id) != oldProxy.outwatchState.map(_.id)) {
          proxy.elm.foreach(h.trigger)
        }
      })
      hooks.usesOutwatchState = true
    case h: DomUnmountHook =>
      hooks.destroyHook = createHooksSingle(hooks.destroyHook, h.trigger)
      hooks.domUnmountHook = createHooksSingle(hooks.domUnmountHook, h.trigger)
    case h: DomUpdateHook =>
      hooks.postPatchHook = createProxyHooksPair(hooks.postPatchHook, { (oldproxy, proxy) =>
        if (proxy.outwatchState.map(_.id) == oldproxy.outwatchState.map(_.id)) {
          proxy.elm.foreach(h.trigger)
        }
      })
      hooks.usesOutwatchState = true
    case h: InsertHook =>
      hooks.insertHook = createHooksSingle(hooks.insertHook, h.trigger)
    case h: PrePatchHook =>
      hooks.prePatchHook = createHooksPairOption(hooks.prePatchHook, h.trigger)
    case h: UpdateHook =>
      hooks.updateHook = createHooksPair(hooks.updateHook, h.trigger)
    case h: PostPatchHook =>
      hooks.postPatchHook = createHooksPair(hooks.postPatchHook, h.trigger)
    case h: DestroyHook =>
      hooks.destroyHook = createHooksSingle(hooks.destroyHook, h.trigger)
    case EffectModifier(effect) =>
      append(effect.unsafeRunSync())
  }

  def appendUnmountHook(): Unit = {
    hooks.postPatchHook = createProxyHooksPair(hooks.postPatchHook, { (oldProxy, proxy) =>
      if (proxy.outwatchState.map(_.id) != oldProxy.outwatchState.map(_.id)) {
        oldProxy.outwatchState.foreach(_.domUnmountHook.foreach(_(oldProxy)))
      }
    })
  }

  private def setProxy(child: VNodeProxy): Unit =
      if (children.proxies.isEmpty) {
        children.proxies = new js.Array[VNodeProxy](1)
        children.proxies.get(0) = child
      } else children.proxies.get += child
  private def setNode(child: ChildVNode): Unit =
    if (children.nodes.isEmpty) {
      children.nodes = new js.Array[ChildVNode](1)
      children.nodes.get(0) = child
    } else children.nodes.get += child
  private def setSpecialStyle(styleName: String)(title: String, value: String): Unit =
    if (attributes.styles.isEmpty) {
      attributes.styles = js.Dictionary[Style.Value](styleName -> js.Dictionary(title -> value))
    } else if (!attributes.styles.contains(styleName)) {
      attributes.styles.get(styleName) = js.Dictionary[String](title -> value): Style.Value
    } else {
      attributes.styles.get(styleName).asInstanceOf[js.Dictionary[String]](title) = value
    }

  private def createProxyHooksSingle(current: js.UndefOr[Hooks.HookSingleFn], hook: VNodeProxy => Unit): Hooks.HookSingleFn =
    if (current.isEmpty) hook
    else { p => current.get(p); hook(p) }
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
  var proxies: js.UndefOr[js.Array[VNodeProxy]] = js.undefined
  var nodes: js.UndefOr[js.Array[ChildVNode]] = js.undefined
  var hasStream: Boolean = false
}

private[outwatch] class SeparatedAttributes {
  var attrs: js.UndefOr[js.Dictionary[Attr.Value]] = js.undefined
  var props: js.UndefOr[js.Dictionary[Prop.Value]] = js.undefined
  var styles: js.UndefOr[js.Dictionary[Style.Value]] = js.undefined
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

private[outwatch] sealed trait ContentKind
private[outwatch] object ContentKind {
  case class Dynamic(observable: Observable[VDomModifier], initialValue: VDomModifier) extends ContentKind
  case class Static(modifier: VDomModifier) extends ContentKind
}

// StreamableModifiers takes a list of modifiers. It constructs an Observable
// of updates from dynamic modifiers in this list.
private[outwatch] class StreamableModifiers(modifiers: js.Array[_ <: VDomModifier])(implicit scheduler: Scheduler) {

  //TODO: hidden signature of this method (we need StaticModifier as a type)
  //handleStreamedModifier: Modifier => Either[StaticModifier, Observable[StaticModifier]]
  private def handleStreamedModifier(modifier: VDomModifier)(implicit scheduler: Scheduler): ContentKind = modifier match {
    case ModifierStreamReceiver(modStream) =>
      val observable = modStream.observable.switchMap[VDomModifier] { mod =>
        handleStreamedModifier(mod) match {
          //TODO: why is startWith different and leaks a subscription? see tests with: stream.startWith(initialValue :: Nil)
          case ContentKind.Dynamic(stream, initialValue) => Observable.concat(Observable.now(initialValue), stream)
          case ContentKind.Static(mod) => Observable.now(mod)
        }
      }

      handleStreamedModifier(modStream.value.getOrElse(EmptyModifier)) match {
        case ContentKind.Dynamic(initialObservable, mod) =>
          val combinedObservable = observable.publishSelector { observable =>
            Observable.merge(initialObservable.takeUntil(observable), observable)
          }
          ContentKind.Dynamic(combinedObservable, mod)
        case ContentKind.Static(mod) =>
          ContentKind.Dynamic(observable, mod)
      }

    case CompositeModifier(modifiers) if (modifiers.nonEmpty) =>
      val streamableModifiers = new StreamableModifiers(modifiers)
      if (streamableModifiers.updaterObservables.isEmpty) {
        ContentKind.Static(CompositeModifier(modifiers))
      } else {
        ContentKind.Dynamic(
          streamableModifiers.observable.map(CompositeModifier(_)),
          CompositeModifier(streamableModifiers.initialModifiers))
      }

    case child: VNode  => ContentKind.Static(VNodeProxyNode(SnabbdomModifiers.toSnabbdom(child)))
    case child: StringVNode  => ContentKind.Static(VNodeProxyNode(VNodeProxy.fromString(child.string)))

    case EffectModifier(effect) => handleStreamedModifier(effect.unsafeRunSync())

    case mod => ContentKind.Static(mod)
  }

  // the nodes array has a fixed size - each static child node is one element
  // and the dynamic nodes can place one element on each update and start with
  // EmptyModifier, and we reserve an array element for each attribute
  // receiver.
  val initialModifiers = new js.Array[VDomModifier](modifiers.size)

  // for each node which might be dynamic, we have an Observable of Modifier updates
  val updaterObservables = new js.Array[Observable[(Int, VDomModifier)]]

  // fill the initial state and updater observables
  {
    var i = 0
    var j = 0
    while (i < modifiers.size) {
      val index = i
      val jndex = j
      handleStreamedModifier(modifiers(index)) match {
        case ContentKind.Dynamic(stream, initialValue) =>
          initialModifiers(index) = initialValue
          updaterObservables(jndex) = stream.map { mod =>
            (index, mod)
          }
          j += 1
        case ContentKind.Static(mod) =>
          initialModifiers(index) = mod
      }
      i += 1
    }
  }

  // an observable representing the current state of this VNode. We take all
  // state update functions we have from dynamic modifiers and then scan over
  // them starting with the initial state. We do not actually, but mutate the
  // initial modifiers because of performance considerations.
  val observable = Observable.merge(updaterObservables: _*).map { case (index, mod) =>
    initialModifiers(index) = mod
    initialModifiers
  }
}

// Receivers represent a VNode with its static/streamable children and its
// attribute streams. it is about capturing the dynamic content of a node.
// it is considered "empty" if it is only static. Otherwise it provides an
// Observable to stream the current modifiers of this node.
private[outwatch] final class Receivers(childNodes: js.Array[ChildVNode])(implicit scheduler: Scheduler) {
  private val streamableModifiers = new StreamableModifiers(childNodes)
  def initialState: js.Array[VDomModifier] = streamableModifiers.initialModifiers
  def observable: Observable[js.Array[VDomModifier]] = streamableModifiers.observable
}
