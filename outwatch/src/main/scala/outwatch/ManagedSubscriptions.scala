package outwatch

import cats.Monoid
import cats.effect.Sync
import cats.implicits._
import org.scalajs.dom
import scala.scalajs.js
import colibri._
import colibri.effect.RunEffect

trait ManagedSubscriptions {

  @inline def managedSubscribe[F[_] : Source, T](source: F[T]): VDomModifier = managedDelay(Source[F].unsafeSubscribe(source)(Observer.empty))

  @inline def managed[F[_] : Sync : RunEffect, T : CanCancel](subscription: F[T]): VDomModifier = VDomModifier(
    subscription.map[VDomModifier](cancelable => CancelableModifier(() => Cancelable.lift(cancelable)))
  )

  def managed[F[_] : Sync : RunEffect, T : CanCancel : Monoid](sub1: F[T], sub2: F[T], subscriptions: F[T]*): VDomModifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map[T](subs => Monoid[T].combineAll(subs))
    managed(composite)
  }

  @deprecated("Use managedDelay(subscription) instead", "")
  @inline def managedFunction[T : CanCancel](subscription: () => T): VDomModifier = managedDelay(subscription())
  @inline def managedDelay[T : CanCancel](subscription: => T): VDomModifier = CancelableModifier(() => Cancelable.lift(subscription))

  object managedElement {
    def apply[T : CanCancel](subscription: dom.Element => T): VDomModifier = VDomModifier.delay {
      var lastSub: js.UndefOr[T] = js.undefined
      VDomModifier(
        dsl.onDomMount foreach { elem => lastSub = subscription(elem) },
        dsl.onDomUnmount doAction { lastSub.foreach(CanCancel[T].unsafeCancel) }
      )
    }

    def asHtml[T : CanCancel](subscription: dom.html.Element => T): VDomModifier = apply(elem => subscription(elem.asInstanceOf[dom.html.Element]))

    def asSvg[T : CanCancel](subscription: dom.svg.Element => T): VDomModifier = apply(elem => subscription(elem.asInstanceOf[dom.svg.Element]))
  }
}

object ManagedSubscriptions extends ManagedSubscriptions
