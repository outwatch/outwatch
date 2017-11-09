package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.{DestroyHook, Emitter, Hook, InsertHook, UpdateHook}
import rxscalajs.Observable

final case class TransformingEmitterBuilder[E <: Event, O] private[helpers] (
  eventType: String,
  transform: Observable[E] => Observable[O]
) {

  def apply[T](value: T): TransformingEmitterBuilder[E, T] = map(_ => value)

  def apply[T](latest: Observable[T]): TransformingEmitterBuilder[E, T] = copy(
    transform = obs => transform(obs).withLatestFromWith(latest)((_, u) => u)
  )

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): TransformingEmitterBuilder[E, T] = map(f)

  def map[T](f: O => T): TransformingEmitterBuilder[E, T] = copy(transform = obs => transform(obs).map(f))

  def filter(predicate: O => Boolean): TransformingEmitterBuilder[E, O] = copy(transform = obs => transform(obs).filter(predicate))

  def -->(sink: Sink[_ >: O]): IO[Emitter] = {
    val observer = sink.redirect(transform).observer
    IO.pure(Emitter(eventType, event => observer.next(event.asInstanceOf[E])))
  }
}


final class EmitterBuilder[E <: Event] private(val eventType: String) extends AnyVal {

  @inline private def create[O](transform: Observable[E] => Observable[O]) = new TransformingEmitterBuilder[E, O](
    eventType, transform
  )

  def apply[T](value: T): TransformingEmitterBuilder[E, T] = map(_ => value)

  def apply[T](latest: Observable[T]): TransformingEmitterBuilder[E, T] = create(_.withLatestFromWith(latest)((_, u) => u))

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: E => T): TransformingEmitterBuilder[E, T] = map(f)

  def map[T](f: E => T): TransformingEmitterBuilder[E, T] = create(_.map(f))

  def filter(predicate: E => Boolean): TransformingEmitterBuilder[E, E] = create(_.filter(predicate))

  def -->(sink: Sink[_ >: E]): IO[Emitter] = {
    IO.pure(Emitter(eventType, event => sink.observer.next(event.asInstanceOf[E])))
  }
}

object EmitterBuilder {
  def apply[E <: Event](eventType: String) = new EmitterBuilder[E](eventType)
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
