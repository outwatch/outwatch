package outwatch

import scala.concurrent.Future
import cats.effect.ContextShift
import cats.effect.IO
import monix.execution.Ack.Continue
import monix.execution.ExecutionModel.SynchronousExecution
import monix.execution.schedulers.TrampolineScheduler
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import org.scalajs.dom.{document, window}
import org.scalatest.BeforeAndAfterEach
import org.scalatest._
import outwatch.Deprecated.IgnoreWarnings.initEvent
import org.scalatest.flatspec.{ AnyFlatSpec, AsyncFlatSpec }
import org.scalatest.matchers.should.Matchers

trait EasySubscribe {

  implicit class Subscriber[T](obs: Observable[T]) {
    def apply(next: T => Unit)(implicit s: Scheduler): Cancelable = obs.subscribe { t =>
      next(t)
      Continue
    }
  }
}

// TODO: We need this mock until localStorage is implemented in jsdom (https://github.com/tmpvar/jsdom/pull/2076)
trait LocalStorageMock {
  import scala.collection.mutable
  import scala.scalajs.js


  if (js.isUndefined(window.localStorage)) {
    val storageObject = new js.Object {
      private val map = new mutable.HashMap[String, String]

      def getItem(key: String): String = map.getOrElse(key, null)

      def setItem(key: String, value: String): Unit = {
        map += key -> value
      }

      def removeItem(key: String): Unit = {
        map -= key
      }

      def clear(): Unit = map.clear()
    }

    js.Dynamic.global.window.updateDynamic("localStorage")(storageObject)
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

trait OutwatchSpec extends Matchers with BeforeAndAfterEach with EasySubscribe with LocalStorageMock { self: Suite =>

  implicit val scheduler: TrampolineScheduler = TrampolineScheduler(Scheduler.global, SynchronousExecution)
  implicit val cs: ContextShift[IO] = IO.contextShift(scheduler)

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

abstract class JSDomSpec extends AnyFlatSpec with OutwatchSpec {
  implicit def executionContext = scheduler
}
abstract class JSDomAsyncSpec extends AsyncFlatSpec with OutwatchSpec {
  override def executionContext = scheduler

  implicit def ioAssertionToFutureAssertion(io: IO[Assertion]): Future[Assertion] = io.unsafeToFuture()
}
