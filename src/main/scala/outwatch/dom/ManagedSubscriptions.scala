package outwatch.dom

import cats.effect.{Effect, Sync}
import cats.implicits._
import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import org.scalajs.dom
import outwatch.dom.dsl.attributes.lifecycle

trait ManagedSubscriptions {

  def managed[F[+_]: Effect](subscription: F[Cancelable])(implicit s: Scheduler): VDomModifierF[F] = {
    subscription.flatMap { sub: Cancelable =>
      Sink.create[F, dom.Element] { (_: dom.Element) =>
        Sync[F].delay(sub.cancel())
      }.flatMap(sink => lifecycle.onDestroy --> sink)
    }
  }

  def managed[F[+_]: Effect](sub1: F[Cancelable], sub2: F[Cancelable], subscriptions: F[Cancelable]*)
             (implicit s: Scheduler): VDomModifierF[F] = {

    (sub1 :: sub2 :: subscriptions.toList).sequence.flatMap { subs: List[Cancelable] =>
      val composite = CompositeCancelable(subs: _*)
      Sink.create[F, dom.Element]{ (_: dom.Element) =>
        Sync[F].delay(composite.cancel())
      }.flatMap(sink => lifecycle.onDestroy --> sink)
    }
  }

}

object ManagedSubscriptions extends ManagedSubscriptions
