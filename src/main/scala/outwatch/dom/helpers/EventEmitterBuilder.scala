package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom._
import rxscalajs.{Observable, Subject}

case class GenericEmitterBuilder[T](eventType: String, t: T) {
  def -->[U >: T](sink: Sink[U]) = GenericEmitter(eventType, sink.asInstanceOf[Subject[U]], t)
}

case class GenericMappedEmitterBuilder[T,E](constructor: Subject[E] => Emitter, mapping: E => T){
  def -->[U >: T](sink: Sink[U]) = {
    val proxy = Subject[E]
    sink <-- proxy.map(mapping)
    constructor(proxy)
  }
}

case class GenericStreamEmitterBuilder[T](eventType: String, stream: Observable[T]) {
  def -->[U >: T](sink: Sink[U]) = {
    val proxy = Subject[Event]()
    proxy
      .withLatestFrom(stream)
      .map(_._2)
      .subscribe(t => sink.asInstanceOf[Subject[U]].next(t))
    EventEmitter(eventType, proxy)
  }
}
case class InputEventEmitterBuilder(eventType: String){
  def --> (sink: Sink[InputEvent]) =
    InputEventEmitter(eventType, sink.asInstanceOf[Subject[InputEvent]])

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](f: InputEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Subject[InputEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class KeyEventEmitterBuilder(eventType: String){
  def -->(sink: Sink[KeyboardEvent]) =
    KeyEventEmitter(eventType, sink.asInstanceOf[Subject[KeyboardEvent]])

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](f: KeyboardEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Subject[KeyboardEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class MouseEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[MouseEvent]) =
    MouseEventEmitter(eventType, sink.asInstanceOf[Subject[MouseEvent]])

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](f: MouseEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Subject[MouseEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

case class StringEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[String]) =
    StringEventEmitter(eventType, sink.asInstanceOf[Subject[String]])

  def apply[T](f: String => T) =
    GenericMappedEmitterBuilder(StringEventEmitter(eventType, _: Subject[String]), f)
}

case class BoolEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[Boolean]) =
    BoolEventEmitter(eventType, sink.asInstanceOf[Subject[Boolean]])

  def apply[T](f: Boolean => T) =
    GenericMappedEmitterBuilder(BoolEventEmitter(eventType, _: Subject[Boolean]), f)
}

case class NumberEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[Double]) =
    NumberEventEmitter(eventType, sink.asInstanceOf[Subject[Double]])

  def apply[T](f: Double => T) =
    GenericMappedEmitterBuilder(NumberEventEmitter(eventType, _: Subject[Double]), f)
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

  @deprecated
  def :=(children: Seq[VNode]) = {
    ChildrenStreamReceiver(Observable.of(children))
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
