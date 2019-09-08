package outwatch

import outwatch.dom._
import outwatch.dom.dsl._
import monix.execution.ExecutionModel.SynchronousExecution
import monix.execution.schedulers.TrampolineScheduler
import monix.execution.Scheduler
import outwatch.dom.interpreter.SnabbdomOps

import org.scalajs.dom.{ document, window }
import scala.scalajs.js
import scala.scalajs.js.annotation._
import bench._

object VNodeConstructionBenchmark extends js.JSApp {

  implicit val scheduler: Scheduler = TrampolineScheduler(Scheduler.global, SynchronousExecution)

  def main(): Unit = {
    import scala.concurrent.duration._

    bench.util.runComparison(vnodes, List(1), 60 seconds)
  }

  val vnodes = Comparison("VNode Construction", Seq(
    BenchmarkWithoutInit(
      "10 literal tags",
      { _ =>
        SnabbdomOps.toSnabbdom(div(span(), a(), img(), hr(), button(), input(), form(), label(), b(), i()))
      }
    ),
    BenchmarkWithoutInit(
      "10 literal attrs",
      { size =>
        SnabbdomOps.toSnabbdom(div(
          id := "a",
          min := "wo",
          max := "wa",
          src := "x",
          href := "k",
          target := "j",
          value := "jo",
          contentEditable := true,
          name := "hui",
          autoComplete := "true"
        ))
      }
    )
  ))

}
