package outwatch.ext

import cats.Monoid

import _root_.monix.execution.{Ack, Cancelable}
import _root_.monix.execution.cancelables.CompositeCancelable
import _root_.monix.reactive.{Observable, Observer}
import _root_.monix.reactive.subjects.PublishSubject

import scala.concurrent.Future

package object monix extends MonixReactive {

  implicit class RichObserver[I](val observer: Observer[I]) extends AnyVal {
    def redirect[I2](f: Observable[I2] => Observable[I]): ConnectableObserver[I2] = {
      val subject = PublishSubject[I2]
      val transformed = f(subject)
      new ConnectableObserver[I2](subject, implicit scheduler => transformed.subscribe(observer))
    }

    @deprecated("Use contramap instead.", "")
    @inline def redirectMap[I2](f: I2 => I): Observer[I2] = observer.contramap(f)

    def redirectMapMaybe[I2](f: I2 => Option[I]): Observer[I2] = new Observer[I2] {
      override def onNext(elem: I2): Future[Ack] = f(elem).fold[Future[Ack]](Ack.Continue)(observer.onNext(_))
      override def onError(ex: Throwable): Unit = observer.onError(ex)
      override def onComplete(): Unit = observer.onComplete()
    }

    def redirectCollect[I2](f: PartialFunction[I2, I]): Observer[I2] = redirectMapMaybe(f.lift)
    def redirectFilter(f: I => Boolean): Observer[I] = redirectMapMaybe(e => Some(e).filter(f))
  }

  implicit class RichProHandler[I,O](val self: MonixProHandler[I,O]) extends AnyVal {
    def mapObservable[O2](f: O => O2): MonixProHandler[I, O2] = MonixProHandler(self, self.map(f))
    def mapObserver[I2](f: I2 => I): MonixProHandler[I2, O] = MonixProHandler(self.contramap(f), self)
    def mapProHandler[I2, O2](write: I2 => I)(read: O => O2): MonixProHandler[I2, O2] = MonixProHandler(self.contramap(write), self.map(read))

    def collectObservable[O2](f: PartialFunction[O, O2]): MonixProHandler[I, O2] = MonixProHandler(self, self.collect(f))
    def collectObserver[I2](f: PartialFunction[I2, I]): MonixProHandler[I2, O] = MonixProHandler(self.redirectCollect(f), self)
    def collectProHandler[I2, O2](write: PartialFunction[I2, I])(read: PartialFunction[O, O2]): MonixProHandler[I2, O2] = MonixProHandler(self.redirectCollect(write), self.collect(read))

    def filterObservable(f: O => Boolean): MonixProHandler[I, O] = MonixProHandler(self, self.filter(f))
    def filterObserver(f: I => Boolean): MonixProHandler[I, O] = MonixProHandler(self.redirectFilter(f), self)
    def filterProHandler(write: I => Boolean)(read: O => Boolean): MonixProHandler[I, O] = MonixProHandler(self.redirectFilter(write), self.filter(read))

    def transformObservable[O2](f: Observable[O] => Observable[O2]): MonixProHandler[I,O2] = MonixProHandler(self, f(self))
    def transformObserver[I2](f: Observable[I2] => Observable[I]): MonixProHandler[I2,O] with ReactiveConnectable = MonixProHandler.connectable(self.redirect(f), self)
    def transformProHandler[I2, O2](write: Observable[I2] => Observable[I])(read: Observable[O] => Observable[O2]): MonixProHandler[I2,O2] with ReactiveConnectable = MonixProHandler.connectable(self.redirect(write), read(self))
  }

  implicit class RichHandler[T](val self: MonixHandler[T]) extends AnyVal {
    def lens[S](seed: T)(read: T => S)(write: (T, S) => T): MonixHandler[S] with ReactiveConnectable = {
      val redirected = self
        .redirect[S](_.withLatestFrom(self.startWith(Seq(seed))){ case (a, b) => write(b, a) })

      MonixProHandler.connectable(redirected, self.map(read))
    }

    def mapHandler[T2](write: T2 => T)(read: T => T2): MonixHandler[T2] = MonixProHandler(self.contramap(write), self.map(read))
    def collectHandler[T2](write: PartialFunction[T2, T])(read: PartialFunction[T, T2]): MonixHandler[T2] = MonixProHandler(self.redirectCollect(write), self.collect(read))
    def filterHandler(write: T => Boolean)(read: T => Boolean): MonixHandler[T] = MonixProHandler(self.redirectFilter(write), self.filter(read))
    def transformHandler[T2](write: Observable[T2] => Observable[T])(read: Observable[T] => Observable[T2]): MonixHandler[T2] with ReactiveConnectable = MonixProHandler.connectable(self.redirect(write), read(self))
  }

  //TODO: add to monix?
  implicit object CancelableMonoid extends Monoid[Cancelable] {
    def empty: Cancelable = Cancelable.empty
    def combine(x: Cancelable, y: Cancelable): Cancelable = CompositeCancelable(x, y)
  }
}
