package outwatch.util

import cats.effect.IO
import org.scalajs.dom.{CloseEvent, ErrorEvent, MessageEvent}
import outwatch.Sink
import rxscalajs.Observable

object WebSocket {
  implicit def toSink(socket: WebSocket): Sink[String] = socket.sink
  implicit def toSource(socket: WebSocket): Observable[MessageEvent] = socket.source
}

final case class WebSocket private(url: String) {
  val ws = new org.scalajs.dom.WebSocket(url)

  lazy val source = Observable.create[MessageEvent](observer => {
    ws.onmessage = (e: MessageEvent) => observer.next(e)
    ws.onerror = (e: ErrorEvent) => observer.error(e)
    ws.onclose = (e: CloseEvent) => observer.complete()
    () => ws.close()
  })

  lazy val sink = Sink.create[String](s => IO(ws.send(s)), _ => IO.pure(()), () => IO(ws.close()))

}

