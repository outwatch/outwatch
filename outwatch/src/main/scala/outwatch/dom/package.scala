package outwatch

import cats.effect.IO
import monix.reactive.{Observable, Observer}

package object dom extends Implicits with ManagedSubscriptions {
  @deprecated("Better to extend HandlerOps[F], where you provide IO, or any context with a Sync instance", "")
  val Handler = new HandlerOps[IO]{}.Handler

  @deprecated("Better to extend ProHandlerOps[F], where you provide IO, or any context with a Sync instance", "")
  val ProHandler = new ProHandlerOps[IO]{}.ProHandler

  type ProHandler[-I, +O] = Observable[O] with Observer[I]
  type Handler[T] = ProHandler[T,T]
}
