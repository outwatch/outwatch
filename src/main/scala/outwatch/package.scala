import cats.effect.IO
import outwatch.Sink.SubjectSink
import outwatch.advanced._

package object outwatch {

  type >-->[-I, +O] = Pipe[I, O]
  val >--> = Pipe

  type Handler[A] = Pipe[A, A]

  object Handler {

    /**
      * This function also allows you to create initial values for your newly created Handler.
      * This is equivalent to calling `startWithMany` with the given values.
      * @param seeds a sequence of initial values that the Handler will emit.
      * @tparam T the type parameter of the elements
      * @return the newly created Handler.
      */
    def create[T](seeds: T*): IO[Handler[T]] = create[T].map { handler =>
      if (seeds.nonEmpty) {
        handler.transformSource(_.startWithMany(seeds: _*))
      }
      else {
        handler
      }
    }

    def create[T]: IO[Handler[T]] = IO {
      val sink = SubjectSink[T]()
      Pipe(sink, sink)
    }
  }
}
