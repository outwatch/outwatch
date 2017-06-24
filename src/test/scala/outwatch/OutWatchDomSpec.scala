package outwatch

import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.dom.{Event, KeyboardEvent, document}
import org.scalatest.BeforeAndAfterEach
import outwatch.dom.VDomModifier.VTree
import outwatch.dom._
import outwatch.dom.helpers._
import rxscalajs.{Observable, Subject}
import snabbdom.{DataObject, h}

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls
import scala.scalajs.js
import scala.scalajs.js.JSON

class OutWatchDomSpec extends UnitSpec with BeforeAndAfterEach {

  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  "Receivers" should "be separated correctly" in {
    val receivers = Seq(
      AttributeStreamReceiver("hidden",Observable.of()),
      AttributeStreamReceiver("disabled",Observable.of()),
      ChildStreamReceiver(Observable.of()),
      ChildrenStreamReceiver(Observable.of())
    )

    val (child$, children$, attribute$) = DomUtils.separateReceivers(receivers)

    child$.length shouldBe 1
    children$.length shouldBe 1
    attribute$.length shouldBe 2

  }

  "Properties" should "be separated correctly" in {
    val properties = Seq(
      Attribute("hidden", "true"),
      InsertHook(Subject()),
      UpdateHook(Subject()),
      InsertHook(Subject()),
      DestroyHook(Subject())
    )

    val (inserts, deletes, updates, attributes, keys) = DomUtils.separateProperties(properties)

    inserts.length shouldBe 2
    deletes.length shouldBe 1
    updates.length shouldBe 1
    attributes.length shouldBe 1
  }

  "VDomModifiers" should "be separated correctly" in {
    val modifiers = Seq(
      Attribute("class", "red"),
      EventEmitter("click", Subject()),
      VDomModifier.StringNode("Test"),
      DomUtils.hyperscriptHelper("div")(),
      AttributeStreamReceiver("hidden",Observable.of())
    )

    val (emitters, receivers, properties, vNodes) = DomUtils.separateModifiers(modifiers)

    emitters.length shouldBe 1
    receivers.length shouldBe 1
    vNodes.length shouldBe 2
    properties.length shouldBe 1
  }

  it should "be separated correctly with children" in {
    val modifiers = Seq(
      Attribute("class","red"),
      EventEmitter[Event]("click",Subject()),
      EventEmitter[InputEvent]("input", Subject()),
      AttributeStreamReceiver("hidden",Observable.of()),
      AttributeStreamReceiver("disabled",Observable.of()),
      ChildrenStreamReceiver(Observable.of()),
      EventEmitter[KeyboardEvent]("keyup", Subject())
    )

    val (emitters, receivers, properties, children) = DomUtils.separateModifiers(modifiers)

    val (child$, children$, attribute$) = DomUtils.separateReceivers(receivers)

    emitters.length shouldBe 3
    child$.length shouldBe 0
    children$.length shouldBe 1
    properties.length shouldBe 1
    attribute$.length shouldBe 2
    children.length shouldBe 0

  }

  it should "be separated correctly with children and properties" in {
    val modifiers = Seq(
      Attribute("class","red"),
      EventEmitter[Event]("click",Subject()),
      EventEmitter[InputEvent]("input", Subject()),
      UpdateHook(Subject()),
      AttributeStreamReceiver("hidden",Observable.of()),
      AttributeStreamReceiver("disabled",Observable.of()),
      ChildrenStreamReceiver(Observable.of()),
      EventEmitter[KeyboardEvent]("keyup", Subject()),
      InsertHook(Subject())
    )

    val (emitters, receivers, properties, children) = DomUtils.separateModifiers(modifiers)

    val (child$, children$, attribute$) = DomUtils.separateReceivers(receivers)

    val (inserts, deletes, updates, attributes, keys) = DomUtils.separateProperties(properties)

    emitters.length shouldBe 3
    child$.length shouldBe 0
    children$.length shouldBe 1
    inserts.length shouldBe 1
    deletes.length shouldBe 0
    updates.length shouldBe 1
    attributes.length shouldBe 1
    attribute$.length shouldBe 2
    children.length shouldBe 0

  }

