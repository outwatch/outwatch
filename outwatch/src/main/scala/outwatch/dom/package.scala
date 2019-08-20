package outwatch

import monix.reactive.{Observable, Observer}

package object dom extends Implicits with ManagedSubscriptions {

  type ProHandler[-I, +O] = Observable[O] with Observer[I]
  type Handler[T] = ProHandler[T,T]

  object implicits extends Implicits with ManagedSubscriptions
}
