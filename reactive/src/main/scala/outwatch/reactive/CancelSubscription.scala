package outwatch.reactive

trait CancelSubscription[-T] {
  def cancel(subscription: T): Unit
}
object CancelSubscription {
  @inline def apply[T](implicit subscription: CancelSubscription[T]): CancelSubscription[T] = subscription
}
