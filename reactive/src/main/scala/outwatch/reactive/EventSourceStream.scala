package outwatch.reactive

import org.scalajs.dom
import scala.scalajs.js

// This is a special Observable for dom events on an EventTarget (e.g. window or document).
// It adds an event listener on subscribe and removes it when the subscription is canceled.
// It honors the Ack of the subscriber and unregisters the events if an Ack.Stop is received.
// It provides convenience methods for doing sync operations on the event before emitting,
// like stopPropagation, stopImmediatePropagation, preventDefault. Because it is not
// sufficient to do them in an observable.doOnNext(_.stopPropagation()), because this
// might be async and event handling/bubbling is done sync.

final class EventSourceStream[+EV] private(target: dom.EventTarget, eventType: String, operator: EV => Unit) extends SourceStream[EV] {
  private val base: SourceStream[EV] = SourceStream.create { sink =>
    var isCancel = false

    val eventHandler: js.Function1[EV, Unit] = { v =>
      if (!isCancel) {
        operator(v)
        sink.onNext(v)
      }
    }

    def register() = target.addEventListener(eventType, eventHandler)
    def unregister() = if (!isCancel) {
      isCancel = true
      target.removeEventListener(eventType, eventHandler)
    }

    register()

    Subscription(() => unregister())
  }

  @inline private def withOperator(newOperator: EV => Unit): EventSourceStream[EV] = new EventSourceStream[EV](target, eventType, { ev => operator(ev); newOperator(ev) })

  @inline def preventDefault(implicit env: EV <:< dom.Event): EventSourceStream[EV] = withOperator(_.preventDefault)
  @inline def stopPropagation(implicit env: EV <:< dom.Event): EventSourceStream[EV] = withOperator(_.stopPropagation)
  @inline def stopImmediatePropagation(implicit env: EV <:< dom.Event): EventSourceStream[EV] = withOperator(_.stopImmediatePropagation)

  @inline def subscribe[G[_] : Sink](sink: G[_ >: EV]): Subscription = base.subscribe(sink)
}
object EventSourceStream {
  @inline def apply[EV <: dom.Event](target: dom.EventTarget, eventType: String): EventSourceStream[EV] = new EventSourceStream[EV](target, eventType, _ => ())
}
