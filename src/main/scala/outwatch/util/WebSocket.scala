package outwatch.util

import cats.effect.Effect
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom._
import outwatch.dom.VDomModifierFactory

trait WebSocketFactory[F[+_]] extends VDomModifierFactory[F] {
  implicit val effectF: Effect[F]

  object WebSocket {
    implicit def toSink(socket: WebSocket): F[Sink[String]] = socket.sink

    implicit def toSource(socket: WebSocket): Observable[MessageEvent] = socket.source
  }

  sealed case class WebSocket private(url: String)(implicit s: Scheduler) {
    val ws = new org.scalajs.dom.WebSocket(url)

    lazy val source = Observable.create[MessageEvent](Unbounded)(observer => {
      ws.onmessage = (e: MessageEvent) => observer.onNext(e)
      ws.onerror = (e: Event) => observer.onError(new Exception("WebSocket Error."))
      ws.onclose = (e: CloseEvent) => observer.onComplete()
      Cancelable(() => ws.close())
    })

    def sink: F[Sink[String]] = Sink.createFull[String](
      s => {
        effectF.delay(ws.send(s))
      },
      _ => effectF.unit,
      effectF.delay(ws.close())
    )

  }

}
