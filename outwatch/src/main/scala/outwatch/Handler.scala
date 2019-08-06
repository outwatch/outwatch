package outwatch

import cats.effect.Sync
import cats.implicits._
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.{BehaviorSubject, ReplaySubject}
import monix.reactive.{Observable, Observer}

import scala.concurrent.Future

trait HandlerOps[F[_]] {
  object Handler {
    def empty[T](implicit F: Sync[F]): F[Handler[T]] = create[T]

    def create[T](implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T])
    def create[T](seed:T)(implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T](seed))

    def unsafe[T]: Handler[T] = ReplaySubject.createLimited(1)
    def unsafe[T](seed:T): Handler[T] = BehaviorSubject[T](seed)
  }
}

trait ProHandlerOps[F[_]] extends HandlerOps[F] {

  implicit class RichProHandler[I,O](self: ProHandler[I,O]) {
    def mapObservable[O2](f: O => O2): ProHandler[I, O2] = ProHandler(self, self.map(f))
    def mapObserver[I2](f: I2 => I): ProHandler[I2, O] = ProHandler(self.contramap(f), self)
    def mapProHandler[I2, O2](write: I2 => I)(read: O => O2): ProHandler[I2, O2] = ProHandler(self.contramap(write), self.map(read))

    def collectObservable[O2](f: PartialFunction[O, O2]): ProHandler[I, O2] = ProHandler(self, self.collect(f))
    def collectObserver[I2](f: PartialFunction[I2, I]): ProHandler[I2, O] = ProHandler(self.redirectCollect(f), self)
    def collectProHandler[I2, O2](write: PartialFunction[I2, I])(read: PartialFunction[O, O2]): ProHandler[I2, O2] = ProHandler(self.redirectCollect(write), self.collect(read))

    def filterObservable(f: O => Boolean): ProHandler[I, O] = ProHandler(self, self.filter(f))
    def filterObserver(f: I => Boolean): ProHandler[I, O] = ProHandler(self.redirectFilter(f), self)
    def filterProHandler(write: I => Boolean)(read: O => Boolean): ProHandler[I, O] = ProHandler(self.redirectFilter(write), self.filter(read))

    def transformObservable[O2](f: Observable[O] => Observable[O2]): ProHandler[I,O2] = ProHandler(self, f(self))
    def transformObserver[I2](f: Observable[I2] => Observable[I]): ProHandler[I2,O] with ReactiveConnectable = ProHandler.connectable(self.redirect(f), self)
    def transformProHandler[I2, O2](write: Observable[I2] => Observable[I])(read: Observable[O] => Observable[O2]): ProHandler[I2,O2] with ReactiveConnectable = ProHandler.connectable(self.redirect(write), read(self))

    @deprecated("A Handler is already an Observer", "")
    def observer:Observer[I] = self
  }

  implicit class RichHandler[T](self: Handler[T]) {
    def lens[S](seed: T)(read: T => S)(write: (T, S) => T)(implicit scheduler: Scheduler): Handler[S] with ReactiveConnectable = {
      val redirected = self
        .redirect[S](_.withLatestFrom(self.startWith(Seq(seed))){ case (a, b) => write(b, a) })

      ProHandler.connectable(redirected, self.map(read))
    }

    def mapHandler[T2](write: T2 => T)(read: T => T2): Handler[T2] = ProHandler(self.contramap(write), self.map(read))
    def collectHandler[T2](write: PartialFunction[T2, T])(read: PartialFunction[T, T2]): Handler[T2] = ProHandler(self.redirectCollect(write), self.collect(read))
    def filterHandler(write: T => Boolean)(read: T => Boolean): Handler[T] = ProHandler(self.redirectFilter(write), self.filter(read))
    def transformHandler[T2](write: Observable[T2] => Observable[T])(read: Observable[T] => Observable[T2]): Handler[T2] with ReactiveConnectable = ProHandler.connectable(self.redirect(write), read(self))
  }

  object ProHandler{
    def create[I,O](f: I => O)(implicit F: Sync[F]): F[ProHandler[I,O]] = for {
      handler <- Handler.create[I]
    } yield handler.mapObservable[O](f)

    def apply[I,O](observer:Observer[I], observable: Observable[O]):ProHandler[I,O] = new Observable[O] with Observer[I] {
      override def onNext(elem: I): Future[Ack] = observer.onNext(elem)
      override def onError(ex: Throwable): Unit = observer.onError(ex)
      override def onComplete(): Unit = observer.onComplete()
      override def unsafeSubscribeFn(subscriber: Subscriber[O]): Cancelable = observable.unsafeSubscribeFn(subscriber)
    }
    def connectable[I,O](observer: Observer[I] with ReactiveConnectable, observable: Observable[O]):ProHandler[I,O] with ReactiveConnectable = new Observable[O] with Observer[I] with ReactiveConnectable {
      override def onNext(elem: I): Future[Ack] = observer.onNext(elem)
      override def onError(ex: Throwable): Unit = observer.onError(ex)
      override def onComplete(): Unit = observer.onComplete()
      override def connect()(implicit scheduler: Scheduler): Cancelable = observer.connect()
      override def unsafeSubscribeFn(subscriber: Subscriber[O]): Cancelable = observable.unsafeSubscribeFn(subscriber)
    }
  }
}
