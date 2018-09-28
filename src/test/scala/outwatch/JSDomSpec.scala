package outwatch


import monix.execution.Ack.Continue
import monix.execution.ExecutionModel.SynchronousExecution
import monix.execution.schedulers.TrampolineScheduler
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import org.scalajs.dom.{Event, document, window}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import outwatch.Deprecated.IgnoreWarnings.initEvent
import snabbdom.{DataObject, Hooks}
import snabbdom.DataObject.{AttrValue, PropValue, StyleValue}

import scala.scalajs.js


trait EasySubscribe {

  implicit class Subscriber[T](obs: Observable[T]) {
    def apply(next: T => Unit)(implicit s: Scheduler): Cancelable = obs.subscribe { t =>
      next(t)
      Continue
    }
  }
}

trait SnabbdomTestHelper {
  def createDataObject(attrs: js.UndefOr[js.Dictionary[AttrValue]] = js.Dictionary[AttrValue](),
                       on: js.UndefOr[js.Dictionary[js.Function1[Event, Unit]]] = js.Dictionary[js.Function1[Event, Unit]](),
                      ): DataObject = DataObject(attrs, js.Dictionary[PropValue](), js.Dictionary[StyleValue](), js.Dictionary[js.Function1[Event, Unit]](), Hooks(js.undefined, js.undefined, js.undefined, js.undefined, js.undefined), js.undefined)

}

// TODO: We need this mock until localStorage is implemented in jsdom (https://github.com/tmpvar/jsdom/pull/2076)
trait LocalStorageMock {
  import scala.collection.mutable
  import scala.scalajs.js


  if (js.isUndefined(window.localStorage)) {
    js.Dynamic.global.window.updateDynamic("localStorage")(new js.Object {
      private val map = new mutable.HashMap[String, String]

      @SuppressWarnings(Array("unused"))
      def getItem(key: String): String = map.getOrElse(key, null)

      @SuppressWarnings(Array("unused"))
      def setItem(key: String, value: String): Unit = {
        map += key -> value
      }

      @SuppressWarnings(Array("unused"))
      def removeItem(key: String): Unit = {
        map -= key
      }

      @SuppressWarnings(Array("unused"))
      def clear(): Unit = map.clear()
    })
  }

  def dispatchStorageEvent(key: String, newValue: String, oldValue: String): Unit = {
    if (key == null) window.localStorage.clear()
    else window.localStorage.setItem(key, newValue)

    val event = document.createEvent("Events")
    initEvent(event)("storage", canBubbleArg = true, cancelableArg = false)
    event.asInstanceOf[js.Dynamic].key = key
    event.asInstanceOf[js.Dynamic].newValue = newValue
    event.asInstanceOf[js.Dynamic].oldValue = oldValue
    event.asInstanceOf[js.Dynamic].storageArea = window.localStorage
    window.dispatchEvent(event)
    ()
  }
}

abstract class JSDomSpec extends FlatSpec with Matchers with BeforeAndAfterEach with EasySubscribe with LocalStorageMock with SnabbdomTestHelper {

  implicit val scheduler = TrampolineScheduler(Scheduler.global, SynchronousExecution)

  override def beforeEach(): Unit = {

    document.body.innerHTML = ""

    window.localStorage.clear()

    // prepare body with <div id="app"></div>
    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
    ()
  }
}
