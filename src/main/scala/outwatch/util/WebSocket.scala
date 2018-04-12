package outwatch.util

import cats.effect.Effect
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.OverflowStrategy.Unbounded
import org.scalajs.dom._
import outwatch.{Sink, SinkFactory}
import outwatch.dom.Observable

object WebSocket {
  implicit def toSink[F[+_]: Effect](socket: WebSocket[F]): F[Sink[F,String]] = socket.sink
  implicit def toSource[F[+_]](socket: WebSocket[F]): Observable[MessageEvent] = socket.source
}

final case class WebSocket[F[+_]] private(url: String)(implicit val effectF: Effect[F], s: Scheduler) extends SinkFactory[F] {
  val ws = new org.scalajs.dom.WebSocket(url)

  lazy val source = Observable.create[MessageEvent](Unbounded)(observer => {
    ws.onmessage = (e: MessageEvent) => observer.onNext(e)
    ws.onerror = (e: Event) => observer.onError(new Exception("WebSocket Error."))
    ws.onclose = (e: CloseEvent) => observer.onComplete()
    Cancelable(() => ws.close())
  })

  def sink: F[Sink[F,String]] = Sink.createFull[String](
    s => {
      effectF.delay(ws.send(s))
    },
    _ => effectF.unit,
    effectF.delay(ws.close())
  )

}

