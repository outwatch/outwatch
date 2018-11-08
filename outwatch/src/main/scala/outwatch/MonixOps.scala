package outwatch

import cats.effect.IO
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.subjects.PublishSubject
import monix.reactive.{Observable, Observer, OverflowStrategy}
import outwatch.dom.ValueObservable

import scala.concurrent.Future

trait MonixOps {
  type ProHandler[-I, +O] = ValueObservable[O] with Observer[I]
  type Handler[T] = ProHandler[T,T]

  @deprecated("use monix.reactive.Observer instead", "")
  type Sink[-A] = Observer[A]
  @deprecated("use ProHandler instead", "")
  type Pipe[-I, +O] = ProHandler[I,O]

  implicit class RichObserver[I](observer: Observer[I]) {
    def redirect[I2](f: Observable[I2] => Observable[I]): ConnectableObserver[I2] = {
      val subject = PublishSubject[I2]
      val transformed = f(subject)
      new ConnectableObserver[I2](subject, implicit scheduler => transformed.subscribe(observer))
    }

    def redirectMap[I2](f: I2 => I): Observer[I2] = new Observer[I2] {
      override def onNext(elem: I2): Future[Ack] = observer.onNext(f(elem))
      override def onError(ex: Throwable): Unit = observer.onError(ex)
      override def onComplete(): Unit = observer.onComplete()
    }

    def redirectMapMaybe[I2](f: I2 => Option[I]): Observer[I2] = new Observer[I2] {
      override def onNext(elem: I2): Future[Ack] = f(elem).fold[Future[Ack]](Ack.Continue)(observer.onNext(_))
      override def onError(ex: Throwable): Unit = observer.onError(ex)
      override def onComplete(): Unit = observer.onComplete()
    }

    def redirectCollect[I2](f: PartialFunction[I2, I]): Observer[I2] = redirectMapMaybe(f.lift)
    def redirectFilter(f: I => Boolean): Observer[I] = redirectMapMaybe(e => Some(e).filter(f))

    @deprecated("use onNext instead.", "")
    def unsafeOnNext(nextValue: I) = observer.onNext(nextValue)

    @deprecated("use emitter(observable) --> observer for safely using subscriptions in a VNode. Or use observable.subscribe(observer) for manually subscribing to an observable", "")
    def <--(observable: Observable[I])(implicit scheduler: Scheduler): IO[Cancelable] = IO {
      observable.subscribe(observer)
    }
  }

  implicit class RichProHandler[I,O](self: ProHandler[I,O]) {
    def mapObservable[O2](f: O => O2): ProHandler[I, O2] = ProHandler(self, self.map(f))
    def mapObserver[I2](f: I2 => I): ProHandler[I2, O] = ProHandler(self.redirectMap(f), self)
    def mapProHandler[I2, O2](write: I2 => I)(read: O => O2): ProHandler[I2, O2] = ProHandler(self.redirectMap(write), self.map(read))

    def collectObservable[O2](f: PartialFunction[O, O2]): ProHandler[I, O2] = ProHandler(self, self.collect(f))
    def collectObserver[I2](f: PartialFunction[I2, I]): ProHandler[I2, O] = ProHandler(self.redirectCollect(f), self)
    def collectProHandler[I2, O2](write: PartialFunction[I2, I])(read: PartialFunction[O, O2]): ProHandler[I2, O2] = ProHandler(self.redirectCollect(write), self.collect(read))

    def filterObservable(f: O => Boolean): ProHandler[I, O] = ProHandler(self, self.filter(f))
    def filterObserver(f: I => Boolean): ProHandler[I, O] = ProHandler(self.redirectFilter(f), self)
    def filterProHandler(write: I => Boolean)(read: O => Boolean): ProHandler[I, O] = ProHandler(self.redirectFilter(write), self.filter(read))

    def transformObservable[O2](f: ValueObservable[O] => ValueObservable[O2]): ProHandler[I,O2] = ProHandler(self, f(self))
    def transformObserver[I2](f: Observable[I2] => Observable[I]): ProHandler[I2,O] with ReactiveConnectable = ProHandler.connectable(self.redirect(f), self)
    def transformProHandler[I2, O2](write: Observable[I2] => Observable[I])(read: ValueObservable[O] => ValueObservable[O2]): ProHandler[I2,O2] with ReactiveConnectable = ProHandler.connectable(self.redirect(write), read(self))

    @deprecated("A Handler is already an Observer", "")
    def observer:Observer[I] = self
  }

  implicit class RichHandler[T](self: Handler[T]) {
    def lens[S](seed: T)(read: T => S)(write: (T, S) => T): Handler[S] with ReactiveConnectable = {
      val redirected = self.redirect[S](_.withLatestFrom(self.startWith(seed :: Nil).observable){ case (a, b) => write(b, a) })

      ProHandler.connectable(redirected, self.map(read))
    }

    def mapHandler[T2](write: T2 => T)(read: T => T2): Handler[T2] = ProHandler(self.redirectMap(write), self.map(read))
    def collectHandler[T2](write: PartialFunction[T2, T])(read: PartialFunction[T, T2]): Handler[T2] = ProHandler(self.redirectCollect(write), self.collect(read))
    def filterHandler(write: T => Boolean)(read: T => Boolean): Handler[T] = ProHandler(self.redirectFilter(write), self.filter(read))
    def transformHandler[T2](write: Observable[T2] => Observable[T])(read: ValueObservable[T] => ValueObservable[T2]): Handler[T2] with ReactiveConnectable = ProHandler.connectable(self.redirect(write), read(self))
  }
}

