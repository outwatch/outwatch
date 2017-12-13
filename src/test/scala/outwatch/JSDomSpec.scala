package outwatch


import monix.execution.Ack.Continue
import monix.execution.{Cancelable, ExecutionModel, Scheduler}
import monix.reactive.Observable
import org.scalajs.dom._
import org.scalatest.BeforeAndAfterEach



trait EasySubscribe {
  import monix.execution.Scheduler.Implicits.global

  implicit class Subscriber[T](obs: Observable[T]) {
    def apply(next: T => Unit): Cancelable = obs.subscribe { t =>
      next(t)
      Continue
    }
  }

}


abstract class JSDomSpec extends UnitSpec with BeforeAndAfterEach with EasySubscribe {

  override implicit val executionContext: Scheduler =
    Scheduler.Implicits.global.withExecutionModel(ExecutionModel.SynchronousExecution)

  override def beforeEach(): Unit = {
    document.body.innerHTML = ""

    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
    ()
  }
}
