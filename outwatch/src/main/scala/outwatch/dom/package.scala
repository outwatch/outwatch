package outwatch

import cats.effect.IO
import monix.reactive.{Observable, Observer}

package object dom extends Implicits with ManagedSubscriptions {
  val Handler = new HandlerOps[IO]{}.Handler

  val ProHandler = new ProHandlerOps[IO]{}.ProHandler

  type ProHandler[-I, +O] = Observable[O] with Observer[I]
  type Handler[T] = ProHandler[T,T]

  val OutWatch = new OutWatchOps[IO]{}.OutWatch

  object io extends ProHandlerOps[IO] with OutWatchOps[IO]
  object implicits extends Implicits with ManagedSubscriptions
}
