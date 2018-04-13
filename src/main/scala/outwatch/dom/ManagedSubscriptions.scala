package outwatch.dom

import cats.effect.Effect
import cats.implicits._
import monix.execution.Ack.Continue
import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import org.scalajs.dom
import outwatch.SinkFactory

trait ManagedSubscriptionsFactory[F[+ _]] extends SinkFactory[F] with DomTypesFactory[F] with OutwatchDsl[F] {
  implicit val effectF: Effect[F]

  import dsl.attributes.lifecycle

    def managed(subscription: F[Cancelable])(implicit s: Scheduler): VDomModifier = {
      subscription.flatMap { sub: Cancelable =>
        Sink.create[dom.Element] { _ =>
          sub.cancel()
          effectF.pure(Continue)
        }.flatMap(sink => lifecycle.onDestroy --> sink)
      }
    }

    def managed(sub1: F[Cancelable], sub2: F[Cancelable], subscriptions: F[Cancelable]*)(implicit s: Scheduler): VDomModifier = {

      (sub1 :: sub2 :: subscriptions.toList).sequence.flatMap { subs: Seq[Cancelable] =>
        val composite = CompositeCancelable(subs: _*)
        Sink.create[dom.Element] { _ =>
          composite.cancel()
          effectF.pure(Continue)
        }.flatMap(sink => lifecycle.onDestroy --> sink)
      }
    }

}
