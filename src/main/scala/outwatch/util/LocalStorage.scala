package outwatch.util

import cats.effect.IO
import cats.implicits._
import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalajs.dom
import org.scalajs.dom.StorageEvent
import org.scalajs.dom.window.{localStorage, sessionStorage}
import outwatch._
import outwatch.dom.dsl.events

class Storage(domStorage: dom.Storage) {
  private def subjectWithTransform(key: String, transform: Observable[Option[String]] => Observable[Option[String]])(implicit scheduler: Scheduler):IO[Handler[Option[String]]] = {
    val storage = new dom.ext.Storage(domStorage)

    for {
      h <- Handler.create[Option[String]](storage(key))
    } yield {
      // We execute the write-action to the storage
      // and pass the written value through to the underlying subject h
      h.transformHandler { input =>
        input.foreach {
          case Some(data) => storage.update(key, data)
          case None => storage.remove(key)
        }
        input
      }(o => transform(o).distinctUntilChanged)
    }
  }

  private def storageEventsForKey(key: String)(implicit scheduler: Scheduler): Observable[Option[String]] =
    // StorageEvents are only fired if the localStorage was changed in another window
    events.window.onStorage.collect {
      case e: StorageEvent if e.storageArea == domStorage && e.key == key =>
        // newValue is either String or null if removed or cleared
        // Option() transformes this to Some(string) or None
        Option(e.newValue)
      case e: StorageEvent if e.storageArea == domStorage && e.key == null =>
        // storage.clear() emits an event with key == null
        None
    }

  def handlerWithoutEvents(key: String)(implicit scheduler: Scheduler): IO[Handler[Option[String]]] = {
    subjectWithTransform(key, identity)
  }

  def handlerWithEventsOnly(key: String)(implicit scheduler: Scheduler): IO[Handler[Option[String]]] = {
    val storageEvents = storageEventsForKey(key)
    subjectWithTransform(key, o => storageEvents)
  }

  def handler(key: String)(implicit scheduler: Scheduler): IO[Handler[Option[String]]] = {
    val storageEvents = storageEventsForKey(key)
    subjectWithTransform(key, Observable.merge(_, storageEvents))
  }
}

object LocalStorage extends Storage(localStorage)
object SessionStorage extends Storage(sessionStorage)
