package outwatch.dom

import cats.{Applicative, Functor}
import cats.implicits._
import monix.execution.{Cancelable, Scheduler}
import monix.execution.cancelables.CompositeCancelable
import org.scalajs.dom

trait ManagedSubscriptions {

  @inline def managed(subscription: () => Cancelable): VDomModifier = managedElement(_ => subscription())
  @inline def managed[F[_] : RunSyncEffect](subscription: F[Cancelable]): VDomModifier = managed(() => RunSyncEffect[F].unsafeRun(subscription))
  def managed[F[_] : RunSyncEffect : Applicative : Functor](sub1: F[Cancelable], sub2: F[Cancelable], subscriptions: F[Cancelable]*): VDomModifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map[Cancelable](subs => CompositeCancelable(subs: _*))
    managed(composite)
  }

  def managedAction(action: Scheduler => Cancelable): VDomModifier = SchedulerAction(scheduler => managed(() => action(scheduler)))

  object managedElement {
    def apply(subscription: dom.Element => Cancelable): VDomModifier = VDomModifier.delay {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount foreach { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount foreach { cancelable.cancel() }
      )
    }

    def asHtml(subscription: dom.html.Element => Cancelable): VDomModifier = VDomModifier.delay {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount.asHtml foreach { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount foreach { cancelable.cancel() }
      )
    }

    def asSvg(subscription: dom.svg.Element => Cancelable): VDomModifier = VDomModifier.delay {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount.asSvg foreach { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount foreach { cancelable.cancel() }
      )
    }
  }
}

object ManagedSubscriptions extends ManagedSubscriptions
