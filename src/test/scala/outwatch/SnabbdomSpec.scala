package outwatch

import snabbdom.{DataObject, h, patch}

import scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.html

class SnabbdomSpec extends UnitSpec {
  "The Snabbdom Facade" should "correctly patch the DOM" in {
    val message = "Hello World"
    val vNode = h("span#msg", DataObject(js.Dictionary(), js.Dictionary()), message)

    val node = document.createElement("div")
    document.body.appendChild(node)

    patch(node, vNode)

    document.getElementById("msg").innerHTML shouldBe message

    val newMessage = "Hello Snabbdom!"
    val newNode = h("div#new", DataObject(js.Dictionary(), js.Dictionary()), newMessage)

    patch(vNode, newNode)

    document.getElementById("new").innerHTML shouldBe newMessage
  }

  it should "correctly patch nodes with keys" in {
    import outwatch.dom._

    val clicks = Handler.create[Int](1).unsafeRunSync()
    val nodes = clicks.map { i =>
      div(
        dom.key := s"key-$i",
        span(onClick(if (i == 1) 2 else 1) --> clicks,  s"This is number $i", id := "btn"),
        input(id := "input")
      )
    }

    val node = document.createElement("div")
    node.id = "app"
    document.body.appendChild(node)

    OutWatch.render("#app", div(child <-- nodes)).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    inputEvt.initEvent("input", false, true)

    val clickEvt = document.createEvent("Events")
    clickEvt.initEvent("click", true, true)

    def inputElement() = document.getElementById("input").asInstanceOf[html.Input]
    val btn = document.getElementById("btn")

    inputElement().value = "Something"
    inputElement().dispatchEvent(inputEvt)
    btn.dispatchEvent(clickEvt)

    inputElement().value shouldBe ""
  }

  it should "correctly handle boolean attributes" in {
    val message = "Hello World"
    val attributes = js.Dictionary[dom.Attribute.Value]("bool1" -> true, "bool0" -> false, "string1" -> "true", "string0" -> "false")
    val vNode = h("span#msg", DataObject(attributes, js.Dictionary()), message)

    val node = document.createElement("div")
    document.body.appendChild(node)

    patch(node, vNode)

    val expected = s"""<span id="msg" bool1="" string1="true" string0="false">$message</span>"""
    document.getElementById("msg").outerHTML shouldBe expected
  }
}
