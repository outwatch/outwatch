package outwatch

import cats.effect.{Effect, IO}
import outwatch.dom.helpers.SeparatedModifiersFactory
import outwatch.dom.{CompatFactory, Implicits, ManagedSubscriptions, OutwatchDsl, RenderFactory, VDomModifierFactory}
import outwatch.util.SyntaxSugarFactory

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
  with SeparatedModifiersFactory[F]
  with OutwatchDsl[F]
  with SyntaxSugarFactory[F]
  with RenderFactory[F]
  with CompatFactory[F]
{
  implicit val effectF: Effect[F]
}

package object dom extends DomEffect[IO]  { thisDom =>
  implicit val effectF: Effect[IO] = IO.ioConcurrentEffect
}
