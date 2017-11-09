package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement
import outwatch.Sink
import outwatch.dom.{DestroyHook, Emitter, Hook, InsertHook, UpdateHook}
import rxscalajs.{Observable, Observer}

case class EmitterBuilder[E, O](
  private val eventType: String,
  private val transform: Observable[E] => Observable[O],
  private val trigger: (Event, Observer[E]) => Unit
) {
  def apply[T](mapping: O => T): EmitterBuilder[E, T] = copy(transform = obs => transform(obs).map(mapping))
  def apply[T](value: T): EmitterBuilder[E, T] = apply(_ => value)
  def apply[T](latest: Observable[T]): EmitterBuilder[E, T] = copy(transform = obs => transform(obs).withLatestFromWith(latest)((_, u) => u))
  def filter(predicate: O => Boolean): EmitterBuilder[E, O] = copy(transform = obs => transform(obs).filter(predicate))

  def -->(sink: Sink[_ >: O]): IO[Emitter[E]] = {
    val observer = sink.redirect(transform).observer
    IO.pure(Emitter(eventType, observer, event => trigger(event, observer)))
  }
}

object EmitterBuilder {
  def apply[E](eventType:String, trigger:(Event,Observer[_ >: E]) => Unit) = {
    new EmitterBuilder[E,E](eventType, identity, trigger)
  }

  def event[E <: Event](eventType:String) = apply[E](
    eventType,
   (e: Event, o:Observer[_ >: E]) => o.asInstanceOf[Observer[Event]].next(e)
  )
  def inputValueAsString(eventType:String) = apply(
    eventType,
    (e: Event, o:Observer[_ >: String]) => o.next(e.target.asInstanceOf[HTMLInputElement].value)
  )
  def inputCheckedAsBool(eventType:String) = apply(
    eventType,
    (e: Event, o:Observer[_ >: Boolean]) => o.next(e.target.asInstanceOf[HTMLInputElement].checked)
  )
  def inputValueAsNumber(eventType:String) = apply(
    eventType,
    (e: Event, o:Observer[_ >: Double]) => o.next(e.target.asInstanceOf[HTMLInputElement].valueAsNumber)
  )
}

trait HookBuilder[E, H <: Hook] {
  def hook(sink: Sink[E]): H
  def -->(sink: Sink[E]): IO[H] = IO.pure(hook(sink))
}

object InsertHookBuilder extends HookBuilder[Element, InsertHook] {
  def hook(sink: Sink[Element]) = InsertHook(sink.observer)
}

object DestroyHookBuilder extends HookBuilder[Element, DestroyHook] {
  def hook(sink: Sink[Element]) = DestroyHook(sink.observer)
}

object UpdateHookBuilder extends HookBuilder[(Element, Element), UpdateHook] {
  def hook(sink: Sink[(Element, Element)]) = UpdateHook(sink.observer)
}