  val fixture = new {
    val proxy = h("div", DataObject(js.Dictionary("class" -> "red", "id" -> "msg"), js.Dictionary()), js.Array(
      h("span", DataObject(js.Dictionary(), js.Dictionary()), js.Array("Hello"))
    ))
  }

  "VTrees" should "be constructed correctly" in {

    val attributes = List(Attribute("class", "red"), Attribute("id", "msg"))
    val message = "Hello"
    val child = DomUtils.hyperscriptHelper("span")(message)
    val nodeType = "div"
    val vtree = DomUtils.hyperscriptHelper(nodeType)(attributes.head, attributes(1), child).asInstanceOf[VDomModifier.VTree]

    vtree.nodeType shouldBe nodeType
    vtree.children.length shouldBe 1
    child.asInstanceOf[VTree].children.length shouldBe 1

    val proxy = fixture.proxy

    JSON.stringify(vtree.asProxy) shouldBe JSON.stringify(proxy)

  }

  it should "be correctly created with the HyperscriptHelper" in {
    val attributes = List(Attribute("class", "red"), Attribute("id", "msg"))
    val message = "Hello"
    val child = DomUtils.hyperscriptHelper("span")(message)
    val nodeType = "div"
    val vtree = DomUtils.hyperscriptHelper(nodeType)(attributes.head, attributes(1), child)

    JSON.stringify(vtree.asProxy) shouldBe JSON.stringify(fixture.proxy)
  }


  it should "be correctly patched into the DOM" in {
    val id = "msg"
    val cls = "red"
    val attributes = List(Attribute("class", cls), Attribute("id", id))
    val message = "Hello"
    val child = DomUtils.hyperscriptHelper("span")(message)
    val nodeType = "div"
    val vtree = DomUtils.hyperscriptHelper(nodeType)(attributes.head, attributes(1), child)


    val node = document.createElement("div")
    document.body.appendChild(node)

    DomUtils.render(node, vtree)

    val patchedNode = document.getElementById(id)

    patchedNode.childElementCount shouldBe 1
    patchedNode.classList.contains(cls) shouldBe true
    patchedNode.children(0).innerHTML shouldBe message

  }

  it should "be replaced if they contain changeables" in {

    def page(num: Int): VNode = {
      val pageNum = createHandler[Int](num)

      div( id := "page",
        num match {
          case 1 =>
            div(child <-- pageNum)
          case 2 =>
            div(child <-- pageNum)
        }
      )
    }

    val pageHandler =  Subject[Int]

    val vtree = div(
      div(child <-- pageHandler.map(page))
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    DomUtils.render(node, vtree)

    pageHandler.next(1)

    val domNode = document.getElementById("page")

    domNode.textContent shouldBe "1"

    pageHandler.next(2)

    domNode.textContent shouldBe "2"

  }

  "The HTML DSL" should "construct VTrees properly" in {
    import outwatch.dom._

    val vtree = div(cls := "red", id := "msg",
      span("Hello")
    )

    JSON.stringify(vtree.asProxy) shouldBe JSON.stringify(fixture.proxy)

  }

  it should "patch into the DOM properly" in {
    import outwatch.dom._

    val message = "Test"
    val vtree = div(cls := "blue", id := "test",
      span(message),
      ul(id := "list",
        li("1"),
        li("2"),
        li("3")
      )
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    DomUtils.render(node, vtree)

    val patchedNode = document.getElementById("test")

    patchedNode.childElementCount shouldBe 2
    patchedNode.classList.contains("blue") shouldBe true
    patchedNode.children(0).innerHTML shouldBe message

    document.getElementById("list").childElementCount shouldBe 3

  }

  it should "change the value of a textfield" in {


    val messages = Subject[String]
    val vtree = div(
      input(outwatch.dom.value <-- messages, id := "input")
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    DomUtils.render(node, vtree)

    val field = document.getElementById("input").asInstanceOf[HTMLInputElement]

    field.value shouldBe ""

    val message = "Hello"
    messages.next(message)

    field.value shouldBe message

    val message2 = "World"
    messages.next(message2)

    field.value shouldBe message2

  }

}
