package outwatch

import cats.effect.IO
import outwatch.Sink.{ObservableSink, SubjectSink}
import rxscalajs.Observable


trait HandlerOps[-I, +O] { self : Handler[I, O]  =>

  def mapSink[I2](f: I2 => I): Handler[I2, O] = Handler(redirectMap(f), self)

  def collectSink[I2](f: PartialFunction[I2, I]): Handler[I2, O] = Handler(self.redirect(_.collect(f)), self)

  def mapSource[O2](f: O => O2): Handler[I, O2] = Handler(self, self.map(f))

  def collectSource[O2](f: PartialFunction[O, O2]): Handler[I, O2] = Handler(self, self.collect(f))

  def filterSource(f: O => Boolean): Handler[I, O] = Handler(self, self.filter(f))

  def mapHandler[I2, O2](f: I2 => I)(g: O => O2): Handler[I2, O2] = Handler(self.redirectMap(f), self.map(g))

  def collectHandler[I2, O2](f: PartialFunction[I2, I])(g: PartialFunction[O, O2]): Handler[I2, O2] = Handler(
    self.redirect(_.collect(f)), self.collect(g)
  )

  def transformSink[I2](f: Observable[I2] => Observable[I]): Handler[I2, O] = Handler(self.redirect(f), self)

  def transformSource[O2](f: Observable[O] => Observable[O2]): Handler[I, O2] = Handler(self, f(self))

  def transformHandler[I2, O2](f: Observable[I2] => Observable[I])(g: Observable[O] => Observable[O2]): Handler[I2, O2] =
    Handler(self.redirect(f), g(self))
}


object Handler {

  private[outwatch] def apply[I, O](sink: Sink[I], source: Observable[O]): Handler[I, O] =
    new ObservableSink[I, O](sink, source) with HandlerOps[I, O]

  implicit class FilterSink[I, +O](handler: Handler[I, O]) {
    def filterSink(f: I => Boolean): Handler[I, O] = Handler(handler.redirect(_.filter(f)), handler)
  }

  /**
    * This function also allows you to create initial values for your newly created Handler.
    * This is equivalent to calling `startWithMany` with the given values.
    * @param seeds a sequence of initial values that the Handler will emit.
    * @tparam T the type parameter of the elements
    * @return the newly created Handler.
    */
  def create[T](seeds: T*): IO[Handler[T, T]] = create[T].map { handler =>
    if (seeds.nonEmpty) {
      handler.transformSource(_.startWithMany(seeds: _*))
    }
    else {
      handler
    }
  }

  def create[T]: IO[Handler[T, T]] = IO {
    val sink = SubjectSink[T]()
    Handler(sink, sink)
  }
}
