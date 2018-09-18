package outwatch.dom

import cats.effect.IO
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observer
import org.scalajs.dom._
import outwatch.dom.helpers.{SeparatedModifiers, SnabbdomModifiers}
import snabbdom.{DataObject, VNodeProxy}

import scala.scalajs.js
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


sealed trait Modifier

// Modifiers

sealed trait Property extends Modifier

final case class Emitter(eventType: String, trigger: Event => Unit) extends Modifier

private[outwatch] final case class CompositeModifier(modifiers: js.Array[_ <: Modifier]) extends Modifier

case object EmptyModifier extends Modifier

sealed trait ChildVNode extends Modifier

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
  def trigger: T => Unit
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

private[outwatch] final case class DomMountHook(trigger: Element => Unit) extends Hook[Element]
private[outwatch] final case class DomUnmountHook(trigger: Element => Unit) extends Hook[Element]
private[outwatch] final case class DomUpdateHook(trigger: Element => Unit) extends Hook[Element]

private[outwatch] final case class InsertHook(trigger: Element => Unit) extends Hook[Element]
private[outwatch] final case class PrePatchHook(trigger: ((Option[Element], Option[Element])) => Unit) extends Hook[(Option[Element], Option[Element])]
private[outwatch] final case class UpdateHook(trigger: ((Element, Element)) => Unit) extends Hook[(Element, Element)]
private[outwatch] final case class PostPatchHook(trigger: ((Element, Element)) => Unit) extends Hook[(Element, Element)]
private[outwatch] final case class DestroyHook(trigger: Element => Unit) extends Hook[Element]

// Child Nodes

private[outwatch] sealed trait StaticVNode extends ChildVNode {
  def toSnabbdom(implicit s: Scheduler): VNodeProxy
}
object StaticVNode {
  val empty: StaticVNode = StringVNode("")
}


final case class ModifierStreamReceiver(stream: ValueObservable[VDomModifier]) extends ChildVNode

// Static Nodes
private[outwatch] final case class StringVNode(string: String) extends StaticVNode {
  override def toSnabbdom(implicit s: Scheduler): VNodeProxy = VNodeProxy.fromString(string)
}

private[outwatch] final case class VTree(nodeType: String, modifiers: js.Array[Modifier]) extends StaticVNode {

  def apply(args: VDomModifier*): VNode = IO {
    copy(modifiers = modifiers ++ args.map(_.unsafeRunSync))
  }

  private var _proxy: VNodeProxy = null
  private[outwatch] def proxy: Option[VNodeProxy] = Option(_proxy)
  override def toSnabbdom(implicit s: Scheduler): VNodeProxy = {
    if (_proxy == null) {
      _proxy = SnabbdomModifiers.toSnabbdom(SeparatedModifiers.from(modifiers), nodeType)
    }
    _proxy
  }
}
