package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom._
import outwatch.Sink
import monix.reactive.Observer
import outwatch.dom.{DestroyHook, Emitter, Hook, InsertHook, Observable, PostPatchHook, PrePatchHook, UpdateHook}


trait EmitterBuilder[E <: Event, O] extends Any {

  private[outwatch] def transform[T](tr: Observable[O] => Observable[T]): TransformingEmitterBuilder[E, T]

  def apply[T](value: T): TransformingEmitterBuilder[E, T] = map(_ => value)

  def apply[T](latest: Observable[T]): TransformingEmitterBuilder[E, T] = transform(_.withLatestFrom(latest)((_, u) => u))

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): TransformingEmitterBuilder[E, T] = map(f)

  def map[T](f: O => T): TransformingEmitterBuilder[E, T] = transform(_.map(f))

  def filter(predicate: O => Boolean): TransformingEmitterBuilder[E, O] = transform(_.filter(predicate))

  def collect[T](f: PartialFunction[O, T]): TransformingEmitterBuilder[E, T] = transform(_.collect(f))

  def -->(sink: Sink[_ >: O]): IO[Emitter]
}

object EmitterBuilder extends TargetOps {
  def apply[E <: Event](eventType: String) = new SimpleEmitterBuilder[E](eventType)
}

final case class TransformingEmitterBuilder[E <: Event, O] private[helpers] (
  eventType: String,
  transformer: Observable[E] => Observable[O]
) extends EmitterBuilder[E, O] {

  private[outwatch] def transform[T](tr: Observable[O] => Observable[T]): TransformingEmitterBuilder[E, T] = copy(
    transformer = tr compose transformer
  )

  def -->(sink: Sink[_ >: O]): IO[Emitter] = {
    val observer = sink.redirect(transformer).observer
    IO.pure(Emitter(eventType, event => observer.onNext(event.asInstanceOf[E])))
  }
}

final class SimpleEmitterBuilder[E <: Event] private[helpers](
  val eventType: String
) extends AnyVal with
          EmitterBuilder[E, E] {

  private[outwatch] def transform[O](transformer: Observable[E] => Observable[O]) = new TransformingEmitterBuilder[E, O](eventType, transformer)

  def -->(sink: Sink[_ >: E]): IO[Emitter] = {
    IO.pure(Emitter(eventType, event => sink.observer.onNext(event.asInstanceOf[E])))
  }
}

trait HookBuilder[E, H <: Hook[_]] {
  def hook(sink: Sink[E]): H

  def -->(sink: Sink[E]): IO[H] = IO.pure(hook(sink))
}

object InsertHookBuilder extends HookBuilder[html.Element, InsertHook] {
  def hook(sink: Sink[html.Element]) = InsertHook(sink.observer.asInstanceOf[Observer[Element]])
}

object PrePatchHookBuilder extends HookBuilder[(Option[html.Element], Option[html.Element]), PrePatchHook] {
  def hook(sink: Sink[(Option[html.Element], Option[html.Element])]) = PrePatchHook(sink.observer.asInstanceOf[Observer[(Option[Element],Option[Element])]])
}

object UpdateHookBuilder extends HookBuilder[(html.Element, html.Element), UpdateHook] {
  def hook(sink: Sink[(html.Element, html.Element)]) = UpdateHook(sink.observer.asInstanceOf[Observer[(Element,Element)]])
}

object PostPatchHookBuilder extends HookBuilder[(html.Element, html.Element), PostPatchHook] {
  def hook(sink: Sink[(html.Element, html.Element)]) = PostPatchHook(sink.observer.asInstanceOf[Observer[(Element,Element)]])
}

object DestroyHookBuilder extends HookBuilder[html.Element, DestroyHook] {
  def hook(sink: Sink[html.Element]) = DestroyHook(sink.observer.asInstanceOf[Observer[Element]])
}
