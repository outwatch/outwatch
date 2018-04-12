package outwatch.dom.helpers

import cats.effect.Effect
     import monix.reactive.Observer
     import org.scalajs.dom.Event
     import outwatch.Sink
     import outwatch.dom.{Emitter, Observable}


trait EmitterBuilder[F[+_], E, O, R] {
  implicit val effectF: Effect[F]

  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[F, E, T, R]

  def -->(sink: Sink[F, _ >: O]): F[R]

  def apply[T](value: T): EmitterBuilder[F, E, T, R] = map(_ => value)

  def apply[T](latest: Observable[T]): EmitterBuilder[F, E, T, R] = transform(_.withLatestFrom(latest)((_, u) => u))

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): EmitterBuilder[F, E, T, R] = map(f)

  def map[T](f: O => T): EmitterBuilder[F, E, T, R] = transform(_.map(f))

  def filter(predicate: O => Boolean): EmitterBuilder[F, E, O, R] = transform(_.filter(predicate))

  def collect[T](f: PartialFunction[O, T]): EmitterBuilder[F, E, T, R] = transform(_.collect(f))
}

trait EmitterBuilderFactory[F[+_]] extends EmitterOps[F] {
  implicit val effectF: Effect[F]
  def emitterBuilderFactory[E <: Event](eventType: String): SimpleEmitterBuilder[F, E, Emitter] =
    SimpleEmitterBuilder[F, E, Emitter](observer => Emitter(eventType, event => observer.onNext(event.asInstanceOf[E])))
}

final case class TransformingEmitterBuilder[F[+_], E, O, R] private[helpers](
  transformer: Observable[E] => Observable[O],
  create: Observer[E] => R
)(implicit val effectF: Effect[F]) extends EmitterBuilder[F, E, O, R] {

  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[F, E, T, R] = copy(
    transformer = tr compose transformer
  )


  def -->(sink: Sink[F, _ >: O]): F[R] = {
    val redirected: Sink[F, E] = sink.unsafeRedirect[E](transformer)
    effectF.pure(create(redirected.observer))
  }
}

final case class SimpleEmitterBuilder[F[+_], E, R](create: Observer[E] => R)(implicit val effectF: Effect[F]) extends EmitterBuilder[F, E, E, R] {

  def transform[T](tr: Observable[E] => Observable[T]): EmitterBuilder[F, E, T, R] =
    new TransformingEmitterBuilder[F, E, T, R](tr, create)

  def -->(sink: Sink[F, _ >: E]): F[R] = effectF.pure(create(sink.observer))
}