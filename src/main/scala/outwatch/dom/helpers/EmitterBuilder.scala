package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.{BoolEventEmitter, MouseEventEmitter, NumberEventEmitter, StringEventEmitter, _}
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

class InsertHookBuilder() {
  def -->(sink: Sink[Unit]) = InsertHook(sink.asInstanceOf[Subject[Unit]])
}

class DestroyHookBuilder() {
  def -->(sink: Sink[Unit]) = DestroyHook(sink.asInstanceOf[Subject[Unit]])
}

class UpdateHookBuilder() {
  def -->(sink: Sink[Unit]) = UpdateHook(sink.asInstanceOf[Subject[Unit]])
}
