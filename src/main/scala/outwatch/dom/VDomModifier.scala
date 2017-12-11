package outwatch.dom

import org.scalajs.dom._
import outwatch.dom.helpers.DomUtils
import rxscalajs.Observer
import snabbdom.{DataObject, VNodeProxy, hFunction}

import scala.scalajs.js
import collection.breakOut
import cats.implicits._


/*
VDomModifier_
  Property
    Attribute
      Attr
      AccumAttr
      Prop
      Style
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

final case class Emitter(eventType: String, trigger: Event => Unit) extends VDomModifier_

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
  def apply(title: String, value: Attr.Value) = Attr(title, value)
}


sealed trait Hook[T] extends Property {
  def observer: Observer[T]
}

// Attributes

case object EmptyAttribute extends Attribute

sealed trait TitledAttribute extends Attribute {
  val title: String
}

final case class Attr(title: String, value: Attr.Value) extends TitledAttribute
object Attr {
  type Value = DataObject.AttrValue
}

final case class Prop(title: String, value: Prop.Value) extends TitledAttribute
object Prop {
  type Value = DataObject.PropValue
}

final case class Style(title: String, value: String) extends TitledAttribute

// Hooks

private[outwatch] final case class InsertHook(observer: Observer[Element]) extends Hook[Element]
private[outwatch] final case class PrePatchHook(observer: Observer[(Option[Element], Option[Element])])
  extends Hook[(Option[Element], Option[Element])]
private[outwatch] final case class UpdateHook(observer: Observer[(Element, Element)]) extends Hook[(Element, Element)]
private[outwatch] final case class PostPatchHook(observer: Observer[(Element, Element)]) extends Hook[(Element, Element)]
private[outwatch] final case class DestroyHook(observer: Observer[Element]) extends Hook[Element]

// Child Nodes
sealed trait StaticVNode extends Any with ChildVNode {
  def asProxy: VNodeProxy
}

final case class ChildStreamReceiver(childStream: Observable[StaticVNode]) extends ChildVNode
final case class ChildrenStreamReceiver(childrenStream: Observable[Seq[StaticVNode]]) extends ChildVNode

// Static Nodes
private[outwatch] final case class StringVNode(string: String) extends AnyVal with StaticVNode {
  override def asProxy: VNodeProxy = VNodeProxy.fromString(string)
}

// TODO: instead of Seq[VDomModifier] use Vector or JSArray?
// Fast concatenation and lastOption operations are important
// Needs to be benchmarked in the Browser
private[outwatch] final case class VTree(nodeType: String,
                       modifiers: Seq[VDomModifier_]) extends StaticVNode {

  //TODO no .toList
  def apply(args: VDomModifier*) = args.toList.sequence.map(args => VTree(nodeType, modifiers ++ args))

  override def asProxy: VNodeProxy = {
    val (children, attributeObject) = DomUtils.extractChildrenAndDataObject(modifiers)

    import DomUtils.Children
    children match {
      case Children.VNodes(vnodes, _) =>
        val childProxies: js.Array[VNodeProxy] = vnodes.collect { case s: StaticVNode => s.asProxy }(breakOut)
        hFunction(nodeType, attributeObject, childProxies)
      case Children.StringModifiers(textChildren) =>
        hFunction(nodeType, attributeObject, textChildren.map(_.string).mkString)
      case Children.Empty =>
        hFunction(nodeType, attributeObject)
    }
  }
}






