package outwatch

import monix.execution.Scheduler.Implicits.global
import outwatch.dom._
import outwatch.dom.dsl._

import org.scalajs.dom.{document, window}
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSImport("jsdom", JSImport.Namespace)
object jsdom extends js.Object {
  def jsdom(innerHTML: js.UndefOr[String]): js.Any = js.native
}

object Performance {

  def main(args: Array[String]): Unit = {
    setupJsDom()

    beforeEach()
    runChildren()

    beforeEach()
    runThunks()

    beforeEach()
    runCommands()
  }

  val numIterations = 100

  def setupJsDom(): Unit = {
    // see https://airbnb.io/enzyme/docs/guides/jsdom.html

    val jdom = jsdom.jsdom("")
    js.Dynamic.global.document = jdom
    js.Dynamic.global.window = jdom.asInstanceOf[js.Dynamic].defaultView
    js.Dynamic.global.navigator = js.Dynamic.literal(userAgent = "node.js");
  }

  def beforeEach(): Unit = {

    document.body.innerHTML = ""

    // prepare body with <div id="app"></div>
    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
    ()
  }

  def runChildren(): Unit = {
    (0 to 10) foreach { round =>
      val elemId = "msg"

      val handler = Handler.create[Int](0).unsafeRunSync
      val handler2 = Handler.create[Int](0).unsafeRunSync
      val handler3 = Handler.create[Int].unsafeRunSync

      val vtree = div(
        id := elemId,
        span(id := "pete", "Go!"),
        onClick foreach {},
        onDomMount foreach {},
        onDomUnmount foreach {},
        //      dsl.cls <-- handler.map(_.toString),
        //      dsl.value <-- handler.map(_.toString),
        handler.map { i =>
          (0 to i).map { j =>
            div(dsl.defaultValue := j.toString, styleAttr := "background:black;", input(tpe := "text"), span(span(span)), handler3)
          }
          //        input(tpe := "text", dsl.defaultValue := i.toString, styleAttr := "background:black;")
        },
        handler2.map { i =>
          (0 to i).map { j =>
            div(
              div("hans", cls := j.toString, onClick foreach {}, handler3),
              p(p),
              handler3
            )
          }
        }
      )

      val node = document.createElement("div")
      document.body.appendChild(node)

      val t = System.nanoTime()

      OutWatch.renderInto(node, vtree).unsafeRunSync()

      (0 to numIterations).foreach { i =>
        handler.onNext(i)
        handler2.onNext(i)
      }

//      println(node.innerHTML)

      val t2 = System.nanoTime()
      handler.onNext(numIterations + 1)
      val t3 = System.nanoTime()
      handler2.onNext(numIterations + 1)
      val t4 = System.nanoTime()

      println(s"NORMAL $round =====> " + (t2 - t))
      println("SINGLE1 TOOK =====> " + (t3 - t2))
      println("SINGLE2 TOOK =====> " + (t4 - t3))
    }
  }

  def runThunks(): Unit = {
    (0 to 10) foreach { round =>
      val elemId = "msg"

      val handler = Handler.create[Int](0).unsafeRunSync
      val handler2 = Handler.create[Int](0).unsafeRunSync
      val handler3 = Handler.create[Int].unsafeRunSync

      val vtree = div(
        id := elemId,
        span(id := "pete", "Go!"),
        onClick foreach {},
        onDomMount foreach {},
        onDomUnmount foreach {},
        //      dsl.cls <-- handler.map(_.toString),
        //      dsl.value <-- handler.map(_.toString),
        handler.map { i =>
          (0 to i).map { j =>
            input.thunk("handler")(j)(VDomModifier(tpe := "text", dsl.defaultValue := j.toString, styleAttr := "background:black;", handler3))
          }
          //        input(tpe := "text", dsl.defaultValue := i.toString, styleAttr := "background:black;")
        },
        handler2.map { i =>
          (0 to i).map { j =>
            div.thunk("handler2")(j)(VDomModifier(
              div("hans", cls := j.toString, onClick foreach {}, handler3),
              p(p),
              handler3
            ))
          }
        }
      )

      val node = document.createElement("div")
      document.body.appendChild(node)

      val t = System.nanoTime()

      OutWatch.renderInto(node, vtree).unsafeRunSync()

      (0 to numIterations).foreach { i =>
        handler.onNext(i)
        handler2.onNext(i)
      }

//      println(node.innerHTML)

      val t2 = System.nanoTime()
      handler.onNext(numIterations + 1)
      val t3 = System.nanoTime()
      handler2.onNext(numIterations + 1)
      val t4 = System.nanoTime()

      println(s"THUNK $round =====> " + (t2 - t))
      println("SINGLE1 TOOK =====> " + (t3 - t2))
      println("SINGLE2 TOOK =====> " + (t4 - t3))

    }
  }

  def runCommands(): Unit = {
    (0 to 10) foreach { round =>

      val elemId = "msg"

      val handler3 = Handler.create[Int].unsafeRunSync

      def node1(j: Int) = input(tpe := "text", dsl.defaultValue := j.toString, styleAttr := "background:black;", handler3)
      def node2(j: Int) = div(
        div("hans", cls := j.toString, onClick foreach {}, handler3),
        p(p),
        handler3
      )

      val handler = Handler.create[ChildCommand](ChildCommand.ReplaceAll(js.Array(node1(0)))).unsafeRunSync
      val handler2 = Handler.create[ChildCommand](ChildCommand.ReplaceAll(js.Array(node2(0)))).unsafeRunSync

      val vtree = div(
        id := elemId,
        span(id :=  "pete", "Go!"),
        onClick foreach   {},
        onDomMount foreach   {},
        onDomUnmount foreach   {},
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
      (0 to numIterations).foreach { i =>
        handler.onNext(ChildCommand.Append(node1(node1Counter)))
        handler2.onNext(ChildCommand.Append(node2(node2Counter)))

        node1Counter += 1
        node2Counter += 1
      }

      //      println(node.innerHTML)

      val t2 = System.nanoTime()
      handler.onNext(ChildCommand.Append(node1(node1Counter)))
      val t3 = System.nanoTime()
      handler2.onNext(ChildCommand.Append(node2(node2Counter)))
      val t4 = System.nanoTime()

      println(s"COMMAND $round =====> " + (t2 - t))
      println("SINGLE1 TOOK =====> " + (t3 - t2))
      println("SINGLE2 TOOK =====> " + (t4 - t3))
    }
  }
}
