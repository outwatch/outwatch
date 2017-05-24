package outwatch

import snabbdom.{DataObject, h, patch}

import scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLInputElement

import outwatch.dom._

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
    import outwatch.dom.<^.{< => tag, _}

    val clicks = createHandler[Int](1)
    val nodes = clicks.map { i =>
      tag.div(
        ^.key := s"key-$i",
        tag.span(^.click(if (i == 1) 2 else 1) --> clicks,  s"This is number $i", ^.id := "btn"),
        tag.input(^.id := "input")
      )
    }

    val node = document.createElement("div")
    node.id = "app"
    document.body.appendChild(node)

    OutWatch.render("#app", tag.div(^.child <-- nodes))

    val inputEvt = document.createEvent("HTMLEvents")
    inputEvt.initEvent("input", false, true)

    val clickEvt = document.createEvent("Events")
    clickEvt.initEvent("click", true, true)

    def inputElement() = document.getElementById("input").asInstanceOf[HTMLInputElement]
    val btn = document.getElementById("btn")

    inputElement().value = "Something"
    inputElement().dispatchEvent(inputEvt)
    btn.dispatchEvent(clickEvt)

    inputElement().value shouldBe ""
  }

}
