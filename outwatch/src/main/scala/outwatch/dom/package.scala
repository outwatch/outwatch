package outwatch

package object dom extends Implicits with ManagedSubscriptions with SideEffects with MonixOps {

  val Sink = outwatch.Sink
  val Handler = outwatch.Handler

}
