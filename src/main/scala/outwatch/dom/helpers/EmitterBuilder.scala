package outwatch.dom.helpers

import cats.effect.IO
import monix.execution.Scheduler
import monix.reactive.{Observable, Observer}
import org.scalajs.dom.Event
import outwatch.dom.Emitter
import outwatch.RichObserver


trait EmitterBuilder[E, O, R] extends Any {

  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[E, T, R]

  def -->(observer: Observer[O])(implicit scheduler:Scheduler): IO[R]

  def apply[T](value: T): EmitterBuilder[E, T, R] = map(_ => value)

  def apply[T](latest: Observable[T]): EmitterBuilder[E, T, R] = transform(_.withLatestFrom(latest)((_, u) => u))

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): EmitterBuilder[E, T, R] = map(f)

  def map[T](f: O => T): EmitterBuilder[E, T, R] = transform(_.map(f))

  def filter(predicate: O => Boolean): EmitterBuilder[E, O, R] = transform(_.filter(predicate))

  def collect[T](f: PartialFunction[O, T]): EmitterBuilder[E, T, R] = transform(_.collect(f))
}

object EmitterBuilder extends EmitterOps {
  def apply[E <: Event](eventType: String): EmitterBuilder[E, E, Emitter] =
    SimpleEmitterBuilder[E, Emitter](observer => Emitter(eventType, event => observer.onNext(event.asInstanceOf[E])))
}

final case class TransformingEmitterBuilder[E, O, R] private[helpers](
  transformer: Observable[E] => Observable[O],
  create: Observer[E] => IO[R]
) extends EmitterBuilder[E, O, R] {

  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[E, T, R] = copy(
    transformer = tr compose transformer
  )

  def -->(observer: Observer[O])(implicit scheduler:Scheduler): IO[R] = {
    val redirected: Observer[E] = observer.redirect[E](transformer)
    create(redirected)
  }
}

final case class CustomEmitterBuilder[E, R](create: Observer[E] => IO[R]) extends AnyVal with EmitterBuilder[E, E, R] {

  def transform[T](tr: Observable[E] => Observable[T]): EmitterBuilder[E, T, R] =
    new TransformingEmitterBuilder[E, T, R](tr, create)

  def -->(observer: Observer[E])(implicit scheduler:Scheduler): IO[R] = create(observer)
}
object SimpleEmitterBuilder {
  def apply[E, R](create: Observer[E] => R) = CustomEmitterBuilder[E, R](observer => IO.pure(create(observer)))
}
