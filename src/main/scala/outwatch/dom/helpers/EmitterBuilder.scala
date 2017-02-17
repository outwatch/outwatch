package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.{BoolEventEmitter, MouseEventEmitter, NumberEventEmitter, StringEventEmitter, _}
import rxscalajs.{Observable, Observer, Subject}

final case class GenericEmitterBuilder[T](eventType: String, t: T) {
  def -->[U >: T](sink: Sink[U]) = ConstantEmitter(eventType, sink.observer, t)
}

final case class GenericMappedEmitterBuilder[T,E](constructor: Subject[E] => Emitter, mapping: E => T){
  def -->[U >: T](sink: Sink[U]) = {
    val proxy = Subject[E]
    sink <-- proxy.map(mapping)
    constructor(proxy)
  }
}

final case class GenericStreamEmitterBuilder[T](eventType: String, stream: Observable[T]) {
  def -->[U >: T](sink: Sink[U]) = {
    val proxy = Subject[Event]()
    sink <-- proxy
      .withLatestFrom(stream)
      .map(_._2)
    EventEmitter(eventType, proxy)
  }
}
final class InputEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[InputEvent]) =
    InputEventEmitter(eventType, sink.observer)

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](f: InputEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Subject[InputEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class KeyEventEmitterBuilder(eventType: String){
  def -->(sink: Sink[KeyboardEvent]) =
    KeyEventEmitter(eventType, sink.observer)

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](f: KeyboardEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Subject[KeyboardEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class MouseEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[MouseEvent]) =
    MouseEventEmitter(eventType, sink.observer)

  def apply[T](t: T) = GenericEmitterBuilder(eventType, t)

  def apply[T](f: MouseEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Subject[MouseEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class StringEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[String]) =
    StringEventEmitter(eventType, sink.observer)

  def apply[T](f: String => T) =
    GenericMappedEmitterBuilder(StringEventEmitter(eventType, _: Subject[String]), f)
}

final class BoolEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[Boolean]) =
    BoolEventEmitter(eventType, sink.observer)

  def apply[T](f: Boolean => T) =
    GenericMappedEmitterBuilder(BoolEventEmitter(eventType, _: Subject[Boolean]), f)
}

final class NumberEventEmitterBuilder(eventType: String) {
  def -->(sink: Sink[Double]) =
    NumberEventEmitter(eventType, sink.observer)

  def apply[T](f: Double => T) =
    GenericMappedEmitterBuilder(NumberEventEmitter(eventType, _: Subject[Double]), f)
}

final class InsertHookBuilder() {
  def -->(sink: Sink[Element]) = InsertHook(sink.observer)
}

final class DestroyHookBuilder() {
  def -->(sink: Sink[Element]) = DestroyHook(sink.observer)
}

final class UpdateHookBuilder() {
  def -->(sink: Sink[(Element, Element)]) = UpdateHook(sink.observer)
}
