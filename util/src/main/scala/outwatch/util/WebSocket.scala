package outwatch.util

import cats.effect.IO
import outwatch.helpers._
import colibri._
import org.scalajs.dom.{Event, MessageEvent}

object WebSocket {
  implicit def toObserver(socket: WebSocket): IO[Observer[String]] = socket.observer
  implicit def toObservable(socket: WebSocket): Observable[MessageEvent] = socket.observable
}

final case class WebSocket private(url: String) {
  val ws = new org.scalajs.dom.WebSocket(url)

  lazy val observable:Observable[MessageEvent] = Observable.create[MessageEvent](observer => {
    ws.onmessage = (e: MessageEvent) => observer.unsafeOnNext(e)
    ws.onerror = (e: Event) => observer.unsafeOnError(new Exception(s"Error in WebSocket: $e"))
    Cancelable(() => ws.close())
  })

  lazy val observer:IO[Observer[String]] = {
    IO {
      new Observer[String] {
        override def unsafeOnNext(elem: String): Unit = ws.send(elem)
        override def unsafeOnError(ex: Throwable): Unit = OutwatchTracing.errorSubject.unsafeOnNext(ex)
      }
    }
  }
}

