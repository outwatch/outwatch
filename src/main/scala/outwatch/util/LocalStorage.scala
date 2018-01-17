package outwatch.util

import cats.effect.IO
import monix.execution.Scheduler
import org.scalajs.dom
import org.scalajs.dom.StorageEvent
import org.scalajs.dom.window.localStorage
import org.scalajs.dom.window.sessionStorage
import outwatch.dom.Observable
import outwatch.dom.dsl.events
import cats.implicits._
import outwatch.Handler

object Storage {
  def handler(domStorage: dom.Storage)(key: String)(implicit scheduler: Scheduler): IO[Handler[Option[String]]] = {
    // StorageEvents are only fired if the localStorage was changed in another window
    val storageEvents: Observable[Option[String]] = events.window.onStorage
      .collect {
        case e: StorageEvent if e.storageArea == domStorage && e.key == key =>
          // newValue is either String or null if removed or cleared
          // Option() transformes this to Some(string) or None
          Option(e.newValue.asInstanceOf[String]) //TODO: https://github.com/scala-js/scala-js-dom/pull/308
        case e: StorageEvent if e.storageArea == domStorage && e.key == null =>
          // storage.clear() emits an event with key == null
          None
      }
    val storage = new dom.ext.Storage(domStorage)

    for {
      h <- Handler.create[Option[String]](storage(key))
    } yield {
      // We execute the write-action to the storage
      // and pass the written value through to the underlying handler h
      h.transformHandler(Observable.merge(_, storageEvents).distinctUntilChanged) { input =>
        input.foreach {
          case Some(data) => storage.update(key, data)
          case None => storage.remove(key)
        }
        input
      }
    }
  }
}

object LocalStorage {
  def handler(key: String)(implicit scheduler: Scheduler): IO[Handler[Option[String]]] =
    Storage.handler(localStorage)(key)(scheduler)
}

object SessionStorage {
  def handler(key: String)(implicit scheduler: Scheduler): IO[Handler[Option[String]]] =
    Storage.handler(sessionStorage)(key)(scheduler)
}

