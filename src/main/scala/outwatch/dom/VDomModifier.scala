package outwatch.dom

import org.scalajs.dom._
import scala.scalajs.js.|
import rxscalajs.{Observable, Observer}
import snabbdom.{VNodeProxy, h}

import scala.scalajs.js
import collection.breakOut

sealed trait VDomModifier extends Any

sealed trait Emitter extends VDomModifier {
  val eventType: String
}

sealed trait Property extends VDomModifier

sealed trait Receiver extends VDomModifier

sealed trait VNode extends Any with VDomModifier {
  def asProxy: VNodeProxy
}

final case class EventEmitter[E <: Event](eventType: String, sink: Observer[E]) extends Emitter
final case class StringEventEmitter(eventType: String, sink: Observer[String]) extends Emitter
final case class BoolEventEmitter(eventType: String, sink: Observer[Boolean]) extends Emitter
final case class NumberEventEmitter(eventType: String, sink: Observer[Double]) extends Emitter

sealed trait Attribute extends Property{
  val title: String
}

object Attribute {
  def apply(title: String, value: String | Boolean) = Attr(title, value)
}

final case class Attr(title: String, value: String | Boolean) extends Attribute
final case class Prop(title: String, value: String) extends Attribute
final case class Style(title: String, value: String) extends Attribute
final case class InsertHook(sink: Observer[Element]) extends Property
final case class DestroyHook(sink: Observer[Element]) extends Property
final case class UpdateHook(sink: Observer[(Element, Element)]) extends Property
final case class Key(value: String) extends Property

final case class AttributeStreamReceiver(attribute: String, attributeStream: Observable[Attribute]) extends Receiver
final case class ChildStreamReceiver(childStream: Observable[VNode]) extends Receiver
final case class ChildrenStreamReceiver(childrenStream: Observable[Seq[VNode]]) extends Receiver

final case object EmptyVDomModifier extends VDomModifier


object VDomModifier {
  final implicit class StringNode(val string: String) extends AnyVal with VNode {
    def asProxy = VNodeProxy.fromString(string)
  }

  implicit def OptionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse EmptyVDomModifier

  final case class VTree(nodeType: String, modifiers: Vector[VDomModifier]) extends VNode {
    import helpers.DomUtils

    def asProxy = {
      val (children, attributeObject) = DomUtils.extractChildrenAndDataObject(modifiers)
      val childProxies: js.Array[VNodeProxy] = children.map(_.asProxy)(breakOut)
      h(nodeType, attributeObject, childProxies)
    }

    def apply(args: VDomModifier*):VNode = VTree(nodeType, modifiers ++ args)
  }
}
