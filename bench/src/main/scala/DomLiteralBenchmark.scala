package outwatch

import outwatch.dsl._

import bench._

object DomLiteralBenchmark {

  def main(@annotation.unused args: Array[String]): Unit = {
    import scala.concurrent.duration._

    bench.util.runComparison(domLiterals, List(1), 60.seconds): Unit
  }

  val domLiterals = Comparison(
    "Dom Literals",
    Seq(
      BenchmarkWithoutInit(
        "10 literal tags",
        { _ =>
          div(span(), a(), img(), hr(), button(), input(), form(), label(), b(), i())
        },
      ),
      BenchmarkWithoutInit(
        "10 literal attrs",
        { _ =>
          div(
            idAttr          := "a",
            minAttr         := "wo",
            maxAttr         := "wa",
            src             := "x",
            href            := "k",
            target          := "j",
            value           := "jo",
            contentEditable := true,
            name            := "hui",
            autoComplete    := "true",
          )
        },
      ),
    ),
  )

}
