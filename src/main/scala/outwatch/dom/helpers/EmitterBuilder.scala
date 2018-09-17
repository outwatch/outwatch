package outwatch.dom.helpers

import cats.effect.IO
import monix.execution.Scheduler
import monix.reactive.subjects.PublishSubject
import monix.reactive.{Observable, Observer}
import org.scalajs.dom.Event
import outwatch.dom.{Emitter, helpers}
import outwatch.{RichObserver, SideEffects}


trait EmitterBuilder[E, O, R] extends Any {

  def transform[T](tr: Observable[O] => Observable[T])(implicit scheduler: Scheduler): EmitterBuilder[E, T, R]

  def -->(observer: Observer[O]): IO[R] = handleWith(observer)
  def handleWith(action: O => Unit): IO[R]
  def handleWith(action: => Unit): IO[R] = handleWith(_ => action)
  def handleWith(observer: Observer[O]): IO[R] = handleWith(e => observer.onNext(e))

  def apply[T](value: T): EmitterBuilder[E, T, R] = map(_ => value)

  def apply[T](latest: Observable[T])(implicit scheduler: Scheduler) = transform(_.withLatestFrom(latest)((_, u) => u))

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): EmitterBuilder[E, T, R] = map(f)

  def map[T](f: O => T): EmitterBuilder[E, T, R] = collect { case o => f(o) }

  def filter(predicate: O => Boolean): EmitterBuilder[E, O, R] = collect { case o if predicate(o) => o }

  def collect[T](f: PartialFunction[O, T]): EmitterBuilder[E, T, R] = flatMap(f.lift)

  def flatMap[T](f: O => Option[T]): EmitterBuilder[E, T, R]
}

object EmitterBuilder extends EmitterOps {
  def apply[E <: Event](eventType: String): EmitterBuilder[E, E, Emitter] =
    CustomEmitterBuilder.pure[E, Emitter](f => Emitter(eventType, event => f(event.asInstanceOf[E])))
}

final case class FunctionEmitterBuilder[E, Tmp, O, R] private[helpers](
  transformer: Tmp => Option[O],
  create: (Tmp => Unit) => IO[R]
) extends EmitterBuilder[E, O, R] {

  def transform[T](tr: Observable[O] => Observable[T])(implicit scheduler: Scheduler) =
    new TransformingEmitterBuilder[E, O, T, R](tr, f => create(e => transformer(e).foreach(f)))

  def flatMap[T](f: O => Option[T]): EmitterBuilder[E, T, R] = copy(
    transformer = (e:Tmp) => transformer(e).flatMap(f)
  )

  def handleWith(action: O => Unit): IO[R] = {
    create(e => transformer(e).foreach(action))
  }
}

final case class TransformingEmitterBuilder[E, Tmp, O, R] private[helpers](
  transformer: Observable[Tmp] => Observable[O],
  create: (Tmp => Unit) => IO[R]
)(implicit scheduler: Scheduler) extends EmitterBuilder[E, O, R] {

  def transform[T](tr: Observable[O] => Observable[T])(implicit scheduler: Scheduler) = copy(
    transformer = tr compose transformer
  )

  def flatMap[T](f: O => Option[T]): EmitterBuilder[E, T, R] = {//transform(_.map(f).collect { case Some(o) => o })
    new FunctionEmitterBuilder[E, O, T, R](f, f => create {
      val subject = PublishSubject[Tmp]
      transformer(subject).foreach(f)
      subject.onNext
    })
  }

  def handleWith(action: O => Unit): IO[R] = {
    val subject = PublishSubject[O]
    subject.foreach(action)

    val redirected: Observer[Tmp] = subject.redirect[Tmp](transformer)
    create(redirected.onNext _)
  }
}

final case class CustomEmitterBuilder[E, R](create: (E => Unit) => IO[R]) extends AnyVal with EmitterBuilder[E, E, R] {

  def transform[T](tr: Observable[E] => Observable[T])(implicit scheduler: Scheduler) =
    new TransformingEmitterBuilder[E, E, T, R](tr, create)

  def flatMap[T](f: E => Option[T]): EmitterBuilder[E, T, R] =
    new FunctionEmitterBuilder[E, E, T, R](f, create)

  def handleWith(action: E => Unit): IO[R] = create(action)
}
object CustomEmitterBuilder {
  def pure[E, R](create: (E => Unit) => R) = CustomEmitterBuilder[E, R](f => IO.pure(create(f)))
}
object SimpleEmitterBuilder {
  @deprecated("Use CustomEmitterBuilder.pure instead.", "")
  def apply[E, R](create: Observer[E] => R) = CustomEmitterBuilder.pure[E, R](f => create(SideEffects.observerFromFunction(f)))
}
