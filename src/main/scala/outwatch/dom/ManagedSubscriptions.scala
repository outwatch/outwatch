package outwatch.dom

import cats.effect.IO
import monix.execution.Ack.Continue
import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import org.scalajs.dom
import outwatch.dom.dsl.attributes.lifecycle

trait ManagedSubscriptions {

  def managed(subscription: IO[Cancelable])(implicit s: Scheduler): VDomModifier = {
    subscription.flatMap { sub: Cancelable =>
      Sink.create[dom.Element] { _ =>
        sub.cancel()
        Continue
      }.flatMap( sink => lifecycle.onDestroy --> sink)
    }
  }

  def managed(sub1: IO[Cancelable], sub2: IO[Cancelable], subscriptions: IO[Cancelable]*)(implicit s: Scheduler): VDomModifier = {

    (sub1 :: sub2 :: subscriptions.toList).sequence.flatMap { subs: Seq[Cancelable] =>
      val composite = CompositeCancelable(subs: _*)
      Sink.create[dom.Element]{ _ =>
        composite.cancel()
        Continue
      }.flatMap(sink => lifecycle.onDestroy --> sink)
    }
  }

}

object ManagedSubscriptions extends ManagedSubscriptions
