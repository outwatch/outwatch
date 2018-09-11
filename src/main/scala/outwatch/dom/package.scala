package outwatch

import cats.effect.IO
import org.scalajs.dom.Element

package object dom extends Implicits with ManagedSubscriptions with SideEffects with MonixOps {

  val Sink = outwatch.Sink
  val Handler = outwatch.Handler

  type VNodeT[+T <: Element] = IO[VTree[T]]
  type VNode = IO[VTree[Element]]
  type VDomModifier = IO[Modifier]
  object VDomModifier {
    val empty: VDomModifier = IO.pure(EmptyModifier)

    def apply(modifier: VDomModifier, modifier2: VDomModifier, modifiers: VDomModifier*): VDomModifier =
      (Seq(modifier, modifier2) ++ modifiers).sequence.map(CompositeModifier)
    def apply[T](t: T)(implicit as: AsVDomModifier[T]): VDomModifier = as.asVDomModifier(t)
  }
}
