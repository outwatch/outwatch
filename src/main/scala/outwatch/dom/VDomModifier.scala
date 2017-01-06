package outwatch.dom

import org.scalajs.dom._
import rxscalajs.subscription.AnonymousSubscription
import rxscalajs.{Observable, Subject}
import snabbdom.{DataObject, VNodeProxy, h, patch}
import outwatch.dom.helpers.InputEvent

import scala.concurrent.Promise
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON

sealed trait VDomModifier extends Any

sealed trait Emitter extends VDomModifier {
  val eventType: String
}

sealed trait Property extends VDomModifier

sealed trait Receiver extends VDomModifier

sealed trait VNode extends VDomModifier {
  val asProxy: VNodeProxy
}


case class EventEmitter(eventType: String, sink: Subject[_ <: Event]) extends Emitter
case class InputEventEmitter(eventType: String, sink: Subject[InputEvent]) extends Emitter
case class MouseEventEmitter(eventType: String, sink: Subject[MouseEvent]) extends Emitter
case class KeyEventEmitter(eventType: String, sink: Subject[KeyboardEvent]) extends Emitter
case class GenericEmitter[T](eventType: String, sink: Subject[T], t: T) extends Emitter
case class StringEventEmitter(eventType: String, sink: Subject[String]) extends Emitter
case class BoolEventEmitter(eventType: String, sink: Subject[Boolean]) extends Emitter
case class NumberEventEmitter(eventType: String, sink: Subject[Double]) extends Emitter

case class Attribute(title: String, value: String) extends Property
case class InsertHook(sink: Subject[Unit]) extends Property
case class DestroyHook(sink: Subject[Unit]) extends Property
case class UpdateHook(sink: Subject[Unit]) extends Property

case class AttributeStreamReceiver(attribute: String, attributeStream: Observable[Attribute]) extends Receiver
case class ChildStreamReceiver(childStream: Observable[VNode]) extends Receiver
case class ChildrenStreamReceiver(childrenStream: Observable[Seq[VNode]]) extends Receiver




object VDomModifier {
  implicit class StringNode(string: String) extends VNode {
    val asProxy = string.asInstanceOf[VNodeProxy]
  }

  case class VTree(nodeType: String,
                   children: Seq[VNode],
                   attributeObject: DataObject,
                   changables: Observable[(Seq[Attribute], Seq[VNode])]
                  ) extends VNode {


    lazy val childProxies: Seq[VNodeProxy] = children.map(_.asProxy)

    val asProxy = h(nodeType, attributeObject, childProxies.toJSArray)

  }
}




