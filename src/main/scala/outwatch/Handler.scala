package outwatch

import cats.effect.IO
import outwatch.Sink.{ObservableSink, SubjectSink}

object Handler {

  /**
    * This function also allows you to create initial values for your newly created Handler.
    * This is equivalent to calling `startWithMany` with the given values.
    * @param seeds a sequence of initial values that the Handler will emit.
    * @tparam T the type parameter of the elements
    * @return the newly created Handler.
    */
  def create[T](seeds: T*): IO[Pipe[T, T]] = Pipe.create(seeds:_*)

  def create[T]: IO[Pipe[T, T]] = Pipe.create[T]
}

