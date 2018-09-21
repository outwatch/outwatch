package outwatch

import monix.execution.Ack
import monix.reactive.Observer

import scala.concurrent.Future

object Sink {

  def create[T](next: T => Future[Ack],
    error: Throwable => Unit = t => throw t,
    complete: () => Unit = () => ()
   ): Observer[T] = {
    new Observer[T] {
      override def onNext(t: T): Future[Ack] = next(t)
      override def onError(ex: Throwable): Unit = error(ex)
      override def onComplete(): Unit = complete()
    }
  }

  def fromFunction[T](next: T => Unit): Observer[T] = Sink.create[T]{ t => next(t); Ack.Continue }
}


