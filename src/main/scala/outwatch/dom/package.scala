package outwatch

import cats.effect.Effect
import outwatch.dom.helpers.SeparatedModifiersFactory
import outwatch.dom.{Implicits, ManagedSubscriptions, VDomModifierFactory}

trait ReactiveTypes[F[+_]] extends SinkFactory[F] {
  type Observer[-A] = monix.reactive.Observer[A]
  val Observer = monix.reactive.Observer

  type Observable[+A] = monix.reactive.Observable[A]
  val Observable = monix.reactive.Observable

  type Pipe[-I, +O] = Observable[O] with Sink[I]
  type Handler[T] = Pipe[T, T]
}

trait DomEffect[F[+ _]] extends VDomModifierFactory[F]
  with Implicits[F]
  with ManagedSubscriptions[F]
  with OutwatchOps[F]
  with SeparatedModifiersFactory[F] {
  implicit val effectF: Effect[F]

}

//package object dom extends domEffect[IO] { thisDom =>
//  implicit val effectF: Effect[IO] = IO.ioConcurrentEffect
//}
