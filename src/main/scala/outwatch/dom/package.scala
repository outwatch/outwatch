package outwatch

import cats.effect.IO
import monix.reactive.Observable

package object dom extends Implicits with ManagedSubscriptions with SideEffects with MonixOps {

  val Sink = outwatch.Sink
  val Handler = outwatch.Handler

  type VNode = IO[VTree]
  type VDomModifier = IO[Modifier]
  object VDomModifier {
    val empty: VDomModifier = IO.pure(EmptyModifier)

    def apply(modifier: VDomModifier, modifier2: VDomModifier, modifiers: VDomModifier*): VDomModifier =
      (Seq(modifier, modifier2) ++ modifiers).sequence.map(CompositeModifier)
    def apply[T](t: T)(implicit as: AsVDomModifier[T]): VDomModifier = as.asVDomModifier(t)
  }
}
