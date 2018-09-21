package outwatch.dom

import cats.effect.IO
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observer
import org.scalajs.dom._
import outwatch.AsVDomModifier
import outwatch.dom.helpers.{SeparatedModifiers, SnabbdomModifiers}
import snabbdom.{DataObject, VNodeProxy}

import scala.scalajs.js
import scala.concurrent.Future

sealed trait VDomModifier

object VDomModifier {
  val empty: VDomModifier = EmptyModifier

  def apply(modifier: VDomModifier, modifier2: VDomModifier, modifiers: VDomModifier*): VDomModifier =
    CompositeModifier(js.Array(modifier, modifier2) ++ modifiers)
  def apply[T](t: T)(implicit as: AsVDomModifier[T]): VDomModifier = as.asVDomModifier(t)
}

// Modifiers

sealed trait Property extends VDomModifier

final case class Emitter(eventType: String, trigger: Event => Unit) extends VDomModifier

final case class CompositeModifier(modifiers: js.Array[_ <: VDomModifier]) extends VDomModifier

case object EmptyModifier extends VDomModifier

sealed trait ChildVNode extends VDomModifier

// Properties

final case class Key(value: Key.Value) extends Property
object Key {
  type Value = DataObject.KeyValue
}

sealed trait Attribute extends Property {
  val title: String
}
object Attribute {
  def apply(title: String, value: Attr.Value): Attribute = BasicAttr(title, value)
}

// Attributes

sealed trait Attr extends Attribute {
  val value: Attr.Value
}
object Attr {
  type Value = DataObject.AttrValue
}

final case class BasicAttr(title: String, value: Attr.Value) extends Attr

/**
  * Attribute that accumulates the previous value in the same VNode with it's value
  */
final case class AccumAttr(title: String, value: Attr.Value, accum: (Attr.Value, Attr.Value)=> Attr.Value) extends Attr

final case class Prop(title: String, value: Prop.Value) extends Attribute
object Prop {
  type Value = DataObject.PropValue
}

sealed trait Style extends Attribute {
  val value: String
}
object Style {
  type Value = DataObject.StyleValue
}

final case class AccumStyle(title: String, value: String, accum: (String, String) => String) extends Style

final case class BasicStyle(title: String, value: String) extends Style
final case class DelayedStyle(title: String, value: String) extends Style
final case class RemoveStyle(title: String, value: String) extends Style
final case class DestroyStyle(title: String, value: String) extends Style

// Hooks

sealed trait Hook extends Property
private[outwatch] final case class DomMountHook(trigger: Element => Unit) extends Hook
private[outwatch] final case class DomUnmountHook(trigger: Element => Unit) extends Hook
private[outwatch] final case class DomUpdateHook(trigger: Element => Unit) extends Hook

private[outwatch] final case class InsertHook(trigger: Element => Unit) extends Hook
private[outwatch] final case class PrePatchHook(trigger: ((Option[Element], Option[Element])) => Unit) extends Hook
private[outwatch] final case class UpdateHook(trigger: ((Element, Element)) => Unit) extends Hook
private[outwatch] final case class PostPatchHook(trigger: ((Element, Element)) => Unit) extends Hook
private[outwatch] final case class DestroyHook(trigger: Element => Unit) extends Hook


private[outwatch] case class EffectModifier(effect: IO[VDomModifier]) extends VDomModifier

// Child Nodes

final case class ModifierStreamReceiver(stream: ValueObservable[VDomModifier]) extends ChildVNode

// Static Nodes
private[outwatch] final case class VNodeProxyNode(proxy: VNodeProxy) extends ChildVNode

private[outwatch] final case class StringVNode(string: String) extends ChildVNode

final case class VNode(nodeType: String, modifiers: js.Array[VDomModifier]) extends ChildVNode {

  def apply(args: VDomModifier*): VNode = copy(modifiers = modifiers ++ args)
}
