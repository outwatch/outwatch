package outwatch

import org.scalajs.dom.{document, html}
import outwatch.Deprecated.IgnoreWarnings.initEvent
import snabbdom.{DataObject, hFunction, patch}

import scala.scalajs.js

class SnabbdomSpec extends JSDomSpec {
  "The Snabbdom Facade" should "correctly patch the DOM" in {
    val message = "Hello World"
    val vNode = hFunction("span#msg", DataObject(js.Dictionary(), js.Dictionary()), message)

    val node = document.createElement("div")
    document.body.appendChild(node)

    patch(node, vNode)

    document.getElementById("msg").innerHTML shouldBe message

    val newMessage = "Hello Snabbdom!"
    val newNode = hFunction("div#new", DataObject(js.Dictionary(), js.Dictionary()), newMessage)

    patch(vNode, newNode)

    document.getElementById("new").innerHTML shouldBe newMessage
  }

  it should "correctly patch nodes with keys" in {
    import outwatch.dom._
    import outwatch.dom.dsl._

    val clicks = Handler.create[Int](1).unsafeRunSync()
    val nodes = clicks.map { i =>
      div(
        attributes.key := s"key-$i",
        span(onClick(if (i == 1) 2 else 1) --> clicks,  s"This is number $i", id := "btn"),
        input(id := "input")
      )
    }

    val node = document.createElement("div")
    node.id = "app"
    document.body.appendChild(node)

    OutWatch.renderInto("#app", div(nodes)).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    initEvent(inputEvt)("input", false, true)

    val clickEvt = document.createEvent("Events")
    initEvent(clickEvt)("click", true, true)

    def inputElement() = document.getElementById("input").asInstanceOf[html.Input]
    val btn = document.getElementById("btn")

    inputElement().value = "Something"
    inputElement().dispatchEvent(inputEvt)
    btn.dispatchEvent(clickEvt)

    inputElement().value shouldBe ""
  }

  it should "correctly handle boolean attributes" in {
    val message = "Hello World"
    val attributes = js.Dictionary[dom.Attr.Value]("bool1" -> true, "bool0" -> false, "string1" -> "true", "string0" -> "false")
    val vNode = hFunction("span#msg", DataObject(attributes, js.Dictionary()), message)

    val node = document.createElement("div")
    document.body.appendChild(node)

    patch(node, vNode)

    val expected = s"""<span id="msg" bool1="" string1="true" string0="false">$message</span>"""
    document.getElementById("msg").outerHTML shouldBe expected
  }
}
