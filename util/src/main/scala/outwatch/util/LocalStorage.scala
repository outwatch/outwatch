package outwatch.util

import org.scalajs.dom
import org.scalajs.dom.StorageEvent
import org.scalajs.dom.window.{localStorage, sessionStorage}

import outwatch.dsl.events
import colibri._

class Storage(storage: dom.Storage) {
  private def storageEventsForKey(key: String): Observable[Option[String]] =
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

  private def storageSubject(key: String, withEvents: Boolean): Subject[Option[String]] = {

    val storageWriter: Option[String] => Unit = {
      case Some(data) => storage.setItem(key, data)
      case None => storage.removeItem(key)
    }

    val eventListener =
      if (withEvents) storageEventsForKey(key)
      else Observable.empty

   Subject.publish[Option[String]]()
     .transformSubject(_.tap(storageWriter))(_.merge(eventListener).prependDelay(Option(storage.getItem(key))).distinctOnEquals)
  }

  def subject(key: String): Subject[Option[String]] = storageSubject(key, withEvents = false)

  def subjectWithEvents(key: String): Subject[Option[String]] = storageSubject(key, withEvents = true)
}

object LocalStorage extends Storage(localStorage)
object SessionStorage extends Storage(sessionStorage)
