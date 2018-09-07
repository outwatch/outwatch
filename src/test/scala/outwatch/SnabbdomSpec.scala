package outwatch

import scala.scalajs.js
import org.scalajs.dom.{document, html}
import outwatch.Deprecated.IgnoreWarnings.initEvent
import outwatch.dom.OutWatch
import outwatch.dom.dsl._
import cats.effect.IO
import snabbdom.{DataObject, hFunction, patch}

class SnabbdomSpec extends JSDomAsyncSpec {

  "The Snabbdom Facade" should "correctly patch the DOM" in {

    val message = "Hello World"

    for {

       vNode <- IO(hFunction("span#msg", DataObject(js.Dictionary(), js.Dictionary()), message))

        node <- IO {
                val node = document.createElement("div")
                document.body.appendChild(node)
                node
              }

            _ <- IO(patch(node, vNode))
          msg <- IO(document.getElementById("msg").innerHTML)
            _ = msg shouldBe message
       newMsg = "Hello Snabbdom!"
      newNode <- IO(hFunction("div#new", DataObject(js.Dictionary(), js.Dictionary()), newMsg))
            _ <- IO(patch(vNode, newNode))
         nMsg <- IO(document.getElementById("new").innerHTML)
            _ = nMsg shouldBe newMsg

    } yield succeed

  }

  it should "correctly patch nodes with keys" in {

    def inputElement() = document.getElementById("input").asInstanceOf[html.Input]

    Handler.create[Int](1).flatMap { clicks =>

      val nodes = clicks.map { i =>
        div(
          attributes.key := s"key-$i",
          span(onClick(if (i == 1) 2 else 1) --> clicks, s"This is number $i", id := "btn"),
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

               _ <- OutWatch.renderInto("#app", div(nodes))

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
    import outwatch.dom._
    import outwatch.dom.dsl._

    def getContent =
      IO(document.getElementById("content").innerHTML)

    for {
      a <- outwatch.Handler.create[Int](0)
      b <- outwatch.Handler.create[Int](100)
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
          _ <- OutWatch.renderInto("#app", vtree)
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

      vNode <- IO(hFunction("span#msg", DataObject(attributes, js.Dictionary()), message))
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
}
