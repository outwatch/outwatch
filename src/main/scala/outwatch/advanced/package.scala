package outwatch

import rxscalajs.Observable

package object advanced {

  implicit class TransformHandler[-I, +O](handler: Handler[I, O]) {

    def transformSink[I2](f: Observable[I2] => Observable[I]): Handler[I2, O] = Handler(handler.redirect(f), handler)

    def transformSource[O2](f: Observable[O] => Observable[O2]): Handler[I, O2] = Handler(handler, f(handler))

    def transformHandler[I2, O2](f: Observable[I2] => Observable[I])(g: Observable[O] => Observable[O2]): Handler[I2, O2] =
      Handler(handler.redirect(f), g(handler))
  }

}
