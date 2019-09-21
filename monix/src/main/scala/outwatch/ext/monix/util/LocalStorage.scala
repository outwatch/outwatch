package outwatch.ext.monix.util

import cats.effect.Sync
import cats.implicits._
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalajs.dom
import org.scalajs.dom.StorageEvent
import org.scalajs.dom.window.{localStorage, sessionStorage}

import outwatch.dom.dsl.events
import outwatch.ext.monix._, handler._

class Storage(domStorage: dom.Storage) {
  private def subjectWithTransform[F[_]: Sync](key: String, transform: Observable[Option[String]] => Observable[Option[String]])(implicit scheduler: Scheduler): F[Handler[Option[String]]] = {
    val storage = new dom.ext.Storage(domStorage)

    for {
      h <- Handler.createF[F](storage(key))
    } yield {
      // We execute the write-action to the storage
      // and pass the written value through to the underlying subject h
      val connectable = h.transformHandler[Option[String]](o => transform(o).distinctUntilChanged) { input =>
        input.doOnNext {
          case Some(data) => Task(storage.update(key, data))
          case None => Task(storage.remove(key))
        }
      }

      connectable.connect()

      connectable
    }
  }

  private def storageEventsForKey[F[_]: Sync](key: String): Observable[Option[String]] =
    // StorageEvents are only fired if the localStorage was changed in another window
    events.window.onStorage.liftSource[Observable].collect {
      case e: StorageEvent if e.storageArea == domStorage && e.key == key =>
        // newValue is either String or null if removed or cleared
        // Option() transformes this to Some(string) or None
        Option(e.newValue)
      case e: StorageEvent if e.storageArea == domStorage && e.key == null =>
        // storage.clear() emits an event with key == null
        None
    }

  def handlerWithoutEvents[F[_]: Sync](key: String)(implicit scheduler: Scheduler): F[Handler[Option[String]]] = {
    subjectWithTransform(key, identity)
  }

  def handlerWithEventsOnly[F[_]: Sync](key: String)(implicit scheduler: Scheduler): F[Handler[Option[String]]] = {
    val storageEvents = storageEventsForKey(key)
    subjectWithTransform(key, _ => storageEvents)
  }

  def handler[F[_]: Sync](key: String)(implicit scheduler: Scheduler): F[Handler[Option[String]]] = {
    val storageEvents = storageEventsForKey(key)
    subjectWithTransform(key, Observable(_, storageEvents).merge)
  }
}

object LocalStorage extends Storage(localStorage)
object SessionStorage extends Storage(sessionStorage)
