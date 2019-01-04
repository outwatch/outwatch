package outwatch

object Main {
  def main(args: Array[String]): Unit = {
    import outwatch.dom._
    import outwatch.dom.dsl._
    import monix.execution.Scheduler.Implicits.global
    val node = org.scalajs.dom.document.body
    OutWatch.renderInto(node, h1("Hello World")).unsafeRunSync()
  }
}
