package outwatch.dom

import cats.effect.{Effect, Sync}
import cats.implicits._
import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import org.scalajs.dom
import outwatch.SinkFactory

trait ManagedSubscriptions[F[+_]] extends SinkFactory[F] with OutWatchLifeCycleAttributes[F] {
  implicit val effectF: Effect[F]

  def managed(subscription: F[Cancelable])(implicit s: Scheduler): VDomModifierF[F] = {
    subscription.flatMap { sub: Cancelable =>
      Sink.create[dom.Element] { (_: dom.Element) =>
        Sync[F].delay(sub.cancel())
      }.flatMap(sink => onDestroy --> sink)
    }
  }

  def managed(sub1: F[Cancelable], sub2: F[Cancelable], subscriptions: F[Cancelable]*)
             (implicit s: Scheduler): VDomModifierF[F] = {

    (sub1 :: sub2 :: subscriptions.toList).sequence.flatMap { subs: List[Cancelable] =>
      val composite = CompositeCancelable(subs: _*)
      Sink.create[dom.Element]{ (_: dom.Element) =>
        Sync[F].delay(composite.cancel())
      }.flatMap(sink => onDestroy --> sink)
    }
  }

}

//object ManagedSubscriptions extends ManagedSubscriptions
