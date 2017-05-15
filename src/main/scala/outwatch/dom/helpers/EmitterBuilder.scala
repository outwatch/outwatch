package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.{BoolEventEmitter, MouseEventEmitter, NumberEventEmitter, StringEventEmitter, _}
import rxscalajs.{Observable, Observer}

final case class GenericMappedEmitterBuilder[T,E](constructor: Observer[E] => Emitter, mapping: E => T){
  def -->[U >: T](sink: Sink[U]): Emitter = {
    constructor(sink.redirectMap(mapping).observer)
  }
}

final case class FilteredGenericMappedEmitterBuilder[T,E](
  constructor: Observer[E] => Emitter,
  mapping: E => T,
  predicate: E => Boolean
) {
  def -->[U >: T](sink: Sink[U]): Emitter = {
    constructor(sink.redirect[E](_.filter(predicate).map(mapping)).observer)
  }
}

final case class WithLatestFromEmitterBuilder[T](eventType: String, stream: Observable[T]) {
  def -->[U >: T](sink: Sink[U]): EventEmitter = {
    val proxy: Sink[Event] = sink.redirect(_.withLatestFromWith(stream)((_,u) => u))
    EventEmitter(eventType, proxy.observer)
  }
}

final case class FilteredWithLatestFromEmitterBuilder[T, E <: Event](
  eventType: String,
  stream: Observable[T],
  predicate: E => Boolean
) {
  def -->[U >: T](sink: Sink[U]): EventEmitter = {
    val proxy: Sink[E] = sink.redirect(_.filter(predicate).withLatestFromWith(stream)((_, u) => u))
    EventEmitter(eventType, proxy.observer)
  }
}

final case class FilteredEmitterBuilder[E](eventType: String, predicate: E => Boolean) {
  def -->(sink: Sink[E]) =
    EventEmitter(eventType, sink.redirect[E](_.filter(predicate)).observer)

  def apply[T](t: T) =
    FilteredGenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[E]), (_: E) => t, predicate)

  def apply[T](f: E => T) =
    FilteredGenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[E]), f, predicate)

  def apply[T](ts: Observable[T]) = FilteredWithLatestFromEmitterBuilder(eventType, ts, predicate)
}

final class EventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[Event]) =
    EventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[Event]), (_: Event) => t)

  def apply[T](f: Event => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[Event]), f)

  def apply[T](ts: Observable[T]) = WithLatestFromEmitterBuilder(eventType, ts)

  def filter(predicate: Event => Boolean) = FilteredEmitterBuilder(eventType, predicate)
}

final class InputEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[InputEvent]) =
    InputEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[InputEvent]), (_: Event) => t)

  def apply[T](f: InputEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[InputEvent]), f)

  def apply[T](ts: Observable[T]) = WithLatestFromEmitterBuilder(eventType, ts)

  def filter(predicate: InputEvent => Boolean) = FilteredEmitterBuilder(eventType, predicate)
}

final class KeyEventEmitterBuilder(val eventType: String) extends AnyVal{
  def -->(sink: Sink[KeyboardEvent]) =
    KeyEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[KeyboardEvent]), (_: Event) => t)
  def apply[T](f: KeyboardEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[KeyboardEvent]), f)

  def apply[T](ts: Observable[T]) = WithLatestFromEmitterBuilder(eventType, ts)

  def filter(predicate: KeyboardEvent => Boolean) = FilteredEmitterBuilder(eventType, predicate)
}

final class MouseEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[MouseEvent]) =
    MouseEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[MouseEvent]), (_: Event) => t)

  def apply[T](f: MouseEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[MouseEvent]), f)

  def apply[T](ts: Observable[T]) = WithLatestFromEmitterBuilder(eventType, ts)

  def filter(predicate: MouseEvent => Boolean) = FilteredEmitterBuilder(eventType, predicate)
}

final class WheelEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[WheelEvent]) =
    WheelEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[WheelEvent]), (_: Event) => t)

  def apply[T](f: WheelEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[WheelEvent]), f)

  def apply[T](ts: Observable[T]) = WithLatestFromEmitterBuilder(eventType, ts)

  def filter(predicate: WheelEvent => Boolean) = FilteredEmitterBuilder(eventType, predicate)
}

final class TouchEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[TouchEvent]) =
    TouchEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[TouchEvent]), (_: Event) => t)

  def apply[T](f: TouchEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[TouchEvent]), f)

  def apply[T](ts: Observable[T]) = WithLatestFromEmitterBuilder(eventType, ts)

  def filter(predicate: TouchEvent => Boolean) = FilteredEmitterBuilder(eventType, predicate)
}

final class ClipboardEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[ClipboardEvent]) =
    ClipboardEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[ClipboardEvent]), (_: Event) => t)

  def apply[T](f: ClipboardEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[ClipboardEvent]), f)

  def apply[T](ts: Observable[T]) = WithLatestFromEmitterBuilder(eventType, ts)

  def filter(predicate: ClipboardEvent => Boolean) = FilteredEmitterBuilder(eventType, predicate)
}

final class DragEventEmitterBuilder(val eventType: String) extends AnyVal {
  def -->(sink: Sink[DragEvent]) =
    DragEventEmitter(eventType, sink.observer)

  def apply[T](t: T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[DragEvent]), (_: Event) => t)

  def apply[T](f: DragEvent => T) =
    GenericMappedEmitterBuilder(EventEmitter(eventType, _:Observer[DragEvent]), f)

  def apply[T](ts: Observable[T]) = WithLatestFromEmitterBuilder(eventType, ts)

  def filter(predicate: DragEvent => Boolean) = FilteredEmitterBuilder(eventType, predicate)
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
