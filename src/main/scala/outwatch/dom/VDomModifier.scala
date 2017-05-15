package outwatch.dom

import org.scalajs.dom._
import rxscalajs.{Observable, Observer}
import snabbdom.{DataObject, VNodeProxy, h}

import scala.scalajs.js.JSConverters._

sealed trait VDomModifier extends Any

sealed trait Emitter extends VDomModifier {
  val eventType: String
}

sealed trait Property extends VDomModifier

sealed trait Receiver extends VDomModifier

sealed trait VNode extends VDomModifier {
  val asProxy: VNodeProxy
}

final case class EventEmitter[E <: Event](eventType: String, sink: Observer[E]) extends Emitter
final case class StringEventEmitter(eventType: String, sink: Observer[String]) extends Emitter
final case class BoolEventEmitter(eventType: String, sink: Observer[Boolean]) extends Emitter
final case class NumberEventEmitter(eventType: String, sink: Observer[Double]) extends Emitter

final case class Attribute(title: String, value: String) extends Property
final case class InsertHook(sink: Observer[Element]) extends Property
final case class DestroyHook(sink: Observer[Element]) extends Property
final case class UpdateHook(sink: Observer[(Element, Element)]) extends Property
final case class Key(value: String) extends Property

final case class AttributeStreamReceiver(attribute: String, attributeStream: Observable[Attribute]) extends Receiver
final case class ChildStreamReceiver(childStream: Observable[VNode]) extends Receiver
final case class ChildrenStreamReceiver(childrenStream: Observable[Seq[VNode]]) extends Receiver




object VDomModifier {
  final implicit class StringNode(string: String) extends VNode {
    val asProxy = VNodeProxy.fromString(string)
  }

  final case class VTree(nodeType: String,
                   children: Seq[VNode],
                   attributeObject: DataObject,
                   changables: Observable[(Seq[Attribute], Seq[VNode])]
                  ) extends VNode {


    lazy val childProxies: Seq[VNodeProxy] = children.map(_.asProxy)

    val asProxy = h(nodeType, attributeObject, childProxies.toJSArray)

  }
}




