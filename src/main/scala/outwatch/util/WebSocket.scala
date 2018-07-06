package outwatch.util

import cats.effect.IO
import monix.execution.Ack.Continue
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.OverflowStrategy.Unbounded
import monix.reactive.{Observable, Observer}
import org.scalajs.dom.{CloseEvent, Event, MessageEvent}

import scala.concurrent.Future

object WebSocket {
  implicit def toObserver(socket: WebSocket): IO[Observer[String]] = socket.observer
  implicit def toObservable(socket: WebSocket): Observable[MessageEvent] = socket.observable
}

final case class WebSocket private(url: String)(implicit s: Scheduler) {
  val ws = new org.scalajs.dom.WebSocket(url)

  @deprecated("use observable instead", "")
  def source = observable
  lazy val observable:Observable[MessageEvent] = Observable.create[MessageEvent](Unbounded)(observer => {
    ws.onmessage = (e: MessageEvent) => observer.onNext(e)
    ws.onerror = (e: Event) => observer.onError(new Exception(s"Error in WebSocket: $e"))
    ws.onclose = (e: CloseEvent) => observer.onComplete()
    Cancelable(() => ws.close())
  })

  @deprecated("use observer instead", "")
  def sink = observer
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

