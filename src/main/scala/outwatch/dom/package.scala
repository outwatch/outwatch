package outwatch

import cats.effect.IO
import scala.scalajs.js

package object dom extends Implicits with ManagedSubscriptions with SideEffects with MonixOps {

  val Sink = outwatch.Sink
  val Handler = outwatch.Handler

  type VNode = IO[VTree]
  type VDomModifier = IO[Modifier]
  object VDomModifier {
    val empty: VDomModifier = IO.pure(EmptyModifier)

    def apply(modifier: VDomModifier, modifier2: VDomModifier, modifiers: VDomModifier*): VDomModifier = IO {
      CompositeModifier(js.Array(modifier.unsafeRunSync, modifier2.unsafeRunSync) ++ modifiers.map(_.unsafeRunSync))
    }
    def apply[T](t: T)(implicit as: AsVDomModifier[T]): VDomModifier = as.asVDomModifier(t)
  }
}
