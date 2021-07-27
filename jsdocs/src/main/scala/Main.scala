import cats.effect.IO
import outwatch._
import outwatch.dsl._

object Main {
  def main(args:Array[String]) = {
    // entrypoint needed for mdoc, so that the facades
    // for snabbdom don't get removed by dead code elimination
    val app = OutWatch.renderInto[IO]("#app", div())
  }
}

