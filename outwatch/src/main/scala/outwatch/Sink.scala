package outwatch

import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.Observer

import scala.concurrent.Future

object Sink {

  def create[T](next: T => Future[Ack],
    error: Throwable => Unit = ex => throw ex,
    complete: () => Unit = () => ()
   ): Observer[T] = new Observer[T] {
    override def onNext(t: T): Future[Ack] = next(t)
    override def onError(ex: Throwable): Unit = error(ex)
    override def onComplete(): Unit = complete()
  }

  def fromFunction[T](next: T => Unit): Observer[T] = Sink.create[T]{ t => next(t); Ack.Continue }
}

trait ReactiveConnectable {
  def connect()(implicit scheduler: Scheduler): Cancelable
}
class ConnectableObserver[T](observer: Observer[T], connection: Scheduler => Cancelable) extends Observer[T] with ReactiveConnectable {
  override def onNext(elem: T): Future[Ack] = observer.onNext(elem)
  override def onError(ex: Throwable): Unit = observer.onError(ex)
  override def onComplete(): Unit = observer.onComplete()
  override def connect()(implicit scheduler: Scheduler): Cancelable = connection(scheduler)
}
