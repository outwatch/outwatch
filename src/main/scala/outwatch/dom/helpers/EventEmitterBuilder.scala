package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.dom._
import rxscalajs.{Observable, Subject}

case class GenericEmitterBuilder[T](eventType: String, t: T) {
  def -->(sink: Sink[T]) = GenericEmitter(eventType, sink.asInstanceOf[Subject[T]], t)
}

case class GenericStreamEmitterBuilder[T](eventType: String, stream: Observable[T]) {
  def --> (sink: Sink[T]) = {
    val proxy = Subject[Event]()
    proxy
      .withLatestFrom(stream)
      .map(_._2)
      .subscribe(t => sink.asInstanceOf[Subject[T]].next(t))
    EventEmitter(eventType, proxy)
  }
}
case class InputEventEmitterBuilder(eventType: String){
  def --> (sink: Sink[InputEvent]) =
    InputEventEmitter(eventType, sink.asInstanceOf[Subject[InputEvent]])

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class KeyEventEmitterBuilder(eventType: String){
  def -->(sink: Sink[KeyboardEvent]) =
    KeyEventEmitter(eventType, sink.asInstanceOf[Subject[KeyboardEvent]])

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class MouseEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[MouseEvent]) =
    MouseEventEmitter(eventType, sink.asInstanceOf[Subject[MouseEvent]])

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class StringEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[String]) =
    StringEventEmitter(eventType, sink.asInstanceOf[Subject[String]])
}

case class BoolEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[Boolean]) =
    BoolEventEmitter(eventType, sink.asInstanceOf[Subject[Boolean]])
}

case class NumberEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[Double]) =
    NumberEventEmitter(eventType, sink.asInstanceOf[Subject[Double]])
}

case class ChildStreamReceiverBuilder() {
  def <--[T <: Any](valueStream: Observable[T]): ChildStreamReceiver = {
    ChildStreamReceiver(valueStream.map(anyToVNode))
  }

  private val anyToVNode = (any: Any) => any match {
    case vn: VNode => vn
    case _ => VDomModifier.StringNode(any.toString)
  }
}

case class ChildrenStreamReceiverBuilder() {
  def <--(childrenStream: Observable[Seq[VNode]]) = {
    ChildrenStreamReceiver(childrenStream)
  }
}


case class AttributeBuilder[T](attributeName: String){
  def :=(value: T) = Attribute(attributeName, value.toString)

  def <--(valueStream: Observable[T]) = {
    val attributeStream = valueStream.map(n => Attribute(attributeName, n.toString))
    AttributeStreamReceiver(attributeName, attributeStream)
  }


}

case class BoolAttributeBuilder(attributeName: String) {
  def :=(value: Boolean) = Attribute(attributeName, toEmptyIfFalse(value))

  def <--(valueStream: Observable[Boolean]) = {
    AttributeStreamReceiver(attributeName, valueStream.map(b => {
      Attribute(attributeName, toEmptyIfFalse(b))
    }))
  }

  private def toEmptyIfFalse(b: Boolean) = if (b) b.toString else ""
}

object BoolAttributeBuilder {
  implicit def toAttribute(builder: BoolAttributeBuilder): Attribute =
    Attribute(builder.attributeName, "_")
}
