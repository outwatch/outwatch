package outwatch.dom.helpers

import cats.effect.IO
import monix.reactive.subjects.BehaviorSubject
import outwatch.dom._

import scala.collection.breakOut

object SeparatedModifiers {
  private[outwatch] def separate(modifiers: Seq[VDomModifier_]): SeparatedModifiers = {
    modifiers.foldRight(SeparatedModifiers())((m, sm) => m :: sm)
  }
}

private[outwatch] final case class SeparatedModifiers(
  properties: SeparatedProperties = SeparatedProperties(),
  emitters: SeparatedEmitters = SeparatedEmitters(),
  attributeReceivers: List[AttributeStreamReceiver] = Nil,
  children: Children = Children.Empty
) extends SnabbdomModifiers { self =>

  def ::(m: VDomModifier_): SeparatedModifiers = m match {
    case pr: Property => copy(properties = pr :: properties)
    case vn: ChildVNode => copy(children = vn :: children)
    case em: Emitter => copy(emitters = em :: emitters)
    case rc: AttributeStreamReceiver => copy(attributeReceivers = rc :: attributeReceivers)
    case cm: CompositeModifier => cm.modifiers.foldRight(self)((m, sm) => m :: sm)
    case sm: StringModifier => copy(children = sm :: children)
    case EmptyModifier => self
  }
}

private[outwatch] trait Children {
  def ::(mod: StringModifier): Children

  def ::(node: ChildVNode): Children

  def ensureKey: Children = this
}

object Children {
  private def toVNode(mod: StringModifier) = StringVNode(mod.string)
  private def toModifier(node: StringVNode) = StringModifier(node.string)

  private[outwatch] case object Empty extends Children {
    override def ::(mod: StringModifier): Children = StringModifiers(mod :: Nil)

    override def ::(node: ChildVNode): Children = node match {
      case s: StringVNode => toModifier(s) :: this
      case n => n :: VNodes(Nil, StreamStatus())
    }
  }

  private[outwatch] case class StringModifiers(modifiers: List[StringModifier]) extends Children {
    override def ::(mod: StringModifier): Children = copy(mod :: modifiers)

    override def ::(node: ChildVNode): Children = node match {
      case s: StringVNode => toModifier(s) :: this // this should never happen
      case n => n :: VNodes(modifiers.map(toVNode), StreamStatus())
    }
  }

  private[outwatch] case class VNodes(nodes: List[ChildVNode], streamStatus: StreamStatus) extends Children {

    private def ensureVTreeKey(vtree: VTree): VTree = {
      val defaultKey = Key(vtree.hashCode)
      val newModifiers = defaultKey +: vtree.modifiers
      vtree.copy(modifiers = newModifiers)
    }

    private def ensureVNodeKey[N >: VTree](node: N): N = node match {
      case vtree: VTree => ensureVTreeKey(vtree)
      case other => other
    }

    override def ensureKey: Children = if (streamStatus.hasChildOrChildren) copy(nodes = nodes.map(ensureVNodeKey)) else this

    override def ::(mod: StringModifier): Children = copy(toVNode(mod) :: nodes)

    override def ::(node: ChildVNode): Children = node match {
      case s: StaticVNode => copy(nodes = s :: nodes)
      case s: ChildStreamReceiver => copy(s :: nodes, streamStatus.copy(numChild = streamStatus.numChild + 1))
      case s: ChildrenStreamReceiver => copy(s :: nodes, streamStatus.copy(numChildren = streamStatus.numChildren + 1))
    }
  }

  private[outwatch] case class StreamStatus(numChild: Int = 0, numChildren: Int = 0) {
    def hasChildOrChildren: Boolean = (numChild + numChildren) > 0

    def hasMultipleChildren: Boolean = numChildren > 1
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

private[outwatch] final case class SeparatedAttributes(
  attributes: List[Attribute] = Nil
) extends SnabbdomAttributes {
  @inline def ::(a: Attribute): SeparatedAttributes = copy(attributes = a :: attributes)
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

private[outwatch] final case class Receivers(
  children: Children,
  attributeStreamReceivers: List[AttributeStreamReceiver]
) {
  private val (childNodes, childStreamStatus) = children match {
    case Children.VNodes(nodes, streamStatus) => (nodes, streamStatus)
    case _ => (Nil, Children.StreamStatus())
  }

  lazy val observable: Observable[(Seq[Attribute], Seq[IO[StaticVNode]])] = {
    val childStreamReceivers = if (childStreamStatus.hasChildOrChildren) {
      childNodes.foldRight(Observable(List.empty[IO[StaticVNode]])) {
        case (vn: StaticVNode, obs) => obs.combineLatestMap(BehaviorSubject(IO.pure(vn)))((nodes, n) => n :: nodes)
        case (csr: ChildStreamReceiver, obs) => obs.combineLatestMap(csr.childStream)((nodes, n) => n :: nodes)
        case (csr: ChildrenStreamReceiver, obs) =>
          obs.combineLatestMap(
            if (childStreamStatus.hasMultipleChildren) csr.childrenStream.startWith(Seq(Seq.empty)) else csr.childrenStream
          )((nodes, n) => n.toList ++ nodes)
      }
    } else {
      Observable(Seq.empty)
    }

    // only use last encountered observable per attribute
    val attributeReceivers: Observable[Seq[Attribute]] = if (attributeStreamReceivers.isEmpty) {
      Observable(Seq.empty)
    } else {
      Observable.combineLatestList(
        attributeStreamReceivers
          .groupBy(_.attribute)
          .values
          .map(_.last.attributeStream)(breakOut): _*
      )
    }

    attributeReceivers.combineLatest(childStreamReceivers)
  }

  lazy val nonEmpty: Boolean = {
    attributeStreamReceivers.nonEmpty || childStreamStatus.hasChildOrChildren
  }
}

