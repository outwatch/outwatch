package outwatch

import org.scalajs.dom.document
import outwatch.dom._
import outwatch.dom.dsl._

import scala.scalajs.js

class PerfTest extends JSDomSpec {

  "Perf" should "be" in {
    (0 to 10) foreach { _ =>
      val elemId = "msg"

      val handler = Handler.create[Int](0).unsafeRunSync
      val handler2 = Handler.create[Int](0).unsafeRunSync
      val handler3 = Handler.create[Int].unsafeRunSync

      val vtree = div(
        id := elemId,
        span(id := "pete", "Go!"),
        onClick handleWith {},
        onDomMount handleWith {},
        onDomUnmount handleWith {},
        //      dsl.cls <-- handler.map(_.toString),
        //      dsl.value <-- handler.map(_.toString),
        handler.map { i =>
          (0 to i).map { j =>
            input(tpe := "text", dsl.defaultValue := j.toString, styleAttr := "background:black;")
          }
          //        input(tpe := "text", dsl.defaultValue := i.toString, styleAttr := "background:black;")
        },
        handler2.map { i =>
          (0 to i).map { j =>
            div("hans", cls := j.toString, onClick handleWith {}, handler3)
          }
        }
      )

      val node = document.createElement("div")
      document.body.appendChild(node)

      val t = System.nanoTime()

      OutWatch.renderInto(node, vtree).unsafeRunSync()

      (0 to 100).foreach { i =>
        handler.onNext(i)
        handler2.onNext(i)
      }


      val t2 = System.nanoTime()

      println(node.innerHTML)

      println("TOOK =====> " + (t2 - t))
    }
  }

  it should "cmd" in {
    (0 to 10) foreach { _ =>

    val elemId = "msg"

    val handler3 = Handler.create[Int].unsafeRunSync

    def node1(j: Int) = input(tpe := "text", dsl.defaultValue := j.toString, styleAttr := "background:black;")
    def node2(j: Int) = div("hans", cls := j.toString, onClick handleWith {}, handler3)

    val handler = Handler.create[ChildCommand](ChildCommand.ReplaceAll(js.Array(node1(0)))).unsafeRunSync
    val handler2 = Handler.create[ChildCommand](ChildCommand.ReplaceAll(js.Array(node2(0)))).unsafeRunSync

    val vtree = div(
      id := elemId,
      span(id :=  "pete", "Go!"),
      onClick handleWith   {},
      onDomMount handleWith   {},
      onDomUnmount handleWith   {},
//      dsl.cls <-- handler.map(_.toString),
//      dsl.value <-- handler.map(_.toString),
      handler,
      handler2
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    val t = System.nanoTime()

    OutWatch.renderInto(node, vtree).unsafeRunSync()

    var node1Counter = 0
    var node2Counter = 0
    (0 to 100).foreach { i =>
      handler.onNext(ChildCommand.Append(node1(node1Counter)))
      handler2.onNext(ChildCommand.Append(node2(node2Counter)))

      node1Counter += 1
      node2Counter += 1
    }



    val t2 = System.nanoTime()

    println(node.innerHTML)

    println("TOOK =====> " + (t2 - t))
    }
  }
}
