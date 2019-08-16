package outwatch
package dom

import cats.implicits._
import monix.execution.Scheduler
import monix.reactive.Observable

import outwatch.ReactiveConnectable

trait RichHandlerOps {
  implicit class RichHandler[T](self: Handler[T]) {
    def lens[S](seed: T)(read: T => S)(write: (T, S) => T)(implicit scheduler: Scheduler): Handler[S] with ReactiveConnectable = {
      val redirected = self
        .redirect[S](_.withLatestFrom(self.startWith(Seq(seed))){ case (a, b) => write(b, a) })

      ProHandlerBuilder.connectable(redirected, self.map(read))
    }

    def mapHandler[T2](write: T2 => T)(read: T => T2): Handler[T2] = ProHandlerBuilder(self.contramap(write), self.map(read))
    def collectHandler[T2](write: PartialFunction[T2, T])(read: PartialFunction[T, T2]): Handler[T2] = ProHandlerBuilder(self.redirectCollect(write), self.collect(read))
    def filterHandler(write: T => Boolean)(read: T => Boolean): Handler[T] = ProHandlerBuilder(self.redirectFilter(write), self.filter(read))
    def transformHandler[T2](write: Observable[T2] => Observable[T])(read: Observable[T] => Observable[T2]): Handler[T2] with ReactiveConnectable = ProHandlerBuilder.connectable(self.redirect(write), read(self))
  }
}