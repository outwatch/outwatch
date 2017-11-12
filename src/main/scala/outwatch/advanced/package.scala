package outwatch

import rxscalajs.Observable

package object advanced {

  implicit class TransformPipe[-I, +O](pipe: Pipe[I, O]) {

    def transformSink[I2](f: Observable[I2] => Observable[I]): Pipe[I2, O] = Pipe(pipe.sink.redirect(f), pipe.source)

    def transformSource[O2](f: Observable[O] => Observable[O2]): Pipe[I, O2] = Pipe(pipe.sink, f(pipe.source))

    def transformPipe[I2, O2](f: Observable[I2] => Observable[I])(g: Observable[O] => Observable[O2]): Pipe[I2, O2] =
      Pipe(pipe.sink.redirect(f), g(pipe.source))
  }

}
