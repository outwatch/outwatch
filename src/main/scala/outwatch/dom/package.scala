package outwatch

import cats.effect.IO
import scala.scalajs.js

package object dom extends Implicits with ManagedSubscriptions with SideEffects with MonixOps {

  val Sink = outwatch.Sink
  val Handler = outwatch.Handler

}
