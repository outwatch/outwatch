package outwatch

import rxscalajs.Observable

package object dom extends Attributes with Tags with Handlers {
  type Handler[T] = Observable[T] with Sink[T]
}
