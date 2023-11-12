package outwatch

import outwatch._
import outwatch.dsl._
import outwatch.interpreter.SnabbdomOps

import org.scalajs.dom.{document, window}
import scala.scalajs.js
import scala.scalajs.js.annotation._
import bench._

object VNodeConstructionBenchmark {

  def main(args: Array[String]): Unit = {
    import scala.concurrent.duration._

    bench.util.runComparison(vnodes, List(1), 60.seconds)
  }

  val vnodes = Comparison(
    "VNode Construction",
    Seq(
      BenchmarkWithoutInit(
        "10 literal tags",
        { _ =>
          SnabbdomOps.toSnabbdom(
            div(span(), a(), img(), hr(), button(), input(), form(), label(), b(), i()),
            RenderConfig.default,
          )
        },
      ),
      BenchmarkWithoutInit(
        "10 literal attrs",
        { size =>
          SnabbdomOps.toSnabbdom(
            div(
              idAttr          := "a",
              min             := "wo",
              max             := "wa",
              src             := "x",
              href            := "k",
              target          := "j",
              value           := "jo",
              contentEditable := true,
              name            := "hui",
              autoComplete    := "true",
            ),
            RenderConfig.default,
          )
        },
      ),
    ),
  )

}
