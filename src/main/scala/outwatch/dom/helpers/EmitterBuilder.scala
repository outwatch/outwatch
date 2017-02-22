package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.{BoolEventEmitter, MouseEventEmitter, NumberEventEmitter, StringEventEmitter, _}
import rxscalajs.{Observable, Observer, Subject}

final case class GenericMappedEmitterBuilder[T,E](constructor: Observer[E] => Emitter, mapping: E => T){
  def -->[U >: T](sink: Sink[U]) = {
    constructor(sink.redirectMap(mapping).observer)
  }
}

final case class GenericStreamEmitterBuilder[T](eventType: String, stream: Observable[T]) {
  def -->[U >: T](sink: Sink[U]) = {
    val proxy: Sink[Event] = sink.redirect(_.withLatestFromWith(stream)((_,u) => u))
    EventEmitter(eventType, proxy.observer)
  }
}
final class EventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[Event]) =
    EventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[Event]), (_: Event) => t)

  def apply[T](f: Event => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[Event]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class InputEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[InputEvent]) =
    InputEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[InputEvent]), (_: Event) => t)

  def apply[T](f: InputEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[InputEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class KeyEventEmitterBuilder(val eventType: String) extends AnyVal{
  def -->(sink: Sink[KeyboardEvent]) =
    KeyEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[InputEvent]), (_: Event) => t)
  def apply[T](f: KeyboardEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[KeyboardEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class MouseEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[MouseEvent]) =
    MouseEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[InputEvent]), (_: Event) => t)

  def apply[T](f: MouseEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[MouseEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class ClipboardEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[ClipboardEvent]) =
    ClipboardEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[InputEvent]), (_: Event) => t)

  def apply[T](f: ClipboardEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[ClipboardEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class DragEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[DragEvent]) =
    DragEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[InputEvent]), (_: Event) => t)

  def apply[T](f: DragEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[DragEvent]), f)

  def apply[T](ts: Observable[T]) = GenericStreamEmitterBuilder(eventType, ts)
}

final class StringEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[String]) =
    StringEventEmitter(eventType, sink.observer)

  def apply[T](f: String => T) =
    GenericMappedEmitterBuilder(StringEventEmitter(eventType, _: Observer[String]), f)
}

final class BoolEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[Boolean]) =
    BoolEventEmitter(eventType, sink.observer)

  def apply[T](f: Boolean => T) =
    GenericMappedEmitterBuilder(BoolEventEmitter(eventType, _: Observer[Boolean]), f)
}

final class NumberEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[Double]) =
    NumberEventEmitter(eventType, sink.observer)

  def apply[T](f: Double => T) =
    GenericMappedEmitterBuilder(NumberEventEmitter(eventType, _: Observer[Double]), f)
}

object InsertHookBuilder {
  def -->(sink: Sink[Element]) = InsertHook(sink.observer)
}

object DestroyHookBuilder {
  def -->(sink: Sink[Element]) = DestroyHook(sink.observer)
}

object UpdateHookBuilder {
  def -->(sink: Sink[(Element, Element)]) = UpdateHook(sink.observer)
}
