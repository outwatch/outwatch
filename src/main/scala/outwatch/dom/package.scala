package outwatch

import cats.effect.Effect
import outwatch.dom.helpers.SeparatedModifiersFactory
import outwatch.dom.{Implicits, ManagedSubscriptions, OutWatchLifeCycleAttributes, VDomModifierFactory}

trait ReactiveTypes[F[+_]] {
  type Observable[+A] = monix.reactive.Observable[A]
  val Observable = monix.reactive.Observable

  type Sink[-A] = outwatch.Sink[F, A]
  type Pipe[-I, +O] = Observable[O] with Sink[I]
  type Handler[T] = Pipe[T, T]
}

trait DomEffect[F[+ _]] extends VDomModifierFactory[F]
  with Implicits[F]
  with ManagedSubscriptions[F]
  with HandlerFactory[F]
  with OutWatchLifeCycleAttributes[F]
  with SeparatedModifiersFactory[F] {
  implicit val effectF: Effect[F]

}

//package object dom extends domEffect[IO] { thisDom =>
//  implicit val effectF: Effect[IO] = IO.ioConcurrentEffect
//}
