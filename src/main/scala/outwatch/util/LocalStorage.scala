package outwatch.util

import cats.effect.Effect
import cats.implicits._
import monix.execution.Scheduler
import org.scalajs.dom
import org.scalajs.dom.StorageEvent
import org.scalajs.dom.window.{localStorage, sessionStorage}
import outwatch.OutwatchOps
import outwatch.dom.OutwatchDsl

trait StorageFactory[F[+_]] {
  implicit val effectF:Effect[F]

  object LocalStorage extends Storage(localStorage)
  object SessionStorage extends Storage(sessionStorage)
}

class Storage[F[+_]](domStorage: dom.Storage)(implicit val effectF:Effect[F]) extends OutwatchOps[F] with OutwatchDsl[F] {
  private def handlerWithTransform(key: String, transform: Observable[Option[String]] => Observable[Option[String]])(implicit scheduler: Scheduler) = {
    val storage = new dom.ext.Storage(domStorage)

    for {
      h <- Handler.create[Option[String]](storage(key))
    } yield {
      // We execute the write-action to the storage
      // and pass the written value through to the underlying handler h
      h.transformHandler(o => transform(o).distinctUntilChanged) { input =>
        input.foreach {
          case Some(data) => storage.update(key, data)
          case None => storage.remove(key)
        }
        input
      }
    }
  }

  private def storageEventsForKey(key: String)(implicit scheduler: Scheduler): Observable[Option[String]] =
    // StorageEvents are only fired if the localStorage was changed in another window
    dsl.events.window.onStorage.collect {
      case e: StorageEvent if e.storageArea == domStorage && e.key == key =>
        // newValue is either String or null if removed or cleared
        // Option() transformes this to Some(string) or None
        Option(e.newValue)
      case e: StorageEvent if e.storageArea == domStorage && e.key == null =>
        // storage.clear() emits an event with key == null
        None
    }

  def handlerWithoutEvents(key: String)(implicit scheduler: Scheduler): F[Handler[Option[String]]] = {
    handlerWithTransform(key, identity)
  }

  def handlerWithEventsOnly(key: String)(implicit scheduler: Scheduler): F[Handler[Option[String]]] = {
    val storageEvents = storageEventsForKey(key)
    handlerWithTransform(key, _ => storageEvents)
  }

  def handler(key: String)(implicit scheduler: Scheduler): F[Handler[Option[String]]] = {
    val storageEvents = storageEventsForKey(key)
    handlerWithTransform(key, Observable.merge(_, storageEvents))
  }
}
