package outwatch.reactive

import cats.Functor

// This typeclass represents the capability of managing a subscription
// in type T. That means, for any value of T, we can embed a subscription
// into this value and get a new T. That means the type T owns
// this subscription and decides when to subscribe and when to unsubscribe.

trait SubscriptionOwner[T] {
  def own(owner: T)(subscription: () => Subscription): T
}
object SubscriptionOwner {
  @inline def apply[T](implicit owner: SubscriptionOwner[T]): SubscriptionOwner[T] = owner

  implicit def functorOwner[F[_] : Functor, T : SubscriptionOwner]: SubscriptionOwner[F[T]] = new SubscriptionOwner[F[T]] {
    def own(owner: F[T])(subscription: () => Subscription): F[T] = Functor[F].map(owner)(owner => SubscriptionOwner[T].own(owner)(subscription))
  }
}
