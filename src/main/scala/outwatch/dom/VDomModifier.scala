package outwatch.dom

import monix.execution.{Ack, Scheduler}
import monix.reactive.{Observable, Observer}
import org.scalajs.dom._
import outwatch.dom.helpers.SeparatedModifiers
import snabbdom.{DataObject, VNodeProxy}

import scala.concurrent.Future

/*
Modifier
  Property
    Attribute
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
    Hook
      InsertHook
      PrePatchHook
      UpdateHook
      PostPatchHook
      DestroyHook
    Key
  ChildVNode
    ModifierStreamReceiver
    StaticVNode
      StringVNode
      VTree
  Emitter
  CompositeModifier
  EmptyModifier
 */


sealed trait Modifier extends Any

// Modifiers

sealed trait Property extends Modifier

final case class Emitter(eventType: String, trigger: Event => Future[Ack]) extends Modifier

private[outwatch] final case class CompositeModifier(modifiers: Seq[Modifier]) extends Modifier

case object EmptyModifier extends Modifier

sealed trait ChildVNode extends Any with Modifier

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


sealed trait Hook[T] extends Property {
  def observer: Observer[T]
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

private[outwatch] final case class InsertHook(observer: Observer[Element]) extends Hook[Element]
private[outwatch] final case class PrePatchHook(observer: Observer[(Option[Element], Option[Element])])
  extends Hook[(Option[Element], Option[Element])]
private[outwatch] final case class UpdateHook(observer: Observer[(Element, Element)]) extends Hook[(Element, Element)]
private[outwatch] final case class PostPatchHook(observer: Observer[(Element, Element)]) extends Hook[(Element, Element)]
private[outwatch] final case class DestroyHook(observer: Observer[Element]) extends Hook[Element]

// Child Nodes

private[outwatch] sealed trait StaticVNode extends Any with ChildVNode {
  def toSnabbdom(implicit s: Scheduler): VNodeProxy
}
object StaticVNode {
  val empty: StaticVNode = StringVNode("")
}


final case class ModifierStreamReceiver(stream: Observable[VDomModifier], defaultValue: Modifier = EmptyModifier) extends ChildVNode

// Static Nodes
private[outwatch] final case class StringVNode(string: String) extends AnyVal with StaticVNode {
  override def toSnabbdom(implicit s: Scheduler): VNodeProxy = VNodeProxy.fromString(string)
}

// TODO: instead of Seq[VDomModifier] use Vector or JSArray?
// Fast concatenation and lastOption operations are important
// Needs to be benchmarked in the Browser
private[outwatch] final case class VTree(nodeType: String, modifiers: Seq[Modifier]) extends StaticVNode {

  def apply(args: VDomModifier*): VNode = args.sequence.map(args => copy(modifiers = modifiers ++ args))

  private var proxy: VNodeProxy = null
  override def toSnabbdom(implicit s: Scheduler): VNodeProxy = {
    if (proxy == null) {
      proxy = SeparatedModifiers.from(modifiers).toSnabbdom(nodeType)
    }
    proxy
  }
}
