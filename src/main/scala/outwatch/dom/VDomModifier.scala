package outwatch.dom

import cats.effect.IO
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observer
import org.scalajs.dom._
import outwatch.dom.helpers.SeparatedModifiers
import snabbdom.{DataObject, VNodeProxy}

import scala.concurrent.Future

/*
VDomModifier_
  Property
    Attribute
      TitledAttribute
        Attr
          BasicAttr
          AccumAttr
        Prop
        Style
          BasicStyle
          DelayedStyle
          RemoveStyle
          DestroyStyle
          AccumStyle
      EmptyAttribute
    Hook
      InsertHook
      PrePatchHook
      UpdateHook
      PostPatchHook
      DestroyHook
    Key
  ChildVNode
    StaticVNode
      StringVNode
      VTree
    ChildStreamReceiver
    ChildrenStreamReceiver
  Emitter
  AttributeStreamReceiver
  CompositeModifier
  StringModifier
  EmptyModifier
 */


sealed trait VDomModifier_ extends Any

// Modifiers

sealed trait Property extends VDomModifier_

final case class Emitter(eventType: String, trigger: Event => Future[Ack]) extends VDomModifier_

private[outwatch] final case class AttributeStreamReceiver(attribute: String, attributeStream: Observable[Attribute]) extends VDomModifier_

private[outwatch] final case class CompositeModifier(modifiers: Seq[VDomModifier_]) extends VDomModifier_

case object EmptyModifier extends VDomModifier_

private[outwatch] final case class StringModifier(string: String) extends VDomModifier_

sealed trait ChildVNode extends Any with VDomModifier_

// Properties

final case class Key(value: Key.Value) extends Property
object Key {
  type Value = DataObject.KeyValue
}

sealed trait Attribute extends Property
object Attribute {
  def apply(title: String, value: Attr.Value): Attribute = BasicAttr(title, value)

  val empty: Attribute = EmptyAttribute
}


sealed trait Hook[T] extends Property {
  def observer: Observer[T]
}

// Attributes

private[outwatch] case object EmptyAttribute extends Attribute

sealed trait TitledAttribute extends Attribute {
  val title: String
}


sealed trait Attr extends TitledAttribute {
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

final case class Prop(title: String, value: Prop.Value) extends TitledAttribute
object Prop {
  type Value = DataObject.PropValue
}

sealed trait Style extends TitledAttribute {
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

private[outwatch] final case class InsertHook(observer: Observer[Element]) extends Hook[Element]
private[outwatch] final case class PrePatchHook(observer: Observer[(Option[Element], Option[Element])])
  extends Hook[(Option[Element], Option[Element])]
private[outwatch] final case class UpdateHook(observer: Observer[(Element, Element)]) extends Hook[(Element, Element)]
private[outwatch] final case class PostPatchHook(observer: Observer[(Element, Element)]) extends Hook[(Element, Element)]
private[outwatch] final case class DestroyHook(observer: Observer[Element]) extends Hook[Element]

// Child Nodes
sealed trait StaticVNode extends Any with ChildVNode {
  def asProxy(implicit s: Scheduler): VNodeProxy
}
object StaticVNode {
  val empty: StaticVNode = StringVNode("")
}

final case class ChildStreamReceiver(childStream: Observable[IO[StaticVNode]]) extends ChildVNode
final case class ChildrenStreamReceiver(childrenStream: Observable[Seq[IO[StaticVNode]]]) extends ChildVNode

// Static Nodes
private[outwatch] final case class StringVNode(string: String) extends AnyVal with StaticVNode {
  override def asProxy(implicit s: Scheduler): VNodeProxy = VNodeProxy.fromString(string)
}

// TODO: instead of Seq[VDomModifier] use Vector or JSArray?
// Fast concatenation and lastOption operations are important
// Needs to be benchmarked in the Browser
private[outwatch] final case class VTree(nodeType: String, modifiers: Seq[VDomModifier_]) extends StaticVNode {

  def apply(args: VDomModifier*): VNode = args.sequence.map(args => copy(modifiers = modifiers ++ args))

  override def asProxy(implicit s: Scheduler): VNodeProxy = {
    val separatedModifiers = SeparatedModifiers.separate(modifiers)
    separatedModifiers.toSnabbdom(nodeType)
  }
}






