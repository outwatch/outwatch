package outwatch

import outwatch.dom._
import cats.effect.Effect

abstract class domEffect[F[_]:Effect] extends Implicits with ManagedSubscriptions with SideEffects {

  type VNode = F[VTree]
  type VDomModifier = F[VDomModifier_]
  object VDomModifier {
    val empty: VDomModifier = Effect[F].pure(EmptyModifier)

    def apply(modifiers: VDomModifier*): VDomModifier = modifiers.sequence.map(CompositeModifier)
  }

  type Observable[+A] = monix.reactive.Observable[A]
  val Observable = monix.reactive.Observable

  type Sink[-A] = outwatch.Sink[A]
  val Sink = outwatch.Sink

  type Pipe[-I, +O] = outwatch.Pipe[I, O]
  val Pipe = outwatch.Pipe

  type Handler[T] = outwatch.Handler[T]
  val Handler = outwatch.Handler
}

package object dom extends domEffect[cats.effect.IO]
