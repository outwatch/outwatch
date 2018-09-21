package outwatch

import cats.effect.IO
import monix.execution.{Ack, Cancelable}
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.{BehaviorSubject, ReplaySubject}
import monix.reactive.{Observable, Observer}

import scala.concurrent.Future

object Handler {
  def empty[T]:IO[Handler[T]] = create[T]

  def create[T]:IO[Handler[T]] = IO(created[T])
  def create[T](seed:T):IO[Handler[T]] = IO(created[T](seed))

  def created[T]:Handler[T] = ReplaySubject.createLimited(1)
  def created[T](seed:T):Handler[T] = BehaviorSubject[T](seed)
}

object ProHandler {
  def create[I,O](f: I => O): IO[ProHandler[I,O]] = for {
    handler <- Handler.create[I]
  } yield handler.mapObservable[O](f)

  def apply[I,O](observer:Observer[I], observable: Observable[O]):ProHandler[I,O] = new Observable[O] with Observer[I] {
    override def onNext(elem: I): Future[Ack] = observer.onNext(elem)
    override def onError(ex: Throwable): Unit = observer.onError(ex)
    override def onComplete(): Unit = observer.onComplete()
    override def unsafeSubscribeFn(subscriber: Subscriber[O]): Cancelable = observable.unsafeSubscribeFn(subscriber)
  }
}
