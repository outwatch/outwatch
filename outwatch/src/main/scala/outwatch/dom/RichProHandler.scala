package outwatch
package dom

import cats.implicits._
import monix.reactive.{Observable, Observer}

import outwatch.ReactiveConnectable

trait RichProHandlerOps {
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
}
