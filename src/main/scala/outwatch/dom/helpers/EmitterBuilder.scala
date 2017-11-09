package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.{DestroyHook, Emitter, Hook, InsertHook, UpdateHook}
import rxscalajs.Observable

final case class EmitterBuilder[E <: Event, O] private (
  eventType: String,
  transform: Observable[E] => Observable[O]
) {

  def apply[T](value: T): EmitterBuilder[E, T] = map(_ => value)

  def apply[T](latest: Observable[T]): EmitterBuilder[E, T] = copy(
    transform = obs => transform(obs).withLatestFromWith(latest)((_, u) => u)
  )

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): EmitterBuilder[E, T] = map(f)

  def map[T](f: O => T): EmitterBuilder[E, T] = copy(transform = obs => transform(obs).map(f))

  def filter(predicate: O => Boolean): EmitterBuilder[E, O] = copy(transform = obs => transform(obs).filter(predicate))

  def -->(sink: Sink[_ >: O]): IO[Emitter] = {
    val observer = sink.redirect(transform).observer
    IO.pure(Emitter(eventType, event => observer.next(event.asInstanceOf[E])))
  }
}

object EmitterBuilder {
  def apply[E <: Event](eventType: String) = new EmitterBuilder[E, E]( eventType, identity )
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
