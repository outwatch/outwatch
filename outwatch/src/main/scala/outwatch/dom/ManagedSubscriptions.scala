package outwatch.dom

import cats.effect.IO
import cats.implicits._
import monix.execution.{Cancelable, Scheduler}
import monix.execution.cancelables.CompositeCancelable
import org.scalajs.dom

trait ManagedSubscriptions {

  @inline def managed(subscription: () => Cancelable): VDomModifier = managedElement(_ => subscription())
  @inline def managed(subscription: IO[Cancelable]): VDomModifier = managed(() => subscription.unsafeRunSync())
  def managed(sub1: IO[Cancelable], sub2: IO[Cancelable], subscriptions: IO[Cancelable]*): VDomModifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map(subs => CompositeCancelable(subs: _*))
    managed(composite)
  }

  def managedAction(action: Scheduler => Cancelable): VDomModifier = SchedulerAction(scheduler => managed(() => action(scheduler)))

  object managedElement {
    def apply(subscription: dom.Element => Cancelable): VDomModifier = IO {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount foreach { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount foreach { cancelable.cancel() }
      )
    }

    def asHtml(subscription: dom.html.Element => Cancelable): VDomModifier = IO {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount.asHtml foreach { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount foreach { cancelable.cancel() }
      )
    }

    def asSvg(subscription: dom.svg.Element => Cancelable): VDomModifier = IO {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount.asSvg foreach { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount foreach { cancelable.cancel() }
      )
    }
  }
}

object ManagedSubscriptions extends ManagedSubscriptions
