package outwatch

import cats.{Applicative, Functor, Monoid}
import cats.implicits._
import org.scalajs.dom
import scala.scalajs.js
import colibri._
import colibri.effect.RunSyncEffect

trait ManagedSubscriptions {

  @inline def managed[F[_] : RunSyncEffect, T : CanCancel](subscription: F[T]): VDomModifier = managedFunction(() => RunSyncEffect[F].unsafeRun(subscription))

  def managed[F[_] : RunSyncEffect : Applicative : Functor, T : CanCancel : Monoid](sub1: F[T], sub2: F[T], subscriptions: F[T]*): VDomModifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map[T](subs => Monoid[T].combineAll(subs))
    managed(composite)
  }

  @inline def managedFunction[T : CanCancel](subscription: () => T): VDomModifier = CancelableModifier(() => Cancelable.lift(subscription()))

  object managedElement {
    def apply[T : CanCancel](subscription: dom.Element => T): VDomModifier = VDomModifier.delay {
      var lastSub: js.UndefOr[T] = js.undefined
      VDomModifier(
        dsl.onDomMount foreach { elem => lastSub = subscription(elem) },
        dsl.onDomUnmount foreach { lastSub.foreach(CanCancel[T].cancel) }
      )
    }

    def asHtml[T : CanCancel](subscription: dom.html.Element => T): VDomModifier = apply(elem => subscription(elem.asInstanceOf[dom.html.Element]))

    def asSvg[T : CanCancel](subscription: dom.svg.Element => T): VDomModifier = apply(elem => subscription(elem.asInstanceOf[dom.svg.Element]))
  }
}

object ManagedSubscriptions extends ManagedSubscriptions
