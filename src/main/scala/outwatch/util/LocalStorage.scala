package outwatch.util

import cats.effect.IO
import monix.execution.Ack.Continue
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.OverflowStrategy.Unbounded
import outwatch.Sink
import org.scalajs.dom.window.localStorage
import outwatch.dom.Observable


object LocalStorageReader {
  def apply(key: String): Observable[String] = {
    Observable.create[String](Unbounded){ observer =>
      observer.onNext(localStorage.getItem(key))
      Cancelable.empty
    }
  }
}

object LocalStorageWriter {
  def apply(key: String)(implicit s: Scheduler): Sink[String] = {
    Sink.create[String](
      data => IO {
        localStorage.setItem(key, data)
        Continue
      }
    )
  }
}
