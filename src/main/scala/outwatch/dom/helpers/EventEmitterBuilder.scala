package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.dom._
import rxscalajs.{Observable, Subject}

case class GenericEmitterBuilder[T](eventType: String, t: T) {
  def -->(sink: Subject[T]) = GenericEmitter(eventType, sink, t)
}

case class GenericStreamEmitterBuilder[T](eventType: String, stream: Observable[T]) {
  def --> (sink: Subject[T]) = {
    val proxy = Subject[Event]()
    proxy
      .withLatestFrom(stream)
      .map(_._2)
      .subscribe(t => sink.next(t))
    EventEmitter(eventType, proxy)
  }
}
case class InputEventEmitterBuilder(eventType: String){
  def --> (subject: Subject[InputEvent]) = InputEventEmitter(eventType, subject)

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class KeyEventEmitterBuilder(eventType: String){
  def -->(sink: Subject[KeyboardEvent]) = KeyEventEmitter(eventType, sink)

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class MouseEventEmitterBuilder(eventType: String) {
  def -->(sink: Subject[MouseEvent]) = MouseEventEmitter(eventType, sink)

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class StringEventEmitterBuilder(eventType: String) {
  def -->(sink: Subject[String]) = StringEventEmitter(eventType, sink)
}

case class BoolEventEmitterBuilder(eventType: String) {
  def -->(sink: Subject[Boolean]) = BoolEventEmitter(eventType, sink)
}

case class NumberEventEmitterBuilder(eventType: String) {
  def -->(sink: Subject[Double]) = NumberEventEmitter(eventType, sink)
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

case class AttributeBuilder(attributeName: String) {
  def :=(value: String) = Attribute(attributeName,value)

  def <--[T <: Any](valueStream: Observable[T]) =  {
    AttributeStreamReceiver(attributeName, valueStream.map (value => value match {
      case b: Boolean => Attribute(attributeName, if (b) b.toString else "")
      case _ => Attribute(attributeName, value.toString)
    }))
  }
}
