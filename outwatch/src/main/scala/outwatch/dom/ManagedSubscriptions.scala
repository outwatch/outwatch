package outwatch.dom

import cats.{Applicative, Functor, Monoid}
import cats.implicits._
import org.scalajs.dom
import scala.scalajs.js
import outwatch.reactive._
import outwatch.effect.RunSyncEffect

trait ManagedSubscriptions {

  @inline def managed[F[_] : RunSyncEffect, T : CancelSubscription](subscription: F[T]): VDomModifier = managedFunction(() => RunSyncEffect[F].unsafeRun(subscription))

  def managed[F[_] : RunSyncEffect : Applicative : Functor, T : CancelSubscription : Monoid](sub1: F[T], sub2: F[T], subscriptions: F[T]*): VDomModifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map[T](subs => Monoid[T].combineAll(subs))
    managed(composite)
  }

  @inline def managedFunction[T : CancelSubscription](subscription: () => T): VDomModifier = SubscriptionModifier(() => Subscription.lift(subscription()))

  object managedElement {
    def apply[T : CancelSubscription](subscription: dom.Element => T): VDomModifier = VDomModifier.delay {
      var lastSub: js.UndefOr[T] = js.undefined
      VDomModifier(
        dsl.onDomMount foreach { elem => lastSub = subscription(elem) },
        dsl.onDomUnmount foreach { lastSub.foreach(CancelSubscription[T].cancel) }
      )
    }

    def asHtml[T : CancelSubscription](subscription: dom.html.Element => T): VDomModifier = apply(elem => subscription(elem.asInstanceOf[dom.html.Element]))

    def asSvg[T : CancelSubscription](subscription: dom.svg.Element => T): VDomModifier = apply(elem => subscription(elem.asInstanceOf[dom.svg.Element]))
  }
}

object ManagedSubscriptions extends ManagedSubscriptions
