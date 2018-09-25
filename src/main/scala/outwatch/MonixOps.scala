package outwatch

import cats.effect.IO
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.subjects.PublishSubject
import monix.reactive.{Observable, Observer}

trait MonixOps {
  type ProHandler[-I, +O] = Observable[O] with Observer[I]
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

    def redirectMap[I2](f: I2 => I): ConnectableObserver[I2] = redirect(_.map(f))

    @deprecated("use onNext instead.", "")
    def unsafeOnNext(nextValue: I) = observer.onNext(nextValue)

    def <--(observable: Observable[I])(implicit scheduler: Scheduler): IO[Cancelable] = IO {
      observable.subscribe(observer)
    }
  }

  implicit class RichProHandler[I,O](self: ProHandler[I,O]) {
    def mapObservable[O2](f: O => O2): ProHandler[I, O2] = transformObservable(_.map(f))
    def mapObserver[I2](f: I2 => I): ProHandler[I2, O] with ReactiveConnectable = transformObserver(_.map(f))
    def mapProHandler[I2, O2](write: I2 => I)(read: O => O2): ProHandler[I2, O2] with ReactiveConnectable = transformProHandler[I2, O2](_.map(write))(_.map(read))

    def collectObservable[O2](f: PartialFunction[O, O2]): ProHandler[I, O2] = transformObservable(_.collect(f))
    def collectObserver[I2](f: PartialFunction[I2, I]): ProHandler[I2, O] with ReactiveConnectable = transformObserver(_.collect(f))
    def collectProHandler[I2, O2](write: PartialFunction[I2, I])(read: PartialFunction[O, O2]): ProHandler[I2, O2] with ReactiveConnectable = transformProHandler[I2, O2](_.collect(write))(_.collect(read))

    def filterObservable(f: O => Boolean): ProHandler[I, O] = transformObservable(_.filter(f))
    def filterObserver(f: I => Boolean): ProHandler[I, O] with ReactiveConnectable = transformObserver(_.filter(f))
    def filterProHandler(write: I => Boolean)(read: O => Boolean): ProHandler[I, O] with ReactiveConnectable = transformProHandler[I, O](_.filter(write))(_.filter(read))

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

    def mapHandler[T2](write: T2 => T)(read: T => T2)(implicit scheduler: Scheduler): Handler[T2] with ReactiveConnectable = ProHandler.connectable(self.redirectMap(write), self.map(read))
    def collectHandler[T2](write: PartialFunction[T2, T])(read: PartialFunction[T, T2])(implicit scheduler: Scheduler): Handler[T2] with ReactiveConnectable = ProHandler.connectable(self.redirect(_.collect(write)), self.collect(read))
    def filterHandler(write: T => Boolean)(read: T => Boolean)(implicit scheduler: Scheduler): Handler[T] with ReactiveConnectable = ProHandler.connectable(self.redirect(_.filter(write)), self.filter(read))

    def transformHandler[T2](write: Observable[T2] => Observable[T])(read: Observable[T] => Observable[T2]): Handler[T2] with ReactiveConnectable = ProHandler.connectable(self.redirect(write), read(self))
  }

}

