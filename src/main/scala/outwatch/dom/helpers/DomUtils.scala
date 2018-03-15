package outwatch.dom.helpers

import cats.Applicative
import cats.effect.Effect
import outwatch.dom._
import outwatch.dom.dsl.attributes

import scala.collection.breakOut

object SeparatedModifiers {
  private[outwatch] def from[F[+_]: Effect](modifiers: Seq[Modifier]): SeparatedModifiers[F] = {
    modifiers.foldRight(SeparatedModifiers[F](children = Children.empty[F]))((m, sm) => m :: sm)
  }
}

private[outwatch] final case class SeparatedModifiers[F[+_]: Effect](
  properties: SeparatedProperties = SeparatedProperties(),
  emitters: SeparatedEmitters = SeparatedEmitters(),
  attributeReceivers: List[AttributeStreamReceiver] = Nil,
  children: Children[F]
) extends SnabbdomModifiers[F] { self =>

  def ::(m: Modifier): SeparatedModifiers[F] = m match {
    case pr: Property => copy(properties = pr :: properties)
    case vn: ChildVNode => copy(children = vn :: children)
    case em: Emitter => copy(emitters = em :: emitters)
    case rc: AttributeStreamReceiver => copy(attributeReceivers = rc :: attributeReceivers)
    case cm: CompositeModifier => cm.modifiers.foldRight(self)((m, sm) => m :: sm)
    case sm: StringModifier => copy(children = sm :: children)
    case EmptyModifier => self
  }
}

private[outwatch] trait Children[F[+_]] {
  def ::(mod: StringModifier): Children[F]

  def ::(node: ChildVNode): Children[F]

  def ensureKey: Children[F] = this
}

object Children {
  private def toVNode(mod: StringModifier) = StringVNode(mod.string)
  private def toModifier(node: StringVNode) = StringModifier(node.string)

  private[outwatch] case class Empty[F[+_]: Effect]() extends Children[F] {
    override def ::(mod: StringModifier): Children[F] = StringModifiers(mod :: Nil)

    override def ::(node: ChildVNode): Children[F] = node match {
      case s: StringVNode => toModifier(s) :: this
      case n => n :: VNodes[F](Nil, hasStream = false)
    }
  }

  def empty[F[+_]: Effect]: Empty[F] = Empty[F]()

  private[outwatch] case class StringModifiers[F[+_]: Effect](modifiers: List[StringModifier]) extends Children[F] {
    override def ::(mod: StringModifier): Children[F] = copy(mod :: modifiers)

    override def ::(node: ChildVNode): Children[F] = node match {
      case s: StringVNode => toModifier(s) :: this // this should never happen
      case n => n :: VNodes[F](modifiers.map(toVNode), hasStream = false)
    }
  }

  private[outwatch] case class VNodes[F[+_]: Effect](nodes: List[ChildVNode], hasStream: Boolean) extends Children[F] {

    private def ensureVNodeKey[N >: VTree[F]](node: N): N = node match {
      case vtree: VTree[_] => vtree.copy[F](modifiers = Key(vtree.hashCode) +: vtree.modifiers)
      case other => other
    }

    override def ensureKey: Children[F] = if (hasStream) copy(nodes = nodes.map(ensureVNodeKey)) else this

    override def ::(mod: StringModifier): Children[F] = copy(toVNode(mod) :: nodes)

    override def ::(node: ChildVNode): Children[F] = node match {
      case s: StaticVNode => copy(nodes = s :: nodes)
      case s: StreamVNode => copy(nodes = s :: nodes, hasStream = true)
    }
  }
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
    case EmptyAttribute => this
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


private[outwatch] case class VNodeState[F[+_]](
  nodes: Array[Seq[F[StaticVNode]]],
  attributes: Map[String, Attribute] = Map.empty
)

private[outwatch] object VNodeState {
  def from[F[+_]: Applicative](nodes: List[ChildVNode]): VNodeState[F] = VNodeState[F](
    nodes.map {
      case n: StaticVNode => List(Applicative[F].pure(n))
      case _ => List.empty
    }(breakOut)
  )

  type Updater[F[+_]] = VNodeState[F] => VNodeState[F]
}

private[outwatch] final case class Receivers[F[+_]: Effect](
  children: Children[F],
  attributeStreamReceivers: List[AttributeStreamReceiver]
) {
  private val (nodes, hasNodeStreams) = children match {
    case Children.VNodes(nodes, hasStream) => (nodes, hasStream)
    case _ => (Nil, false)
  }

  private def updaters: Seq[Observable[VNodeState.Updater[F]]] = nodes.zipWithIndex.map {
    case (_: StaticVNode, _) =>
      Observable.empty
    case (csr: ChildStreamReceiver[F], index) =>
      csr.childStream.map[VNodeState.Updater[F]](n => s => s.copy(nodes = s.nodes.updated(index, n :: Nil)))
    case (csr: ChildrenStreamReceiver[F], index) =>
      csr.childrenStream.map[VNodeState.Updater[F]](n => s => s.copy(nodes = s.nodes.updated(index, n)))
  } ++ attributeStreamReceivers.groupBy(_.attribute).values.map(_.last).map {
    case AttributeStreamReceiver(name, attributeStream) =>
      attributeStream.map[VNodeState.Updater[F]](a => s => s.copy(attributes = s.attributes.updated(name, a)))
  }

  def observable: Observable[VNodeState[F]] = Observable.merge(updaters: _*)
    .scan(VNodeState.from[F](nodes))((state, updater) => updater(state))

  lazy val nonEmpty: Boolean = {
    hasNodeStreams || attributeStreamReceivers.nonEmpty
  }
}