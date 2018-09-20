package outwatch

import org.scalajs.dom.document
import outwatch.dom._
import outwatch.dom.dsl._

class PerfTest extends JSDomSpec {

  "Perf" should "be" in {
    val elemId = "msg"

    val handler = Handler.create[Int](0).unsafeRunSync
    val handler2 = Handler.create[Int](0).unsafeRunSync
    val handler3 = Handler.create[Int](0).unsafeRunSync

    val vtree = div(
      id := elemId,
      span("Go!"),
//      onClick handleWith {},
//      onDomMount handleWith {},
//      onDomUnmount handleWith {},
      handler.map { i =>
        input(tpe := "text", dsl.value := i.toString, styleAttr := "background:black;")
      },
      handler2.map { i =>
        div(dsl.key := "2", i, cls := i.toString, onClick handleWith {}, handler3)
      }
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    val t = System.nanoTime()

    OutWatch.renderInto(node, vtree).unsafeRunSync()

    (0 to 1000000).foreach { i =>
      handler.onNext(i)
      handler2.onNext(i)
    }

    val t2 = System.nanoTime()

    (t2 - t) shouldBe 1
  }
}
