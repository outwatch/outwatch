package outwatch

import cats.effect.IO
import outwatch.Sink.{ObservableSinkHandler, SubjectSink}
import outwatch.dom.Observable

trait HandlerOps[T] {
  self: Handler[T] =>

  def imap[S](read: T => S)(write: S => T): Handler[S] = { Handler(self.redirect[S](_.map(write)), self.map(read)) }

  def lens[S](seed: T)(read: T => S)(write: (T, S) => T): Handler[S] = {
    val redirected = self.redirect[S](_.withLatestFrom(self.startWith(seed)).map { case (a,b) => write(b,a) })
    Handler(redirected, self.map(read))
  }


  def transformHandlerSink(f: Observable[T] => Observable[T]): Handler[T] = Handler(self.redirect(f), self)

  def transformHandlerSource(f: Observable[T] => Observable[T]): Handler[T] = Handler(self, f(self))

  def transformHandler[S](read: Observable[T] => Observable[S])(write: Observable[S] => Observable[T]): Handler[S] = {
    Handler(self.redirect[S](write), read(self))
  }

}

object Handler {
  private[outwatch] def apply[T](sink: Sink[T], source: Observable[T]): Handler[T] =
    new ObservableSinkHandler[T](sink, source) with HandlerOps[T] with PipeOps[T,T]

  /**
    * This function also allows you to create initial values for your newly created Handler.
    * This is equivalent to calling `startWithMany` with the given values.
    *
    * @param seeds a sequence of initial values that the Handler will emit.
    * @tparam T the type parameter of the elements
    * @return the newly created Handler.
    */
  def create[T](seeds: T*): IO[Handler[T]] = create[T].map { handler =>
    if (seeds.nonEmpty) {
      handler.transformHandlerSource(_.startWithMany(seeds: _*))
    }
    else {
      handler
    }
  }

  def create[T]: IO[Handler[T]] = IO {
    val subjectSink = SubjectSink[T]()
    Handler(subjectSink, subjectSink)
  }
}

