package outwatch

import rxscalajs.facade.SubjectFacade
import rxscalajs.subscription.AnonymousSubscription
import rxscalajs.{Observable, Observer, Subject}

sealed trait Sink[T] extends Any {
  def <--(observable: Observable[T]): AnonymousSubscription = {
    observable.subscribe(observer)
  }

  private[outwatch] def observer: Observer[T]
}

object Sink {
  private final class SubjectSink[T] extends Subject[T](new SubjectFacade) with Sink[T] {
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
}

class ObserverSink[T](val observer: Observer[T]) extends AnyVal with Sink[T]


