package outwatch.dom.helpers

import monix.reactive.Observable
import outwatch.dom._

import scala.collection.breakOut

object SeparatedModifiers {
  private[outwatch] def from(modifiers: Seq[Modifier]): SeparatedModifiers = {
    modifiers.foldRight(SeparatedModifiers())((m, sm) => m :: sm)
  }
}

private[outwatch] final case class SeparatedModifiers(
  properties: SeparatedProperties = SeparatedProperties(),
  emitters: SeparatedEmitters = SeparatedEmitters(),
  children: Children = Children.empty
) extends SnabbdomModifiers { self =>

  def ::(m: Modifier): SeparatedModifiers = m match {
    case pr: Property => copy(properties = pr :: properties)
    case vn: ChildVNode => copy(children = vn :: children)
    case em: Emitter => copy(emitters = em :: emitters)
    case cm: CompositeModifier => cm.modifiers.foldRight(self)((m, sm) => m :: sm)
    case EmptyModifier => self
  }
}

//TODO: Children is not a good name, as these can be stringmodifiers or vnodes or modifier streams.
// So this is not neccessarily a child node.
private[outwatch] case class Children(nodes: List[ChildVNode], hasStream: Boolean, hasVTree: Boolean) {
  private def ensureVNodeKey[N >: VTree](node: N): N = node match {
    case vtree: VTree => vtree.copy(modifiers = Key(vtree.hashCode) +: vtree.modifiers)
    case other => other
  }

  def ensureKey: Children = if (hasStream && hasVTree) copy(nodes = nodes.map(ensureVNodeKey)) else this

  def ::(node: ChildVNode): Children = node match {
    case s: StringVNode => copy(nodes = s :: nodes)
    case s: VTree => copy(nodes = s :: nodes, hasVTree = true)
    case s: ModifierStreamReceiver => copy(nodes = s :: nodes, hasStream = true)
  }
}
object Children {
  def empty = Children(Nil, hasStream = false, hasVTree = false)
}

private[outwatch] final case class SeparatedProperties(
  attributes: SeparatedAttributes = SeparatedAttributes(),
  hooks: SeparatedHooks = SeparatedHooks(),
  keys: List[Key] = Nil
) {
  def ::(p: Property): SeparatedProperties = p match {
    case att: Attribute => copy(attributes = att :: attributes)
    case hook: Hook[_] => copy(hooks = hook :: hooks)
    case key: Key => copy(keys = key :: keys)
  }
}

private[outwatch] final case class SeparatedStyles(
  styles: List[Style] = Nil
) extends SnabbdomStyles {
  @inline def ::(s: Style): SeparatedStyles = copy(styles = s :: styles)
}


private[outwatch] final case class SeparatedAttributes(
  attrs: List[Attr] = Nil,
  props: List[Prop] = Nil,
  styles: SeparatedStyles = SeparatedStyles()
) extends SnabbdomAttributes {
  @inline def ::(a: Attribute): SeparatedAttributes = a match {
    case a : Attr => copy(attrs = a :: attrs)
    case p : Prop => copy(props = p :: props)
    case s : Style => copy(styles= s :: styles)
  }
}
object SeparatedAttributes {
  private[outwatch] def from(attributes: Seq[Attribute]): SeparatedAttributes = {
    attributes.foldRight(SeparatedAttributes())((a, sa) => a :: sa)
  }
}

private[outwatch] final case class SeparatedHooks(
  insertHooks: List[InsertHook] = Nil,
  prePatchHooks: List[PrePatchHook] = Nil,
  updateHooks: List[UpdateHook] = Nil,
  postPatchHooks: List[PostPatchHook] = Nil,
  destroyHooks: List[DestroyHook] = Nil
) extends SnabbdomHooks {
  def ::(h: Hook[_]): SeparatedHooks = h match {
    case ih: InsertHook => copy(insertHooks = ih :: insertHooks)
    case pph: PrePatchHook => copy(prePatchHooks = pph :: prePatchHooks)
    case uh: UpdateHook => copy(updateHooks = uh :: updateHooks)
    case pph: PostPatchHook => copy(postPatchHooks = pph :: postPatchHooks)
    case dh: DestroyHook => copy(destroyHooks = dh :: destroyHooks)
  }
}

