package outwatch.dom

import cats.effect.Sync
import cats.implicits._
import monix.reactive.{Observable, Observer}
import scala.concurrent.Future
import monix.execution.{Ack, Cancelable}
import monix.reactive.observers.Subscriber
import outwatch.ReactiveConnectable
import monix.execution.Scheduler

object ProHandler {
  def create[F[_]: Sync, I,O](f: I => O): F[ProHandler[I,O]] = for {
    handler <- Handler.create[F, I]
  } yield handler.mapObservable[O](f)

  def apply[I,O](observer: Observer[I], observable: Observable[O]): ProHandler[I,O] = new Observable[O] with Observer[I] {
    override def onNext(elem: I): Future[Ack] = observer.onNext(elem)
    override def onError(ex: Throwable): Unit = observer.onError(ex)
    override def onComplete(): Unit = observer.onComplete()
    override def unsafeSubscribeFn(subscriber: Subscriber[O]): Cancelable = observable.unsafeSubscribeFn(subscriber)
  }

  def connectable[I,O](observer: Observer[I] with ReactiveConnectable, observable: Observable[O]): ProHandler[I,O] with ReactiveConnectable = new Observable[O] with Observer[I] with ReactiveConnectable {
    override def onNext(elem: I): Future[Ack] = observer.onNext(elem)
    override def onError(ex: Throwable): Unit = observer.onError(ex)
    override def onComplete(): Unit = observer.onComplete()
    override def connect()(implicit scheduler: Scheduler): Cancelable = observer.connect()
    override def unsafeSubscribeFn(subscriber: Subscriber[O]): Cancelable = observable.unsafeSubscribeFn(subscriber)
  }
}
