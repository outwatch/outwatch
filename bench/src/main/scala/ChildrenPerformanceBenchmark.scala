package outwatch

import cats.effect.SyncIO

import outwatch._
import outwatch.dsl._
import colibri._

import org.scalajs.dom.{document, window}
import scala.scalajs.js
import scala.scalajs.js.annotation._
import bench._

@js.native
@JSImport("jsdom", JSImport.Namespace)
object jsdom extends js.Object {
  def jsdom(innerHTML: js.UndefOr[String]): js.Any = js.native
}

object ChildrenPerformance {

  def main(args: Array[String]): Unit = {
    import scala.concurrent.duration._
    setupJsDom()

    bench.util.runComparison(childrenBenchmark, List(100), 5.minutes)
  }

  val childrenBenchmark = Comparison(
    "Patching",
    Seq(
      Benchmark[Int](
        "Children",
        { size => beforeEach(); size },
        size => runChildren(size),
      ),
      Benchmark[Int](
        "Thunk",
        { size => beforeEach(); size },
        size => runThunks(size),
      ),
      Benchmark[Int](
        "Command",
        { size => beforeEach(); size },
        size => runCommands(size),
      ),
    ),
  )

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

  def runChildren(size: Int): Unit = {
    val elemId = "msg"

    val handler  = Subject.behavior[Int](0)
    val handler2 = Subject.behavior[Int](0)
    val handler3 = Subject.replayLatest[Int]()

    val vtree = div(
      idAttr := elemId,
      span(idAttr := "pete", "Go!"),
      onClick doAction {},
      onDomMount doAction {},
      onDomUnmount doAction {},
      //      dsl.cls <-- handler.map(_.toString),
      //      dsl.value <-- handler.map(_.toString),
      handler.map { i =>
        (0 to i).map { j =>
          div(
            dsl.defaultValue := j.toString,
            styleAttr        := "background:black;",
            input(tpe := "text"),
            span(span(span)),
            handler3,
          )
        }
        //        input(tpe := "text", dsl.defaultValue := i.toString, styleAttr := "background:black;")
      },
      handler2.map { i =>
        (0 to i).map { j =>
          div(
            div("hans", cls := j.toString, onClick doAction {}, handler3),
            p(p),
            handler3,
          )
        }
      },
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    Outwatch.renderInto[SyncIO](node, vtree).unsafeRunSync()

    (0 to size).foreach { i =>
      handler.unsafeOnNext(i)
      handler2.unsafeOnNext(i)
    }

    // println(node.innerHTML)

  }

  def runThunks(size: Int): Unit = {
    val elemId = "msg"

    val handler  = Subject.behavior[Int](0)
    val handler2 = Subject.behavior[Int](0)
    val handler3 = Subject.replayLatest[Int]()

    val vtree = div(
      idAttr := elemId,
      span(idAttr := "pete", "Go!"),
      onClick doAction {},
      onDomMount doAction {},
      onDomUnmount doAction {},
      //      dsl.cls <-- handler.map(_.toString),
      //      dsl.value <-- handler.map(_.toString),
      handler.map { i =>
        (0 to i).map { j =>
          input.thunk("handler")(j)(
            VModifier(tpe := "text", dsl.defaultValue := j.toString, styleAttr := "background:black;", handler3),
          )
        }
        //        input(tpe := "text", dsl.defaultValue := i.toString, styleAttr := "background:black;")
      },
      handler2.map { i =>
        (0 to i).map { j =>
          div.thunk("handler2")(j)(
            VModifier(
              div("hans", cls := j.toString, onClick doAction {}, handler3),
              p(p),
              handler3,
            ),
          )
        }
      },
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    Outwatch.renderInto[SyncIO](node, vtree).unsafeRunSync()

    (0 to size).foreach { i =>
      handler.unsafeOnNext(i)
      handler2.unsafeOnNext(i)
    }

    // println(node.innerHTML)

  }

  def runCommands(size: Int): Unit = {
    val elemId = "msg"

    val handler3 = Subject.replayLatest[Int]()

    def node1(j: Int) = input(tpe := "text", dsl.defaultValue := j.toString, styleAttr := "background:black;", handler3)
    def node2(j: Int) = div(
      div("hans", cls := j.toString, onClick doAction {}, handler3),
      p(p),
      handler3,
    )

    val handler  = Subject.behavior[ChildCommand](ChildCommand.ReplaceAll(js.Array(node1(0))))
    val handler2 = Subject.behavior[ChildCommand](ChildCommand.ReplaceAll(js.Array(node2(0))))

    val vtree = div(
      idAttr := elemId,
      span(idAttr := "pete", "Go!"),
      onClick doAction {},
      onDomMount doAction {},
      onDomUnmount doAction {},
      //      dsl.cls <-- handler.map(_.toString),
      //      dsl.value <-- handler.map(_.toString),
      handler,
      handler2,
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    Outwatch.renderInto[SyncIO](node, vtree).unsafeRunSync()

    var node1Counter = 0
    var node2Counter = 0
    (0 to size).foreach { i =>
      handler.unsafeOnNext(ChildCommand.Append(node1(node1Counter)))
      handler2.unsafeOnNext(ChildCommand.Append(node2(node2Counter)))

      node1Counter += 1
      node2Counter += 1
    }

    // println(node.innerHTML)
  }
}
