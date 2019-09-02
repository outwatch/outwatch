package outwatch.dom

import monix.execution.{Ack, Cancelable}
import monix.reactive.Observable
import monix.reactive.observers.Subscriber
import monix.reactive.OverflowStrategy

import org.scalajs.dom
import scala.scalajs.js

// This is a special Observable for dom events on an EventTarget (e.g. window or document).
// It adds an event listener on subscribe and removes it when the subscription is canceled.
// It honors the Ack of the subscriber and unregisters the events if an Ack.Stop is received.
// It provides convenience methods for doing sync operations on the event before emitting,
// like stopPropagation, stopImmediatePropagation, preventDefault. Because it is not
// sufficient to do them in an observable.doOnNext(_.stopPropagation()), because this
// might be async and event handling/bubbling is done sync.

class EventObservable[EV <: dom.Event](target: dom.EventTarget, eventType: String, operator: EV => Unit) extends Observable[EV] {
  private lazy val base: Observable[EV] = Observable.create(OverflowStrategy.Unbounded) { obs =>
    var isCancel = false

    // var for forward-reference
    var eventHandler: js.Function1[EV, Ack] = null
    def register() = target.addEventListener(eventType, eventHandler)
    def unregister() = if (!isCancel) {
      isCancel = true
      target.removeEventListener(eventType, eventHandler)
    }

    eventHandler = { v =>
      if (!isCancel) {
        operator(v)
        val ack = obs.onNext(v)
        if (ack == Ack.Stop) unregister()
        ack
      } else Ack.Stop
    }

    register()

    Cancelable(() => unregister())
  }

  private def withOperator(newOperator: EV => Unit): EventObservable[EV] = new EventObservable(target, eventType, { ev => operator(ev); newOperator(ev) })

  def preventDefault: EventObservable[EV] = withOperator(_.preventDefault)
  def stopPropagation: EventObservable[EV] = withOperator(_.stopPropagation)
  def stopImmediatePropagation: EventObservable[EV] = withOperator(_.stopImmediatePropagation)

  override def unsafeSubscribeFn(subscriber: Subscriber[EV]): Cancelable = base.unsafeSubscribeFn(subscriber)
}

