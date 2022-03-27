package outwatch

import outwatch._
import outwatch.dsl._

import org.scalajs.dom.{ document, window }
import scala.scalajs.js
import scala.scalajs.js.annotation._
import bench._

object DomLiteralBenchmark {

  def main(args: Array[String]): Unit = {
    import scala.concurrent.duration._

    bench.util.runComparison(domLiterals, List(1), 60.seconds)
  }

  val domLiterals = Comparison("Dom Literals", Seq(
    BenchmarkWithoutInit(
      "10 literal tags",
      { _ =>
        div(span(), a(), img(), hr(), button(), input(), form(), label(), b(), i())
      }
    ),
    BenchmarkWithoutInit(
      "10 literal attrs",
      { size =>
        div(
          idAttr := "a",
          min := "wo",
          max := "wa",
          src := "x",
          href := "k",
          target := "j",
          value := "jo",
          contentEditable := true,
          name := "hui",
          autoComplete := "true"
        )
      }
    )
  ))

}
