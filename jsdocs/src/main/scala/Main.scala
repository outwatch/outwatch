import cats.effect.IO
import outwatch._
import outwatch.dsl._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("js-beautify", "html")
object JSBeautify extends js.Object

object Main {
  def main(args: Array[String]) = {
    // entrypoint needed for mdoc, so that the facades
    // for snabbdom, jsBeautify don't get removed by dead code elimination.
    // It is never executed.
    JSBeautify
    val _ = Outwatch.renderInto[IO]("#app", div())
  }
}
