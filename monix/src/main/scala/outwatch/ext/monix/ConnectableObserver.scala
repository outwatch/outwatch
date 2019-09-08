package outwatch.ext.monix

import _root_.monix.execution.{Ack, Cancelable, Scheduler}
import _root_.monix.reactive.Observer

import scala.concurrent.Future

trait ReactiveConnectable {
  def connect()(implicit scheduler: Scheduler): Cancelable
}
class ConnectableObserver[T](val observer: Observer[T], connection: Scheduler => Cancelable) extends Observer[T] with ReactiveConnectable {
  override def onNext(elem: T): Future[Ack] = observer.onNext(elem)
  override def onError(ex: Throwable): Unit = observer.onError(ex)
  override def onComplete(): Unit = observer.onComplete()
  override def connect()(implicit scheduler: Scheduler): Cancelable = connection(scheduler)
}

