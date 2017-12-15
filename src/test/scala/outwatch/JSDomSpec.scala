package outwatch


import monix.execution.Ack.Continue
import monix.execution.ExecutionModel.SynchronousExecution
import monix.execution.schedulers.TrampolineScheduler
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import org.scalajs.dom._
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}


trait EasySubscribe {

  implicit class Subscriber[T](obs: Observable[T]) {
    def apply(next: T => Unit)(implicit s: Scheduler): Cancelable = obs.subscribe { t =>
      next(t)
      Continue
    }
  }
}


abstract class JSDomSpec extends FlatSpec with Matchers with BeforeAndAfterEach with EasySubscribe {

  implicit val scheduler = Scheduler.global
  val trampolineScheduler = TrampolineScheduler(scheduler, SynchronousExecution)

  override def beforeEach(): Unit = {
    document.body.innerHTML = ""

    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
    ()
  }
}
