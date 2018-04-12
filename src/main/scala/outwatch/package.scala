package outwatch

import cats.effect.Effect
import cats.syntax.all._
import monix.execution.Scheduler
import outwatch.dom.VDomModifierFactory

//package object outwatch extends OutwatchOps[IO]{
//}

trait OutwatchOps[F[+_]] extends VDomModifierFactory[F] with SinkFactory[F]  {
  implicit val effectF:Effect[F]

  object Pipe {
    private[outwatch] def apply[I, O](sink: Sink[I], source: Observable[O]): Pipe[I, O] =
      new Sink.ObservableSink[I, O](sink, source)

    /**
      * This function also allows you to create initial values for your newly created Pipe.
      * This is equivalent to calling `startWithMany` with the given values.
      *
      * @param seeds a sequence of initial values that the Pipe will emit.
      * @tparam T the type parameter of the elements
      * @return the newly created Pipe.
      */
    def create[T](seeds: T*)(implicit s: Scheduler): F[Pipe[T, T]] = create[T].map { pipe =>
      if (seeds.nonEmpty) {
        pipe.transformSource(_.startWith(seeds))
      }
      else {
        pipe
      }
    }

    def create[T](implicit s: Scheduler): F[Pipe[T, T]] = effectF.delay {
      val subjectSink = Sink.SubjectSink[T]()
      Pipe(subjectSink, subjectSink)
    }

  }

  implicit class PipeOps[-I, +O](val self: Pipe[I, O]) {

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
  implicit class FilterSink[I, +O](val self: Pipe[I, O]) {
    def filterSink(f: I => Boolean): Pipe[I, O] = Pipe(self.redirect(_.filter(f)), self)
  }

  object Handler {
    private[outwatch] def apply[T](sink: Sink[T], source: Observable[T]): Handler[T] = Pipe(sink, source)

    /**
      * This function also allows you to create initial values for your newly created Handler.
      * This is equivalent to calling `startWithMany` with the given values.
      *
      * @param seeds a sequence of initial values that the Handler will emit.
      * @tparam T the type parameter of the elements
      * @return the newly created Handler.
      */
    def create[T](seeds: T*)(implicit s: Scheduler): F[Handler[T]] = Pipe.create[T](seeds: _*)

    def create[T](implicit s: Scheduler): F[Handler[T]] = Pipe.create[T]

  }
  implicit class HandlerOps[T](val self: Handler[T]) {

    def imap[S](read: T => S)(write: S => T): Handler[S] = self.mapPipe(write)(read)

    def lens[S](seed: T)(read: T => S)(write: (T, S) => T): Handler[S] = {
      val redirected = self.redirect[S](_.withLatestFrom(self.startWith(Seq(seed))){ case (a, b) => write(b, a) })
      Handler(redirected, self.map(read))
    }

    def transformHandler[S](read: Observable[T] => Observable[S])(write: Observable[S] => Observable[T]): Handler[S] =
      self.transformPipe(write)(read)
  }
}
