package outwatch

import cats.effect.IO

package object dom extends Implicits with ManagedSubscriptions with SideEffects {

  type VNode = IO[VTree]
  type VDomModifier = IO[Modifier]
  object VDomModifier {
    val empty: VDomModifier = IO.pure(EmptyModifier)

    def apply(modifier: VDomModifier, modifier2: VDomModifier, modifiers: VDomModifier*): VDomModifier =
      (Seq(modifier, modifier2) ++ modifiers).sequence.map(CompositeModifier)
    def apply[T](t: T)(implicit as: AsVDomModifier[T]): VDomModifier = as.asVDomModifier(t)
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
