package outwatch.dom.helpers

import cats.effect.Effect
import org.scalajs.dom.Event
import outwatch.dom.VDomModifierFactory

trait EmitterFactory[F[+_]] extends VDomModifierFactory[F] with EmitterOps[F] {
  implicit val effectF: Effect[F]

  trait EmitterBuilder[E, O, R] {

    def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[E, T, R]

    def -->(sink: Sink[_ >: O]): F[R]

    def apply[T](value: T): EmitterBuilder[E, T, R] = map(_ => value)

    def apply[T](latest: Observable[T]): EmitterBuilder[E, T, R] = transform(_.withLatestFrom(latest)((_, u) => u))

    @deprecated("Deprecated, use '.map' instead", "0.11.0")
    def apply[T](f: O => T): EmitterBuilder[E, T, R] = map(f)

    def map[T](f: O => T): EmitterBuilder[E, T, R] = transform(_.map(f))

    def filter(predicate: O => Boolean): EmitterBuilder[E, O, R] = transform(_.filter(predicate))

    def collect[T](f: PartialFunction[O, T]): EmitterBuilder[E, T, R] = transform(_.collect(f))
  }

  trait EmitterBuilderFactory {
    def emitterBuilderFactory[E <: Event](eventType: String): SimpleEmitterBuilder[E, Emitter] =
      SimpleEmitterBuilder[E, Emitter](observer => Emitter(eventType, event => observer.onNext(event.asInstanceOf[E])))
  }

  final case class TransformingEmitterBuilder[E, O, R] private[helpers](
    transformer: Observable[E] => Observable[O],
    create: Observer[E] => R
  ) extends EmitterBuilder[E, O, R] {

    def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[E, T, R] = copy(
      transformer = tr compose transformer
    )

    def -->(sink: Sink[_ >: O]): F[R] = {
      val redirected: Sink[E] = sink.unsafeRedirect[E](transformer)
      effectF.pure(create(redirected.observer))
    }
  }

  final case class SimpleEmitterBuilder[E, R](create: Observer[E] => R) extends EmitterBuilder[E, E, R] {

    def transform[T](tr: Observable[E] => Observable[T]): EmitterBuilder[E, T, R] =
      new TransformingEmitterBuilder[E, T, R](tr, create)

    def -->(sink: Sink[_ >: E]): F[R] = effectF.pure(create(sink.observer))
  }
}
