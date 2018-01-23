package outwatch.util

import cats.effect.IO
import monix.execution.Ack.Continue
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom.{CloseEvent, ErrorEvent, MessageEvent}
import outwatch.Sink
import outwatch.dom.Observable

object WebSocket {
  implicit def toSink(socket: WebSocket): Sink[String] = socket.sink
  implicit def toSource(socket: WebSocket): Observable[MessageEvent] = socket.source
}

final case class WebSocket private(url: String)(implicit s: Scheduler) {
  val ws = new org.scalajs.dom.WebSocket(url)

  lazy val source = Observable.create[MessageEvent](Unbounded)(observer => {
    ws.onmessage = (e: MessageEvent) => observer.onNext(e)
    ws.onerror = (e: ErrorEvent) => observer.onError(new Exception(e.message))
    ws.onclose = (e: CloseEvent) => observer.onComplete()
    Cancelable(() => ws.close())
  })

  lazy val sink = Sink.create[String](
    s => IO {
      ws.send(s)
      Continue
    },
    _ => IO.pure(()),
    () => IO(ws.close())
  )

}

