package outwatch.ext.monix.util

import cats.effect.IO
import monix.execution.Ack.Continue
import monix.execution.{Ack, Cancelable}
import monix.reactive.OverflowStrategy.Unbounded
import monix.reactive.{Observable, Observer}
import org.scalajs.dom.{CloseEvent, Event, MessageEvent}

import scala.concurrent.Future

object WebSocket {
  implicit def toObserver(socket: WebSocket): IO[Observer[String]] = socket.observer
  implicit def toObservable(socket: WebSocket): Observable[MessageEvent] = socket.observable
}

final case class WebSocket private(url: String) {
  val ws = new org.scalajs.dom.WebSocket(url)

  lazy val observable:Observable[MessageEvent] = Observable.create[MessageEvent](Unbounded)(observer => {
    ws.onmessage = (e: MessageEvent) => observer.onNext(e)
    ws.onerror = (e: Event) => observer.onError(new Exception(s"Error in WebSocket: $e"))
    ws.onclose = (_: CloseEvent) => observer.onComplete()
    Cancelable(() => ws.close())
  })

  lazy val observer:IO[Observer[String]] = {
    IO {
      new Observer[String] {
        override def onNext(elem: String): Future[Ack] = { ws.send(elem); Continue }
        override def onError(ex: Throwable): Unit = throw ex
        override def onComplete(): Unit = ws.close()
      }
    }
  }
}

