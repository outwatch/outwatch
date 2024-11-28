package outwatch

import outwatch.dsl._
import outwatch.interpreter.SnabbdomOps

import bench._

object AccumAttrHookBenchmark {

  def main(@annotation.unused args: Array[String]): Unit = {
    import scala.concurrent.duration._

    bench.util.runComparison(accumAttrs, List(1), 60.seconds): Unit
  }

  val accumAttrs = Comparison(
    "VNode Construction",
    Seq(
      BenchmarkWithoutInit(
        "10 classes without accumAttrHook",
        { _ =>
          SnabbdomOps.toSnabbdom(
            div(
              cls := "a",
              cls := "b",
              cls := "c",
              cls := "d",
              cls := "e",
              cls := "f",
              cls := "g",
              cls := "h",
              cls := "i",
              cls := "j",
            ),
            RenderConfig.default,
          )
        },
      ),
      BenchmarkWithoutInit(
        "10 classes with accumAttrHook",
        { _ =>
          SnabbdomOps.toSnabbdom(
            div(
              cls := "a",
              cls := "b",
              cls := "c",
              cls := "d",
              cls := "e",
              cls := "f",
              cls := "g",
              cls := "h",
              cls := "i",
              cls := "j",
            ),
            RenderConfig(
              _ => VMod.empty,
              (attr, other) => if (attr.title == "class") s"${attr.value}  $other" else s"${attr.value} $other",
            ),
          )
        },
      ),
    ),
  )

}
