package outwatch

import cats.effect.IO
import outwatch.Sink.{ObservableSink, SubjectSink}
import rxscalajs.Observable


trait PipeOps[-I, +O] { self : Pipe[I, O]  =>

  def mapSink[I2](f: I2 => I): Pipe[I2, O] = Pipe(redirectMap(f), self)

  def mapSource[O2](f: O => O2): Pipe[I, O2] = Pipe(self, self.map(f))

  def mapPipe[I2, O2](f: I2 => I)(g: O => O2): Pipe[I2, O2] = Pipe(self.redirectMap(f), self.map(g))


  def collectSink[I2](f: PartialFunction[I2, I]): Pipe[I2, O] = Pipe(self.redirect(_.collect(f)), self)

  def collectSource[O2](f: PartialFunction[O, O2]): Pipe[I, O2] = Pipe(self, self.collect(f))

  def collectPipe[I2, O2](f: PartialFunction[I2, I])(g: PartialFunction[O, O2]): Pipe[I2, O2] = Pipe(
    self.redirect(_.collect(f)), self.collect(g)
  )


  def filterSource(f: O => Boolean): Pipe[I, O] = Pipe(self, self.filter(f))


  def transformSink[I2](f: Observable[I2] => Observable[I]): Pipe[I2, O] = Pipe(self.redirect(f), self)

  def transformSource[O2](f: Observable[O] => Observable[O2]): Pipe[I, O2] = Pipe(self, f(self))

  def transformPipe[I2, O2](f: Observable[I2] => Observable[I])(g: Observable[O] => Observable[O2]): Pipe[I2, O2] =
    Pipe(self.redirect(f), g(self))
}


object Pipe {
  private[outwatch] def apply[I, O](sink: Sink[I], source: Observable[O]): Pipe[I, O] =
    new ObservableSink[I, O](sink, source) with PipeOps[I, O]

  // This is not in PipeOps, because I is contravariant
  implicit class FilterSink[I, +O](pipe: Pipe[I, O]) {
    def filterSink(f: I => Boolean): Pipe[I, O] = Pipe(pipe.redirect(_.filter(f)), pipe)
  }

  /**
    * This function also allows you to create initial values for your newly created Pipe.
    * This is equivalent to calling `startWithMany` with the given values.
    * @param seeds a sequence of initial values that the Pipe will emit.
    * @tparam T the type parameter of the elements
    * @return the newly created Pipe.
    */
  def create[T](seeds: T*): IO[Pipe[T, T]] = create[T].map { pipe =>
    if (seeds.nonEmpty) {
      pipe.transformSource(_.startWithMany(seeds: _*))
    }
    else {
      pipe
    }
  }

  def create[T]: IO[Pipe[T, T]] = IO {
    val subjectSink = SubjectSink[T]()
    Pipe(subjectSink, subjectSink)
  }
}
