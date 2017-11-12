package outwatch

import rxscalajs.Observable

import scala.language.implicitConversions

trait Pipe[-I, +O] {
  def sink: Sink[I]

  def source: Observable[O]

  def mapSink[I2](f: I2 => I): Pipe[I2, O] = Pipe(sink.redirectMap(f), source)

  def collectSink[I2](f: PartialFunction[I2, I]): Pipe[I2, O] = Pipe(sink.redirect(_.collect(f)), source)

  def mapSource[O2](f: O => O2): Pipe[I, O2] = Pipe(sink, source.map(f))

  def collectSource[O2](f: PartialFunction[O, O2]): Pipe[I, O2] = Pipe(sink, source.collect(f))

  def filterSource(f: O => Boolean): Pipe[I, O] = Pipe(sink, source.filter(f))

  def mapPipe[I2, O2](f: I2 => I)(g: O => O2): Pipe[I2, O2] = Pipe(sink.redirectMap(f), source.map(g))

  def collectPipe[I2, O2](f: PartialFunction[I2, I])(g: PartialFunction[O, O2]): Pipe[I2, O2] = Pipe(
    sink.redirect(_.collect(f)), source.collect(g)
  )
}

object Pipe {
  implicit def toSink[I](pipe: Pipe[I, _]): Sink[I] = pipe.sink
  implicit def toSource[O](pipe: Pipe[_, O]): Observable[O] = pipe.source

  private[outwatch] def apply[I, O](_sink: Sink[I], _source: Observable[O]): Pipe[I, O] = new Pipe[I, O] {
    def sink: Sink[I] = _sink
    def source: Observable[O] = _source
  }

  implicit class FilterPipe[I, +O](pipe: Pipe[I, O]) {

    def filterSink(f: I => Boolean): Pipe[I, O] = Pipe(pipe.sink.redirect(_.filter(f)), pipe.source)
  }
}


