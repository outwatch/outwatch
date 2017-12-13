import outwatch.dom.Observable

package object outwatch {
  type Pipe[-I, +O] = Observable[O] with Sink[I]
  type Handler[T] = Pipe[T, T]

  implicit class PipeOps[-I, +O](val self: Pipe[I, O]) extends AnyVal {

    def mapSink[I2](f: I2 => I): Pipe[I2, O] = Pipe(self.redirectMap(f), self)

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

  // This is not in PipeOps, because I is contravariant
  implicit class FilterSink[I, +O](val self: Pipe[I, O]) extends AnyVal {
    def filterSink(f: I => Boolean): Pipe[I, O] = Pipe(self.redirect(_.filter(f)), self)
  }

  implicit class HandlerOps[T](val self: Handler[T]) extends AnyVal {

    def imap[S](read: T => S)(write: S => T): Handler[S] = self.mapPipe(write)(read)

    def lens[S](seed: T)(read: T => S)(write: (T, S) => T): Handler[S] = {
      val redirected = self.redirect[S](_.withLatestFrom(self.startWith(Seq(seed))){ case (a, b) => write(b, a) })
      Handler(redirected, self.map(read))
    }

    def transformHandler[S](read: Observable[T] => Observable[S])(write: Observable[S] => Observable[T]): Handler[S] =
      self.transformPipe(write)(read)
  }
}
