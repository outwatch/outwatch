package outwatch.util

import cats.effect.Effect
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.Observable

object WebSocket {
  implicit def toSink[F[+_]: Effect](socket: WebSocket): F[Sink[String]] = socket.sink[F]
  implicit def toSource(socket: WebSocket): Observable[MessageEvent] = socket.source
}

final case class WebSocket private(url: String)(implicit s: Scheduler) {
  val ws = new org.scalajs.dom.WebSocket(url)

  lazy val source = Observable.create[MessageEvent](Unbounded)(observer => {
    ws.onmessage = (e: MessageEvent) => observer.onNext(e)
    ws.onerror = (e: Event) => observer.onError(new Exception("WebSocket Error."))
    ws.onclose = (e: CloseEvent) => observer.onComplete()
    Cancelable(() => ws.close())
  })

  def sink[F[+_]: Effect]: F[Sink[String]] = Sink.createFull[F, String](
    s => {
      Effect[F].delay(ws.send(s))
    },
    _ => Effect[F].unit,
    Effect[F].delay(ws.close())
  )

}

