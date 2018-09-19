package outwatch

import cats.effect.IO
import scala.scalajs.js

package object dom extends Implicits with ManagedSubscriptions with SideEffects with MonixOps {

  val Sink = outwatch.Sink
  val Handler = outwatch.Handler

  type VNode = VTree
  type VDomModifier = Modifier
  object VDomModifier {
    val empty: VDomModifier = EmptyModifier

    def apply(modifier: VDomModifier, modifier2: VDomModifier, modifiers: VDomModifier*): VDomModifier =
      CompositeModifier(js.Array(modifier, modifier2) ++ modifiers)
    def apply[T](t: T)(implicit as: AsVDomModifier[T]): VDomModifier = as.asVDomModifier(t)
  }
}
