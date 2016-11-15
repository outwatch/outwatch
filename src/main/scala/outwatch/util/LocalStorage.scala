package outwatch.util

import outwatch.Sink
import rxscalajs.Observable
import org.scalajs.dom.window.localStorage


object LocalStorageReader {
  def apply(key: String): Observable[String] = {
    Observable.create[String](observer => observer.next(localStorage.getItem(key)))
  }
}

object LocalStorageWriter {
  def apply(key: String): Sink[String] = {
    outwatch.Sink.createSink[String](data => localStorage.setItem(key, data))
  }
}
