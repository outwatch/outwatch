package outwatch.util

import cats.effect.Sync
import cats.implicits._
import org.scalajs.dom
import org.scalajs.dom.StorageEvent
import org.scalajs.dom.window.{localStorage, sessionStorage}

import outwatch.dsl.events
import outwatch.reactive.handler._
import colibri._

class Storage(storage: dom.Storage) {
  @deprecated
  private def handlerWithTransform[F[_]: Sync](key: String, transform: Observable[Option[String]] => Observable[Option[String]]): F[Handler[Option[String]]] = {

    for {
      h <- Handler.createF[F](Option(storage.getItem(key)))
    } yield {
      // We execute the write-action to the storage
      // and pass the written value through to the underlying subject h
      h.transformSubject[Option[String]] { o =>
        val c = o.redirect((o: Observable[Option[String]]) => transform(o).distinct)
        c.connect()
        c.value
      } { input =>
        input.doOnNext {
          case Some(data) => storage.setItem(key, data)
          case None => storage.removeItem(key)
        }
      }
    }
  }

  private def storageEventsForKey[F[_]](key: String): Observable[Option[String]] =
    // StorageEvents are only fired if the localStorage was changed in another window
    events.window.onStorage.collect {
      case e: StorageEvent if e.storageArea == storage && e.key == key =>
        // newValue is either String or null if removed or cleared
        // Option() transformes this to Some(string) or None
        Option(e.newValue)
      case e: StorageEvent if e.storageArea == storage && e.key == null =>
        // storage.clear() emits an event with key == null
        None
    }

  @deprecated
  def handlerWithoutEvents[F[_]: Sync](key: String): F[Handler[Option[String]]] = {
    handlerWithTransform(key, identity)
  }

  @deprecated
  def handlerWithEventsOnly[F[_]: Sync](key: String): F[Handler[Option[String]]] = {
    val storageEvents = storageEventsForKey(key)
    handlerWithTransform(key, _ => storageEvents)
  }

  @deprecated
  def handler[F[_]: Sync](key: String): F[Handler[Option[String]]] = {
    val storageEvents = storageEventsForKey(key)
    handlerWithTransform(key, Observable.merge(_, storageEvents))
  }
}

object LocalStorage extends Storage(localStorage)
object SessionStorage extends Storage(sessionStorage)