private[outwatch] final case class SeparatedEmitters(
  emitters: List[Emitter] = Nil
) extends SnabbdomEmitters {
  def ::(e: Emitter): SeparatedEmitters = copy(emitters = e :: emitters)
}

private[outwatch] sealed trait ContentKind
private[outwatch] object ContentKind {
  case class Dynamic(observable: Observable[Modifier], initialValue: Modifier) extends ContentKind
  case class Static(modifier: Modifier) extends ContentKind
}

// StreamableModifiers takes a list of modifiers. It constructs an Observable
// of updates from dynamic modifiers in this list.
private[outwatch] class StreamableModifiers(modifiers: Seq[Modifier]) {

  //TODO: hidden signature of this method (we need StaticModifier as a type)
  //handleStreamedModifier: Modifier => Either[StaticModifier, Observable[StaticModifier]]
  private val handleStreamedModifier: Modifier => ContentKind = {
    case ModifierStreamReceiver(modStream, initialValue) =>
      val observable = modStream.switchMap[Modifier] { mod =>
        handleStreamedModifier(mod.unsafeRunSync) match {
          //TODO: why is startWith different and leaks a subscription? stream.startWith(EmptyModifier :: Nil)
          case ContentKind.Dynamic(stream, defaultValue) => Observable.concat(Observable.now(defaultValue), stream)
          case ContentKind.Static(mod) => Observable.now(mod)
        }
      }

      val value = handleStreamedModifier(initialValue) match {
        case ContentKind.Dynamic(_, initialValue) => initialValue
        case ContentKind.Static(mod) => mod
      }

      ContentKind.Dynamic(observable, value)

    case CompositeModifier(modifiers) if (modifiers.nonEmpty) =>
      val streamableModifiers = new StreamableModifiers(modifiers)
      if (streamableModifiers.updaterObservables.isEmpty) {
        ContentKind.Static(CompositeModifier(modifiers))
      } else {
        ContentKind.Dynamic(
          streamableModifiers.observable
            .map(CompositeModifier(_)),
          CompositeModifier(streamableModifiers.initialModifiers))
      }


    case mod => ContentKind.Static(mod)
  }


  // the nodes array has a fixed size - each static child node is one element
  // and the dynamic nodes can place one element on each update and start with
  // EmptyModifier, and we reserve an array element for each attribute
  // receiver.
  val initialModifiers = new Array[Modifier](modifiers.size)

  // for each node which might be dynamic, we have an Observable of Modifier updates
  val updaterObservables = new collection.mutable.ArrayBuffer[Observable[Array[Modifier] => Array[Modifier]]]

  // an observable representing the current state of this VNode. We take all
  // state update functions we have from dynamic modifiers and then scan over
  // them starting with the initial state.
  val observable = {
    var i = 0;
    while (i < modifiers.size) {
      val index = i
      handleStreamedModifier(modifiers(index)) match {
        case ContentKind.Dynamic(stream, initialValue) =>
          initialModifiers(index) = initialValue
          updaterObservables += stream.map { mod =>
            (array: Array[Modifier]) => array.updated(index, mod)
          }
        case ContentKind.Static(mod) =>
          initialModifiers(index) = mod
      }
      i += 1
    }

    Observable.merge(updaterObservables: _*)
      .scan(initialModifiers)((modifiers, update) => update(modifiers))
  }
}

// Receivers represent a VNode with its static/streamable children and its
// attribute streams. it is about capturing the dynamic content of a node.
// it is considered "empty" if it is only static. Otherwise it provides an
// Observable to stream the current modifiers of this node.
private[outwatch] final case class Receivers(children: Children) {
  private val childNodes = {
    if (children.hasStream) children.nodes // only interested if there is dynamic content
    else Nil
  }

  private lazy val streamableModifiers = new StreamableModifiers(childNodes)
  def initialState: Array[Modifier] = streamableModifiers.initialModifiers
  def observable: Observable[Array[Modifier]] = streamableModifiers.observable

  // it is empty if there is no dynamic modifier, i.e., no observables.
  def nonEmpty: Boolean = childNodes.nonEmpty
}
