package outwatch

import cats.effect.Sync
import cats.implicits._
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.{BehaviorSubject, ReplaySubject}
import monix.reactive.{Observable, Observer}

import scala.concurrent.Future

object Handler {
  def empty[F[_]: Sync, T]: F[Handler[T]] = create[F, T]

  @inline def create[F[_]] = new CreatePartiallyApplied[F]
  def create[F[_], T](implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T])
  def create[F[_], T](seed: T)(implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T](seed))


  /* Partial application trick, "kinda-curried type parameters"
   * https://typelevel.org/cats/guidelines.html
   *
   * In scala-js, @inline assures that this trick has no overhead and generates the same code as calling create[F, T](seed)
   * AnyVal still generates code in this code for creating an CreatePartiallyApplied instance.
   *
   */
  @inline final class CreatePartiallyApplied[F[_]] {
    @inline def apply[T](seed: T)(implicit F: Sync[F]): F[Handler[T]] = create[F, T](seed)
  }

  def unsafe[T]: Handler[T] = ReplaySubject.createLimited(1)
  def unsafe[T](seed:T): Handler[T] = BehaviorSubject[T](seed)
}

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
