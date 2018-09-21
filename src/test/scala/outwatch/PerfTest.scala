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
      span(id :=  "pete", "Go!"),
      onClick handleWith   {},
      onDomMount handleWith   {},
      onDomUnmount handleWith   {},
      dsl.cls <-- handler.map(_.toString),
      dsl.value <-- handler.map(_.toString),
      handler.map { i =>
        (0 to i).map { j =>
          input(tpe := "text", dsl.defaultValue := j.toString, styleAttr := "background:black;")
        }
//        input(tpe := "text", dsl.defaultValue := i.toString, styleAttr := "background:black;")
      },
      handler2.map { i =>
        (0 to i).map { j =>
          div(i, cls := j.toString, onClick handleWith {}, handler3)
        }
//        div(i, cls := i.toString, onClick --> {}, handler3)
      }
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    val t = System.nanoTime()

    OutWatch.renderInto(node, vtree).unsafeRunSync()

//     node.innerHTML shouldBe """<div id="msg"><span id="pete">Go!</span><input type="text" value="0" style="background:black;"><div class="0">00</div></div>"""

     (0 to 100).foreach { i =>
      handler.onNext(i)
      handler2.onNext(i)
     }


    val t2 = System.nanoTime()

//    node.innerHTML shouldBe """<div id="msg"><span id="pete">Go!</span><input type="text" value="1000000" style="background:black;"><div class="1000000">10000000</div></div>"""
  println(node.innerHTML)

    (t2 - t) shouldBe 1
  }
}
