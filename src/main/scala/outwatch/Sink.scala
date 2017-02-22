package outwatch

import rxscalajs.facade.SubjectFacade
import rxscalajs.subscription.AnonymousSubscription
import rxscalajs.{Observable, Observer, Subject}

sealed trait Sink[T] extends Any {
  def <--(observable: Observable[T]): AnonymousSubscription = {
    observable.subscribe(observer)
  }

  private[outwatch] def observer: Observer[T]

  def redirect[R](projection: Observable[R] => Observable[T]): Sink[R] = {
    Sink.redirect(this)(projection)
  }

  def redirectMap[R](projection: R => T): Sink[R] = {
    Sink.redirectMap(this)(projection)
  }
}

object Sink {
  private final case class SubjectSink[T]() extends Subject[T](new SubjectFacade) with Sink[T] {
    override private[outwatch] def observer = this
  }

  def create[T](onNext: T => Unit): Sink[T] = {
    val subject = Subject[T]
    subject.subscribe(onNext)
    new ObserverSink(subject)
  }

  def createHandler[T]: Observable[T] with Sink[T] = {
    new SubjectSink[T]
  }

  def redirect[T,R](sink: Sink[R])(project: Observable[T] => Observable[R]): Sink[T] = {
    val forward = Sink.createHandler[T]()

    sink match {
      case subject@SubjectSink() => project(forward)
        .takeUntil(subject.ignoreElements.defaultIfEmpty())
        .subscribe(sink.observer)
      case ObserverSink(_) => project(forward)
        .subscribe(sink.observer)
    }

    forward
  }

  def redirect[T,U,R](project: (Observable[T], Observable[U]) => Observable[R])(sink: Sink[R]): (Sink[T], Sink[U]) = {
    val t = Sink.createHandler[T]()
    val u = Sink.createHandler[U]()

    sink match {
      case subject@SubjectSink() => project(t,u)
        .takeUntil(subject.ignoreElements.defaultIfEmpty())
        .subscribe(sink.observer)
      case ObserverSink(_) => project(t,u)
        .subscribe(sink.observer)
    }

    (t, u)
  }

  def redirect[T,U,V,R](project: (Observable[T], Observable[U], Observable[V]) => Observable[R])
                     (sink: Sink[R]): (Sink[T], Sink[U], Sink[V]) = {
    val t = Sink.createHandler[T]()
    val u = Sink.createHandler[U]()
    val v = Sink.createHandler[V]()

    sink match {
      case subject@SubjectSink() => project(t,u,v)
        .takeUntil(subject.ignoreElements.defaultIfEmpty())
        .subscribe(sink.observer)
      case ObserverSink(_) => project(t,u,v)
        .subscribe(sink.observer)
    }

    (t, u, v)
  }

  def redirectMap[T, R](sink: Sink[R])(f: T => R): Sink[T] = {
    redirect(sink)(_.map(f))
  }

}

final case class ObserverSink[T](observer: Observer[T]) extends AnyVal with Sink[T]


