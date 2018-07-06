package outwatch

import cats.effect.IO
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observer

import scala.concurrent.Future

object Sink {
  def create[T](next: T => Future[Ack],
    error: Throwable => Unit = _ => (),
    complete: () => Unit = () => ()
    )(implicit s: Scheduler): IO[Observer[T]] = IO {
      new Observer[T] {
        override def onNext(t: T): Future[Ack] = next(t)
        override def onError(ex: Throwable): Unit = error(ex)
        override def onComplete(): Unit = complete()
      }
    }
}


