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


  implicit class RichObserver[I](observer: Observer[I])(implicit scheduler: Scheduler) {
    def redirect[I2](f: Observable[I2] => Observable[I]): Observer[I2] = {
      val subject = PublishSubject[I2]
      f(subject).subscribe(observer)
      subject
    }

    def redirectMap[I2](f: I2 => I): Observer[I2] = redirect(_.map(f))

    @deprecated("use onNext instead.", "")
    def unsafeOnNext(nextValue: I) = observer.onNext(nextValue)

    def <--(observable: Observable[I]): IO[Cancelable] = IO {
      observable.subscribe(observer)
    }
  }

  implicit class RichProHandler[I,O](self: ProHandler[I,O]) {
    def mapObservable[O2](f: O => O2): ProHandler[I, O2] = ProHandler(self.map(f), self)
    def mapObserver[I2](f: I2 => I)(implicit scheduler: Scheduler): ProHandler[I2, O] = ProHandler(self, self.redirectMap(f))
    def mapProHandler[I2, O2](read: O => O2)(write: I2 => I)(implicit scheduler: Scheduler): ProHandler[I2, O2] = ProHandler(self.map(read), self.redirectMap(write))

    def collectObserver[I2](f: PartialFunction[I2, I])(implicit scheduler: Scheduler): ProHandler[I2, O] = ProHandler(self, self.redirect(_.collect(f)))
    def collectObservable[O2](f: PartialFunction[O, O2]): ProHandler[I, O2] = ProHandler(self.collect(f), self)
    def collectHandler[I2, O2](f: PartialFunction[I2, I])(g: PartialFunction[O, O2])(implicit scheduler: Scheduler): ProHandler[I2, O2] = ProHandler( self.collect(g), self.redirect(_.collect(f)))

    def filterObservable(f: O => Boolean): ProHandler[I, O] = ProHandler(self.filter(f), self)
    def filterObserver(f: I => Boolean)(implicit scheduler: Scheduler): ProHandler[I, O] = ProHandler(self, self.redirect(_.filter(f)))

    def transformObservable[O2](f: Observable[O] => Observable[O2]): ProHandler[I,O2] = {
      ProHandler(f(self), self)
    }

    def transformObserver[I2](f: Observable[I2] => Observable[I])(implicit scheduler: Scheduler): ProHandler[I2,O] = {
      ProHandler(self, self.redirect(f))
    }

    def transformProHandler[I2, O2](read: Observable[O] => Observable[O2])(write: Observable[I2] => Observable[I])(implicit scheduler: Scheduler): ProHandler[I2,O2] = {
      ProHandler(read(self), self.redirect(write))
    }

    def handlerStartWith(seeds:Seq[O]):ProHandler[I,O] = {
      transformObservable(_.startWith(seeds))
    }

    @deprecated("A Handler is already an Observer", "")
    def observer:Observer[I] = self
  }

  implicit class RichHandler[T](self: Handler[T]) {
    def lens[S](seed: T)(read: T => S)(write: (T, S) => T)(implicit scheduler: Scheduler): ProHandler[S,S] = {
      val redirected = self
        .redirect[S](_.withLatestFrom(self.startWith(Seq(seed))){ case (a, b) => write(b, a) })

      ProHandler(self.map(read), redirected)
    }

    def mapHandler[T2](read: T => T2)(write: T2 => T)(implicit scheduler: Scheduler): Handler[T2] = ProHandler(self.map(read), self.redirectMap(write))
    def transformHandler[T2](read: Observable[T] => Observable[T2])(write: Observable[T2] => Observable[T])(implicit scheduler: Scheduler): Handler[T2] = {
      ProHandler(read(self), self.redirect(write))
    }
  }

}

