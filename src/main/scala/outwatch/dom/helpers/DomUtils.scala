package outwatch.dom.helpers

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
  attributeReceivers: List[AttributeStreamReceiver] = Nil,
  children: Children = Children.Empty
) extends SnabbdomModifiers { self =>

  def ::(m: Modifier): SeparatedModifiers = m match {
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
      case n => n :: VNodes(Nil, hasStream = false)
    }
  }

  private[outwatch] case class StringModifiers(modifiers: List[StringModifier]) extends Children {
    override def ::(mod: StringModifier): Children = copy(mod :: modifiers)

    override def ::(node: ChildVNode): Children = node match {
      case s: StringVNode => toModifier(s) :: this // this should never happen
      case n => n :: VNodes(modifiers.map(toVNode), hasStream = false)
    }
  }

  private[outwatch] case class VNodes(nodes: List[ChildVNode], hasStream: Boolean) extends Children {

    private def ensureVNodeKey[N >: VTree](node: N): N = node match {
      case vtree: VTree => vtree.copy(modifiers = Key(vtree.hashCode) +: vtree.modifiers)
      case other => other
    }

    override def ensureKey: Children = if (hasStream) copy(nodes = nodes.map(ensureVNodeKey)) else this

    override def ::(mod: StringModifier): Children = copy(toVNode(mod) :: nodes)

    override def ::(node: ChildVNode): Children = node match {
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


private[outwatch] case class VNodeState(modifiers: Array[Modifier], attributes: Array[Attribute]) // TODO: one array!

private[outwatch] object VNodeState {
  def from(nodes: List[ChildVNode], attributes: Array[Attribute]): VNodeState = VNodeState(
    nodes.map {
      case n: StaticVNode => n
      case _ => EmptyModifier
    }(breakOut), attributes
  )

  type Updater = VNodeState => VNodeState
}

private[outwatch] final case class Receivers(
  children: Children,
  attributeStreamReceivers: List[AttributeStreamReceiver]
) {
  private val (nodes, hasNodeStreams) = children match {
    case Children.VNodes(nodes, hasStream) => (nodes, hasStream)
    case _ => (Nil, false)
  }

  private val uniqueAttributeReceivers: List[AttributeStreamReceiver] = attributeStreamReceivers
    .groupBy(_.attribute).values.map(_.last)(breakOut)

  private lazy val updaters: Seq[Observable[VNodeState.Updater]] = nodes.zipWithIndex.map {
    case (_: StaticVNode, _) =>
      Observable.empty
    case (msr: ModifierStreamReceiver, index) =>
      msr.stream.switchMap[VNodeState.Updater] { vdomMod =>
        val mod = vdomMod.unsafeRunSync
        mod match {
          case _: AttributeStreamReceiver | _: ModifierStreamReceiver | _: CompositeModifier => //TODO: have trait for streaming nodes?
            // TODO can we do this better? we need to get nested streams inside this modifier
            // we are calculating separated modifiers here just to get the observable and do not reuse the result
            // at least we could remove streamchildren from the next VNodeState.Updater
            val seps = SeparatedModifiers.from(List(mod))
            val receivers = Receivers(seps.children.ensureKey, seps.attributeReceivers)
            val initialUpdate: VNodeState.Updater = s => s.copy(modifiers = s.modifiers.updated(index, mod))
            if (receivers.nonEmpty) receivers.observable.map[VNodeState.Updater] { innerState =>
              s => s.copy(modifiers = s.modifiers.updated(index, CompositeModifier(mod +: (innerState.modifiers ++ innerState.attributes))))
            }.startWith(Seq(initialUpdate)) else Observable(initialUpdate)
          case _ => Observable(s => s.copy(modifiers = s.modifiers.updated(index, mod)))
        }
      }
  } ++ uniqueAttributeReceivers.zipWithIndex.map {
    case (AttributeStreamReceiver(_, attributeStream), index) =>
      attributeStream.map[VNodeState.Updater](a => s => s.copy(attributes = s.attributes.updated(index, a)))
  }

  lazy val observable: Observable[VNodeState] = Observable.merge(updaters: _*)
    .scan(VNodeState.from(nodes, Array.fill(uniqueAttributeReceivers.size)(Attribute.empty)))((state, updater) => updater(state))

  lazy val nonEmpty: Boolean = {
    hasNodeStreams || uniqueAttributeReceivers.nonEmpty
  }
}
