package outwatch

import cats.effect.Effect
import cats.implicits._
import monix.execution.Scheduler
import outwatch.Sink.{ObservableSink, SubjectSink}
import outwatch.dom.Observable


object Pipe {
  private[outwatch] def apply[I, O](sink: Sink[I], source: Observable[O]): Pipe[I, O] =
    new ObservableSink[I, O](sink, source)

  /**
    * This function also allows you to create initial values for your newly created Pipe.
    * This is equivalent to calling `startWithMany` with the given values.
    *
    * @param seeds a sequence of initial values that the Pipe will emit.
    * @tparam T the type parameter of the elements
    * @return the newly created Pipe.
    */
  def create[T,F[_]:Effect](seeds: T*)(implicit s: Scheduler): F[Pipe[T, T]] = create[T,F].map { pipe =>
    if (seeds.nonEmpty) {
      pipe.transformSource(_.startWith(seeds))
    }
    else {
      pipe
    }
  }

  def create[T,F[_]:Effect](implicit s: Scheduler): F[Pipe[T, T]] = Effect[F].delay {
    val subjectSink = SubjectSink[T]()
    Pipe(subjectSink, subjectSink)
  }

}
