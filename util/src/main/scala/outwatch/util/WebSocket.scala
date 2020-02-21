package outwatch.util

import cats.effect.IO
import outwatch.dom.helpers._
import colibri._
import org.scalajs.dom.{Event, MessageEvent}

object WebSocket {
  implicit def toObserver(socket: WebSocket): IO[Observer[String]] = socket.observer
  implicit def toObservable(socket: WebSocket): Observable[MessageEvent] = socket.observable
}

final case class WebSocket private(url: String) {
  val ws = new org.scalajs.dom.WebSocket(url)

  lazy val observable:Observable[MessageEvent] = Observable.create[MessageEvent](observer => {
    ws.onmessage = (e: MessageEvent) => observer.onNext(e)
    ws.onerror = (e: Event) => observer.onError(new Exception(s"Error in WebSocket: $e"))
    Cancelable(() => ws.close())
  })

  lazy val observer:IO[Observer[String]] = {
    IO {
      new Observer[String] {
        override def onNext(elem: String): Unit = ws.send(elem)
        override def onError(ex: Throwable): Unit = OutwatchTracing.errorSubject.onNext(ex)
      }
    }
  }
}

