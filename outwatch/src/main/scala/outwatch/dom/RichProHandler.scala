package outwatch
package dom

import cats.implicits._
import monix.reactive.{Observable, Observer}

import outwatch.ReactiveConnectable

trait RichProHandlerOps {
  implicit class RichProHandler[I,O](self: ProHandler[I,O]) {
    def mapObservable[O2](f: O => O2): ProHandler[I, O2] = ProHandlerBuilder(self, self.map(f))
    def mapObserver[I2](f: I2 => I): ProHandler[I2, O] = ProHandlerBuilder(self.contramap(f), self)
    def mapProHandler[I2, O2](write: I2 => I)(read: O => O2): ProHandler[I2, O2] = ProHandlerBuilder(self.contramap(write), self.map(read))

    def collectObservable[O2](f: PartialFunction[O, O2]): ProHandler[I, O2] = ProHandlerBuilder(self, self.collect(f))
    def collectObserver[I2](f: PartialFunction[I2, I]): ProHandler[I2, O] = ProHandlerBuilder(self.redirectCollect(f), self)
    def collectProHandler[I2, O2](write: PartialFunction[I2, I])(read: PartialFunction[O, O2]): ProHandler[I2, O2] = ProHandlerBuilder(self.redirectCollect(write), self.collect(read))

    def filterObservable(f: O => Boolean): ProHandler[I, O] = ProHandlerBuilder(self, self.filter(f))
    def filterObserver(f: I => Boolean): ProHandler[I, O] = ProHandlerBuilder(self.redirectFilter(f), self)
    def filterProHandlerBuilder(write: I => Boolean)(read: O => Boolean): ProHandler[I, O] = ProHandlerBuilder(self.redirectFilter(write), self.filter(read))

    def transformObservable[O2](f: Observable[O] => Observable[O2]): ProHandler[I,O2] = ProHandlerBuilder(self, f(self))
    def transformObserver[I2](f: Observable[I2] => Observable[I]): ProHandler[I2,O] with ReactiveConnectable = ProHandlerBuilder.connectable(self.redirect(f), self)
    def transformProHandler[I2, O2](write: Observable[I2] => Observable[I])(read: Observable[O] => Observable[O2]): ProHandler[I2,O2] with ReactiveConnectable = ProHandlerBuilder.connectable(self.redirect(write), read(self))

    @deprecated("A Handler is already an Observer", "")
    def observer:Observer[I] = self
  }
}
