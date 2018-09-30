package outwatch.dom

import cats.effect.IO
import cats.implicits._
import monix.execution.{Cancelable, Scheduler}
import monix.execution.cancelables.CompositeCancelable
import org.scalajs.dom
import outwatch.dom.dsl.attributes.lifecycle

trait ManagedSubscriptions {

  def managed(subscription: IO[Cancelable]): VDomModifier = IO {
    var cancelable: Cancelable = null
    VDomModifier(
      lifecycle.onDomMount handleWith { cancelable = subscription.unsafeRunSync() },
      lifecycle.onDomUnmount handleWith { cancelable.cancel() }
    )
  }

  def managed(sub1: IO[Cancelable], sub2: IO[Cancelable], subscriptions: IO[Cancelable]*): VDomModifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map(subs => CompositeCancelable(subs: _*))
    managed(composite)
  }

  def managedAction(action: Scheduler => IO[Cancelable]): VDomModifier = SchedulerAction(scheduler => managed(action(scheduler)))

  object managedElement {
    def apply(subscription: dom.Element => Cancelable): VDomModifier = IO {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount handleWith { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount handleWith { cancelable.cancel() }
      )
    }

    def asHtml(subscription: dom.html.Element => Cancelable): VDomModifier = IO {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount.asHtml handleWith { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount handleWith { cancelable.cancel() }
      )
    }

    def asSvg(subscription: dom.svg.Element => Cancelable): VDomModifier = IO {
      var cancelable: Cancelable = null
      VDomModifier(
        dsl.onDomMount.asSvg handleWith { elem => cancelable = subscription(elem) },
        dsl.onDomUnmount handleWith { cancelable.cancel() }
      )
    }
  }
}

object ManagedSubscriptions extends ManagedSubscriptions
