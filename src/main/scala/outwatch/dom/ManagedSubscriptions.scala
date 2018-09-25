package outwatch.dom

import cats.effect.IO
import cats.implicits._
import monix.execution.{Cancelable, Scheduler}
import monix.execution.cancelables.CompositeCancelable
import outwatch.dom.dsl.attributes.lifecycle
import outwatch.dom.helpers.QueuedCancelable

trait ManagedSubscriptions {

  def managed(subscription: IO[Cancelable]): VDomModifier = IO {
    val cancelable = new QueuedCancelable()
    VDomModifier(
      lifecycle.onDomMount handleWith { cancelable.enqueue(subscription.unsafeRunSync()) },
      lifecycle.onDomUnmount handleWith { cancelable.dequeue().cancel() }
    )
  }

  def managed(sub1: IO[Cancelable], sub2: IO[Cancelable], subscriptions: IO[Cancelable]*): VDomModifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map(subs => CompositeCancelable(subs: _*))
    managed(composite)
  }

  def managed(action: Scheduler => IO[Cancelable]): VDomModifier = SchedulerAction(scheduler => managed(action(scheduler)))
}

object ManagedSubscriptions extends ManagedSubscriptions
