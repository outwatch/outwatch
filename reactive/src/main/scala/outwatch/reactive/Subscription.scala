package outwatch.reactive

import cats.Monoid

import scala.scalajs.js

trait Subscription {
  def cancel(): Unit
}
object Subscription {

  class Builder extends Subscription {
    private var buffer = new js.Array[Subscription]()

    def +=(subscription: Subscription): Unit =
      if (buffer == null) {
        subscription.cancel()
      } else {
        buffer.push(subscription)
        ()
      }

    def cancel(): Unit =
      if (buffer != null) {
        buffer.foreach(_.cancel())
        buffer = null
      }
  }

  class Variable extends Subscription {
    private var current: Subscription = Subscription.empty

    def update(subscription: Subscription): Unit =
      if (current == null) {
        subscription.cancel()
      } else {
        current.cancel()
        current = subscription
      }

    def cancel(): Unit =
      if (current != null) {
        current.cancel()
        current = null
    }
  }

  class Consecutive extends Subscription {
    private var latest: Subscription = null
    private var subscriptions: js.Array[() => Subscription] = new js.Array[() => Subscription]

    def switch(): Unit = if (latest != null) {
      latest.cancel()
      latest = null
      if (subscriptions != null && subscriptions.nonEmpty) {
        val nextSubscription = subscriptions(0)
        val variable = Subscription.variable()
        latest = variable
        subscriptions.splice(0, deleteCount = 1)
        variable() = nextSubscription()
        ()
      }
    }

    def +=(subscription: () => Subscription): Unit = if (subscriptions != null) {
      if (latest == null) {
        val variable = Subscription.variable()
        latest = variable
        variable() = subscription()
      } else {
        subscriptions.push(subscription)
        ()
      }
    }

    def cancel(): Unit = if (subscriptions != null) {
      subscriptions = null
      if (latest != null) {
        latest.cancel()
        latest = null
      }
    }
  }

  object Empty extends Subscription {
    @inline def cancel(): Unit = ()
  }

  @inline def empty = Empty

  @inline def apply(f: () => Unit) = new Subscription {
    @inline def cancel() = f()
  }

  @inline def lift[T : CancelSubscription](subscription: T) = apply(() => CancelSubscription[T].cancel(subscription))

  @inline def composite(subscriptions: Subscription*): Subscription = compositeFromIterable(subscriptions)
  @inline def compositeFromIterable(subscriptions: Iterable[Subscription]): Subscription = new Subscription {
    def cancel() = subscriptions.foreach(_.cancel())
  }

  @inline def builder(): Builder = new Builder

  @inline def variable(): Variable = new Variable

  @inline def consecutive(): Consecutive = new Consecutive

  implicit object monoid extends Monoid[Subscription] {
    @inline def empty = Subscription.empty
    @inline def combine(a: Subscription, b: Subscription) = Subscription.composite(a, b)
  }

  implicit object cancelSubscription extends CancelSubscription[Subscription] {
    @inline def cancel(subscription: Subscription): Unit = subscription.cancel()
  }
}
