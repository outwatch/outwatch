package outwatch

import rxscalajs.facade.SubjectFacade
import rxscalajs.{Observable, Subject}

sealed trait Sink[T] {
  def <--(observable: Observable[T]) = {
    observable.subscribe(this.asInstanceOf[Subject[T]])
  }
}
object Sink {
  private class SubjectSink[T] extends Subject[T](new SubjectFacade) with Sink[T]

  def create[T](onNext: T => Unit): Sink[T] = {
    val sink = new SubjectSink
    sink.subscribe(onNext)
    sink.asInstanceOf[Sink[T]]
  }

  def createHandler[T]: Observable[T] with Sink[T] = {
    val sink = new SubjectSink
    sink.asInstanceOf[Observable[T] with Sink[T]]
  }
}


