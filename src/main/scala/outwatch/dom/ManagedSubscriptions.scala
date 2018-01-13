package outwatch.dom

import cats.effect.{Effect, IO}
import cats.effect.implicits._
import cats.implicits._
import monix.execution.Ack.Continue
import monix.execution.cancelables.CompositeCancelable
import monix.execution.{Cancelable, Scheduler}
import org.scalajs.dom
import outwatch.dom.dsl.attributes.lifecycle

trait ManagedSubscriptions {

  def managed[F[_]:Effect](subscription: F[Cancelable])(implicit s: Scheduler): VDomModifier = {
    subscription.flatMap { sub: Cancelable =>
      lifecycle.onDestroy --> Sink.create[dom.Element,F](_ => Effect[F].delay {
        sub.cancel()
        Continue
      },(_:Throwable) => Effect[F].pure(Unit), () => Effect[F].pure(Unit))
    }
  }

  def managed[F[_]:Effect](sub1: F[Cancelable], sub2: F[Cancelable], subscriptions: F[Cancelable]*)(implicit s: Scheduler): VDomModifier = {

    (sub1 :: sub2 :: subscriptions.toList).sequence.flatMap { subs: Seq[Cancelable] =>
      val composite = CompositeCancelable(subs: _*)
      lifecycle.onDestroy --> Sink.create[dom.Element,F](_ => Effect[F].delay {
        composite.cancel()
        Continue
      },(_:Throwable) => Effect[F].pure(Unit), () => Effect[F].pure(Unit))
    }
  }

}

object ManagedSubscriptions extends ManagedSubscriptions
