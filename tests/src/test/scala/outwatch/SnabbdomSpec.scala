package outwatch

import scala.scalajs.js
import org.scalajs.dom.{document, html}
import cats.effect.IO
import outwatch.Deprecated.IgnoreWarnings.initEvent

import outwatch.dom._
import outwatch.dom.dsl._
import outwatch.ext.monix._
import outwatch.ext.monix.handler._

import snabbdom._

class SnabbdomSpec extends JSDomAsyncSpec {

  "The Snabbdom Facade" should "correctly patch the DOM" in {

    val message = "Hello World"

    for {

       vNode <- IO(hFunction("span#msg", DataObject.empty, message))

        node <- IO {
                val node = document.createElement("div")
                document.body.appendChild(node)
                node
              }

            _ <- IO(patch(node, vNode))
          msg <- IO(document.getElementById("msg").innerHTML)
            _ = msg shouldBe message
       newMsg = "Hello Snabbdom!"
      newNode <- IO(hFunction("div#new", DataObject.empty, newMsg))
            _ <- IO(patch(vNode, newNode))
         nMsg <- IO(document.getElementById("new").innerHTML)
            _ = nMsg shouldBe newMsg

    } yield succeed

  }

  it should "correctly patch nodes with keys" in {

    def inputElement() = document.getElementById("input").asInstanceOf[html.Input]

    Handler.createF[IO](1).flatMap { clicks =>

      val nodes = clicks.map { i =>
        div(
          attributes.key := s"key-$i",
          span(onClick.use(if (i == 1) 2 else 1) --> clicks, s"This is number $i", id := "btn"),
          input(id := "input")
        )
      }

      for {

               _ <- IO {
                   val node = document.createElement("div")
                   node.id = "app"
                   document.body.appendChild(node)
                   node
                 }

               _ <- OutWatch.renderInto[IO]("#app", div(nodes))

        inputEvt <- IO {
                    val inputEvt = document.createEvent("HTMLEvents")
                    initEvent(inputEvt)("input", canBubbleArg = false, cancelableArg = true)
                    inputEvt
                  }

        clickEvt <- IO {
                    val clickEvt = document.createEvent("Events")
                    initEvent(clickEvt)("click", canBubbleArg = true, cancelableArg = true)
                    clickEvt
                  }

             btn <- IO(document.getElementById("btn"))

              ie <- IO {
                    inputElement().value = "Something"
                    inputElement().dispatchEvent(inputEvt)
                    btn.dispatchEvent(clickEvt)
                    inputElement().value
                  }
               _ = ie shouldBe ""

      } yield succeed

    }
  }

  it should "handle keys with nested observables" in {
    def getContent =
      IO(document.getElementById("content").innerHTML)

    for {
      a <- Handler.createF[IO](0)
      b <- Handler.createF[IO](100)
      vtree = div(
              a.map { a =>
                div(
                  id := "content",
                  dsl.key := "bla",
                  a,
                  b.map { b => div(id := "meh", b) }
                )
              }
            )
          _ <- OutWatch.renderInto[IO]("#app", vtree)
          c1 <- getContent
           _ <- IO.fromFuture(IO(a.onNext(1)))
          c2 <- getContent
           _ <- IO.fromFuture(IO(b.onNext(200)))
          c3 <- getContent
    } yield {
      c1 shouldBe """0<div id="meh">100</div>"""
      c2 shouldBe """1<div id="meh">100</div>"""
      c3 shouldBe """1<div id="meh">200</div>"""
    }

  }

  it should "correctly handle boolean attributes" in {

    val message    = "Hello World"
    val attributes = js.Dictionary[dom.Attr.Value]("bool1" -> true, "bool0" -> false, "string1" -> "true", "string0" -> "false")
    val expected   = s"""<span id="msg" bool1="" string1="true" string0="false">$message</span>"""

    for {

      vNode <- IO(hFunction("span#msg", new DataObject { attrs = attributes }, message))
       node <- IO {
                val node = document.createElement("div")
                document.body.appendChild(node)
                node
              }
          _ <- IO(patch(node, vNode))
       html <- IO(document.getElementById("msg").outerHTML)
          _ = html shouldBe expected

    } yield succeed

  }

  it should "correctly use thunks for updating" in {
    var renderFnCounter = 0

    val renderFn: String => VNodeProxy = { message =>
      renderFnCounter += 1
      hFunction("span#msg", new DataObject { key = "key" }, message)
    }

    val message = "Hello World"

    val node = document.createElement("div")
    document.body.appendChild(node)

    renderFnCounter shouldBe 0


    val vNode1 = thunk(js.undefined, "span#msg", "key", () => renderFn(message), js.Array(message))
    val p1 = patch(node, vNode1)

    renderFnCounter shouldBe 1
    document.getElementById("msg").innerHTML shouldBe message


    val vNode2 = thunk(js.undefined, "span#msg", "key", () => renderFn(message), js.Array(message))
    val p2 = patch(p1, vNode2)

    renderFnCounter shouldBe 1
    document.getElementById("msg").innerHTML shouldBe message


    val newMessage = "Hello Snabbdom!"
    val vNode3 = thunk(js.undefined, "span#msg", "key", () => renderFn(newMessage), js.Array(newMessage))
    val p3 = patch(p2, vNode3)

    p3 should not be null
    renderFnCounter shouldBe 2
    document.getElementById("msg").innerHTML shouldBe newMessage
  }

  it should "correctly use conditional thunks for updating" in {
    var renderFnCounter = 0

    val renderFn: String => VNodeProxy = { message =>
      renderFnCounter += 1
      hFunction("span#msg", new DataObject { key = "key" }, message)
    }

    val message = "Hello World"

    val node = document.createElement("div")
    document.body.appendChild(node)

    renderFnCounter shouldBe 0


    val vNode1 = thunk.conditional(js.undefined, "span#msg", "key", () => renderFn(message), shouldRender = true)
    val p1 = patch(node, vNode1)

    renderFnCounter shouldBe 1
    document.getElementById("msg").innerHTML shouldBe message


    val vNode2 = thunk.conditional(js.undefined, "span#msg", "key", () => renderFn(message), shouldRender = false)
    val p2 = patch(p1, vNode2)

    renderFnCounter shouldBe 1
    document.getElementById("msg").innerHTML shouldBe message


    val newMessage = "Hello Snabbdom!"
    val vNode3 = thunk.conditional(js.undefined, "span#msg", "key", () => renderFn(newMessage), shouldRender = true)
    val p3 = patch(p2, vNode3)

    p3 should not be null
    renderFnCounter shouldBe 2
    document.getElementById("msg").innerHTML shouldBe newMessage
  }
}
