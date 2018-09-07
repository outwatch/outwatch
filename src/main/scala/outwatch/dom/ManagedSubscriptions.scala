package outwatch.dom

import cats.effect.IO
import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import outwatch.dom.dsl.attributes.lifecycle

trait ManagedSubscriptions {

  def managed(subscription: IO[Cancelable])(implicit s: Scheduler): VDomModifier = {
    subscription.flatMap { sub: Cancelable =>
      lifecycle.onSnabbdomDestroy --> sideEffect{sub.cancel()}
    }
  }

  def managed(sub1: IO[Cancelable], sub2: IO[Cancelable], subscriptions: IO[Cancelable]*)(implicit s: Scheduler): VDomModifier = {
    (sub1 :: sub2 :: subscriptions.toList).sequence.flatMap { subs: Seq[Cancelable] =>
      val composite = CompositeCancelable(subs: _*)
      lifecycle.onSnabbdomDestroy --> sideEffect{composite.cancel()}
    }
  }

}

object ManagedSubscriptions extends ManagedSubscriptions
