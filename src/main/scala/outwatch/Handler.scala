package outwatch

import monix.execution.Scheduler
import outwatch.dom.Observable
import cats.effect.Effect

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
  def create[T,F[_]:Effect](seeds: T*)(implicit s: Scheduler): F[Handler[T]] = Pipe.create[T,F](seeds: _*)

  def create[T,F[_]:Effect](implicit s: Scheduler): F[Handler[T]] = Pipe.create[T,F]

}

