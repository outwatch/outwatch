package outwatch

import cats.effect.{unsafe, IO}
import org.scalajs.dom.EventInit
import org.scalajs.dom.{document, window, Event}
import org.scalatest.{BeforeAndAfterEach, _}
import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

trait LocalStorageMock {
  import scala.scalajs.js

  def dispatchStorageEvent(key: String, newValue: String, oldValue: String): Unit = {
    if (key == null) window.localStorage.clear()
    else window.localStorage.setItem(key, newValue)

    val event = new Event(
      "storage",
      new EventInit {
        bubbles = true
        cancelable = false
      },
    )
    event.asInstanceOf[js.Dynamic].key = key
    event.asInstanceOf[js.Dynamic].newValue = newValue
    event.asInstanceOf[js.Dynamic].oldValue = oldValue
    event.asInstanceOf[js.Dynamic].storageArea = window.localStorage
    window.dispatchEvent(event)
    ()
  }
}

trait OutwatchSpec extends Matchers with BeforeAndAfterEach with LocalStorageMock { self: Suite =>
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

abstract class JSDomSpec      extends AnyFlatSpec with OutwatchSpec
abstract class JSDomAsyncSpec extends AsyncFlatSpec with OutwatchSpec {
  // This deadlocks somehow
  // implicit private val ioRuntime: unsafe.IORuntime = unsafe.IORuntime.global

  // ExecutionContext.parasitic only exists in scala 2.13. Not 2.12.
  override val executionContext = scala.scalajs.concurrent.QueueExecutionContext()
  // override final def execute(runnable: Runnable): Unit = runnable.run()
  // override final def reportFailure(t: Throwable): Unit = ExecutionContext.defaultReporter(t)
  // }

  implicit val ioRuntime: unsafe.IORuntime = unsafe.IORuntime(
    compute = executionContext,
    blocking = executionContext,
    config = unsafe.IORuntimeConfig(),
    scheduler = unsafe.IORuntime.defaultScheduler,
    shutdown = () => (),
  )

  implicit def ioAssertionToFutureAssertion(io: IO[Assertion]): Future[Assertion] = io.unsafeToFuture()
}
