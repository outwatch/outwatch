package outwatch.dom.helpers

import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalajs.dom
import outwatch.dom._
import snabbdom.Hooks

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

    that.emitters.foreach { case (k, v) =>
      emitters(k) = v
    }

    // keep out children intentionally

    hooks.insertHook = that.hooks.insertHook
    hooks.prePatchHook = that.hooks.prePatchHook
    hooks.updateHook = that.hooks.updateHook
    hooks.postPatchHook = that.hooks.postPatchHook
    hooks.destroyHook = that.hooks.destroyHook
    hooks.domMountHook = that.hooks.domMountHook
    hooks.domUnmountHook = that.hooks.domUnmountHook
    hooks.domUpdateHook = that.hooks.domUpdateHook

    that.attributes.attrs.foreach { case (k, v) =>
      attributes.attrs(k) = v
    }
    that.attributes.props.foreach { case (k, v) =>
      attributes.props(k) = v
    }
    that.attributes.styles.foreach { case (k, v) =>
      //TODO: copy over js.Dictionary in styles
      attributes.styles(k) = v
    }

    keyOption = that.keyOption orElse keyOption

    m
  }
}

private[outwatch] class SeparatedModifiers {
  //TODO: size hints for arrays
  val emitters = js.Dictionary[js.Function1[dom.Event, Unit]]()
  val children = new Children
  val attributes = new SeparatedAttributes()
  val hooks = new SeparatedHooks()
  var keyOption: js.UndefOr[Key.Value] = js.undefined

  def append(modifier: VDomModifier)(implicit scheduler: Scheduler): Unit = modifier match {
    case EmptyModifier =>
      ()
    case cm: CompositeModifier =>
      cm.modifiers.foreach(append(_))
    case s: VNode =>
      children.nodes += VNodeProxyNode(s.toSnabbdom)
      children.hasVTree = true
    case s: VNodeProxyNode =>
      children.nodes += s
      children.hasVTree = true
    case s: StringVNode =>
      children.nodes += s
    case s: ModifierStreamReceiver =>
      children.nodes += s
      children.hasStream = true
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
    case key: Key =>
      keyOption = key.value
    case em: Emitter =>
      if (!emitters.contains(em.eventType)) {
        emitters(em.eventType) = em.trigger
      } else {
        val prev = emitters(em.eventType)
        emitters(em.eventType) = { ev => prev(ev); em.trigger(ev) }
      }
    case h: DomMountHook =>
      hooks.domMountHook = createHooksSingle(hooks.domMountHook, h)
    case h: DomUnmountHook =>
      hooks.domUnmountHook = createHooksSingle(hooks.domUnmountHook, h)
    case h: DomUpdateHook =>
      hooks.domUpdateHook = createHooksSingle(hooks.domUpdateHook, h)
    case h: InsertHook =>
      hooks.insertHook = createHooksSingle(hooks.insertHook, h)
    case h: PrePatchHook =>
      hooks.prePatchHook = createHooksPairOption(hooks.prePatchHook, h)
    case h: UpdateHook =>
      hooks.updateHook = createHooksPair(hooks.updateHook, h)
    case h: PostPatchHook =>
      hooks.postPatchHook = createHooksPair(hooks.postPatchHook, h)
    case h: DestroyHook =>
      hooks.destroyHook = createHooksSingle(hooks.destroyHook, h)
    case EffectModifier(effect) =>
      append(effect.unsafeRunSync())
  }

  private def setSpecialStyle(styleName: String)(title: String, value: String): Unit =
    if (!attributes.styles.contains(styleName)) {
      attributes.styles(styleName) = js.Dictionary[String](title -> value): Style.Value
    } else {
      attributes.styles(styleName).asInstanceOf[js.Dictionary[String]](title) = value
    }

  private def createEmitter(current: js.UndefOr[Hooks.HookSingleFn], hook: Hook[dom.Element]): Hooks.HookSingleFn =
    if (current.isEmpty) { p => p.elm.foreach(hook.trigger) }
    else { p => current.get(p); p.elm.foreach(hook.trigger) }
  private def createHooksSingle(current: js.UndefOr[Hooks.HookSingleFn], hook: Hook[dom.Element]): Hooks.HookSingleFn =
    if (current.isEmpty) { p => p.elm.foreach(hook.trigger) }
    else { p => current.get(p); p.elm.foreach(hook.trigger) }
  private def createHooksPair(current: js.UndefOr[Hooks.HookPairFn], hook: Hook[(dom.Element, dom.Element)]): Hooks.HookPairFn =
    if (current.isEmpty) { (o,p) => for { oe <- o.elm; pe <- p.elm } hook.trigger((oe, pe)) }
    else { (o,p) => current.get(o, p); for { oe <- o.elm; pe <- p.elm } hook.trigger((oe, pe)) }: Hooks.HookPairFn
  private def createHooksPairOption(current: js.UndefOr[Hooks.HookPairFn], hook: Hook[(Option[dom.Element], Option[dom.Element])]): Hooks.HookPairFn =
    if (current.isEmpty) { (o,p) => hook.trigger((o.elm.toOption, p.elm.toOption)) }
    else { (o,p) => current.get(o, p); hook.trigger((o.elm.toOption, p.elm.toOption)) }
}

private[outwatch] object StyleKey {
  def delayed = "delayed"
  def remove = "remove"
  def destroy = "destroy"
}

private[outwatch] class Children {
  val nodes = new js.Array[ChildVNode]()
  var hasStream: Boolean = false
  var hasVTree: Boolean = false
}

private[outwatch] class SeparatedAttributes {
  val attrs = js.Dictionary[Attr.Value]()
  val props = js.Dictionary[Prop.Value]()
  val styles = js.Dictionary[Style.Value]()
}

private[outwatch] class SeparatedHooks {
  var insertHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var prePatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var updateHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var postPatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var destroyHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var domMountHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var domUnmountHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var domUpdateHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
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

    case EffectModifier(effect) => handleStreamedModifier(effect.unsafeRunSync())

    case child: StaticVNode  => ContentKind.Static(VNodeProxyNode(child.toSnabbdom))

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
