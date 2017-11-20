package outwatch

import cats.effect.IO
import org.scalajs.dom.document
import org.scalajs.dom.html
import org.scalatest.BeforeAndAfterEach
import outwatch.dom.{StringNode, _}
import outwatch.dom.helpers._
import rxscalajs.{Observable, Subject}
import snabbdom.{DataObject, h}

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}

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

    val DomUtils.SeparatedReceivers(child$, children$, attribute$) = DomUtils.separateReceivers(receivers)

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

    val DomUtils.SeparatedProperties(inserts, deletes, updates, attributes, keys) = DomUtils.separateProperties(properties)

    inserts.length shouldBe 2
    deletes.length shouldBe 1
    updates.length shouldBe 1
    attributes.length shouldBe 1
    keys.length shouldBe 0
  }

  "VDomModifiers" should "be separated correctly" in {
    val modifiers = Seq(
      Attribute("class", "red"),
      EmptyVDomModifier,
      Emitter("click", _ => ()),
      new StringNode("Test"),
      div().unsafeRunSync(),
      AttributeStreamReceiver("hidden",Observable.of())
    )

    val DomUtils.SeparatedModifiers(emitters, receivers, properties, vNodes) = DomUtils.separateModifiers(modifiers)

    emitters.length shouldBe 1
    receivers.length shouldBe 1
    vNodes.length shouldBe 2
    properties.length shouldBe 1
  }

  it should "be separated correctly with children" in {
    val modifiers = Seq(
      Attribute("class","red"),
      EmptyVDomModifier,
      Emitter("click", _ => ()),
      Emitter("input",  _ => ()),
      AttributeStreamReceiver("hidden",Observable.of()),
      AttributeStreamReceiver("disabled",Observable.of()),
      ChildrenStreamReceiver(Observable.of()),
      Emitter("keyup",  _ => ())
    )

    val DomUtils.SeparatedModifiers(emitters, receivers, properties, children) = DomUtils.separateModifiers(modifiers)

    val DomUtils.SeparatedReceivers(child$, children$, attribute$) = DomUtils.separateReceivers(receivers)

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
      EmptyVDomModifier,
      Emitter("click", _ => ()),
      Emitter("input", _ => ()),
      UpdateHook(Subject()),
      AttributeStreamReceiver("hidden",Observable.of()),
      AttributeStreamReceiver("disabled",Observable.of()),
      ChildrenStreamReceiver(Observable.of()),
      Emitter("keyup", _ => ()),
      InsertHook(Subject())
    )

    val DomUtils.SeparatedModifiers(emitters, receivers, properties, children) = DomUtils.separateModifiers(modifiers)

    val DomUtils.SeparatedReceivers(child$, children$, attribute$) = DomUtils.separateReceivers(receivers)

    val DomUtils.SeparatedProperties(inserts, deletes, updates, attributes, keys) = DomUtils.separateProperties(properties)

    emitters.length shouldBe 3
    child$.length shouldBe 0
    children$.length shouldBe 1
    inserts.length shouldBe 1
    deletes.length shouldBe 0
    updates.length shouldBe 1
    attributes.length shouldBe 1
    attribute$.length shouldBe 2
    children.length shouldBe 0
    keys.length shouldBe 0

  }

  val fixture = new {
    val proxy = h("div", DataObject(js.Dictionary("class" -> "red", "id" -> "msg"), js.Dictionary()), js.Array(
      h("span", DataObject(js.Dictionary(), js.Dictionary()), js.Array("Hello"))
    ))
  }

  "VTrees" should "be constructed correctly" in {

    val attributes = List(Attribute("class", "red"), Attribute("id", "msg"))
    val message = "Hello"
    val child = span(message)
    val vtree = div(IO.pure(attributes.head), IO.pure(attributes(1)), child)

    val proxy = fixture.proxy

    JSON.stringify(vtree.map(_.asProxy).unsafeRunSync()) shouldBe JSON.stringify(proxy)

  }

  it should "be correctly created with the HyperscriptHelper" in {
    val attributes = List(Attribute("class", "red"), Attribute("id", "msg"))
    val message = "Hello"
    val child = span(message)
    val vtree = div(IO.pure(attributes.head), IO.pure(attributes(1)), child)

    JSON.stringify(vtree.map(_.asProxy).unsafeRunSync()) shouldBe JSON.stringify(fixture.proxy)
  }


  it should "be correctly patched into the DOM" in {
    val id = "msg"
    val cls = "red"
    val attributes = List(Attribute("class", cls), Attribute("id", id))
    val message = "Hello"
    val child = span(message)
    val vtree = div(IO.pure(attributes.head), IO.pure(attributes(1)), child)


    val node = document.createElement("div")
    document.body.appendChild(node)

    DomUtils.render(node, vtree).unsafeRunSync()

    val patchedNode = document.getElementById(id)

    patchedNode.childElementCount shouldBe 1
    patchedNode.classList.contains(cls) shouldBe true
    patchedNode.children(0).innerHTML shouldBe message

  }

  it should "be replaced if they contain changeables" in {

    def page(num: Int): VNode = {
      val pageNum = Pipe.create[Int](num).unsafeRunSync()

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

    DomUtils.render(node, vtree).unsafeRunSync()

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

    JSON.stringify(vtree.map(_.asProxy).unsafeRunSync()) shouldBe JSON.stringify(fixture.proxy)

  }

  it should "construct VTrees with optional children properly" in {
    import outwatch.dom._

    val vtree = div(cls := "red", id := "msg",
      Option(span("Hello")),
      Option.empty[VDomModifier]
    )

    JSON.stringify(vtree.map(_.asProxy).unsafeRunSync()) shouldBe JSON.stringify(fixture.proxy)

  }

  it should "construct VTrees with boolean attributes" in {
    import outwatch.dom._

    def boolBuilder(name: String) = new BoolAttributeBuilder(name)
    def anyBuilder(name: String) = new AttributeBuilder[Boolean](name)
    val vtree = div(
      IO.pure(boolBuilder("a")),
      boolBuilder("b") := true,
      boolBuilder("c") := false,
      anyBuilder("d") := true,
      anyBuilder("e") := false
    )

    val attrs = js.Dictionary[String | Boolean]("a" -> true, "b" -> true, "c" -> false, "d" -> "true", "e" -> "false")
    val expected = h("div", DataObject(attrs, js.Dictionary()), js.Array[Any]())

    JSON.stringify(vtree.map(_.asProxy).unsafeRunSync()) shouldBe JSON.stringify(expected)

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

    DomUtils.render(node, vtree).unsafeRunSync()

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

    DomUtils.render(node, vtree).unsafeRunSync()

    val field = document.getElementById("input").asInstanceOf[html.Input]

    field.value shouldBe ""

    val message = "Hello"
    messages.next(message)

    field.value shouldBe message

    val message2 = "World"
    messages.next(message2)

    field.value shouldBe message2

  }

  it should "update merged nodes children correctly" in {
    val messages = Subject[Seq[VNode]]
    val otherMessages = Subject[Seq[VNode]]
    val vNode = div(children <-- messages)(children <-- otherMessages)

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    otherMessages.next(Seq(div("otherMessage")))
    node.children(0).innerHTML shouldBe "<div>otherMessage</div>"

    messages.next(Seq(div("message")))
    node.children(0).innerHTML shouldBe "<div>otherMessage</div>"

    otherMessages.next(Seq(div("genus")))
    node.children(0).innerHTML shouldBe "<div>genus</div>"
  }

  it should "update merged nodes separate children correctly" in {
    val messages = Subject[String]
    val otherMessages = Subject[String]
    val vNode = div(child <-- messages)(child <-- otherMessages)

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    otherMessages.next("otherMessage")
    node.children(0).innerHTML shouldBe ""

    messages.next("message")
    node.children(0).innerHTML shouldBe "messageotherMessage"

    otherMessages.next("genus")
    node.children(0).innerHTML shouldBe "messagegenus"
  }

  it should "update reused vnodes correctly" in {
    val messages = Subject[String]
    val vNode = div(data.ralf := true, child <-- messages)
    val container = div(vNode, vNode)

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, container).unsafeRunSync()

    messages.next("message")
    node.children(0).children(0).innerHTML shouldBe "message"
    node.children(0).children(1).innerHTML shouldBe "message"

    messages.next("bumo")
    node.children(0).children(0).innerHTML shouldBe "bumo"
    node.children(0).children(1).innerHTML shouldBe "bumo"
  }

  it should "update merged nodes correctly (render reuse)" in {
    val messages = Subject[String]
    val otherMessages = Subject[String]
    val vNodeTemplate = div(child <-- messages)
    val vNode = vNodeTemplate(child <-- otherMessages)

    val node1 = document.createElement("div")
    document.body.appendChild(node1)
    DomUtils.render(node1, vNodeTemplate).unsafeRunSync()

    val node2 = document.createElement("div")
    document.body.appendChild(node2)
    DomUtils.render(node2, vNode).unsafeRunSync()

    messages.next("gurkon")
    otherMessages.next("otherMessage")
    node1.children(0).innerHTML shouldBe "gurkon"
    node2.children(0).innerHTML shouldBe "gurkonotherMessage"

    messages.next("message")
    node1.children(0).innerHTML shouldBe "message"
    node2.children(0).innerHTML shouldBe "messageotherMessage"

    otherMessages.next("genus")
    node1.children(0).innerHTML shouldBe "message"
    node2.children(0).innerHTML shouldBe "messagegenus"
  }

  it should "update merged node attributes correctly" in {
    val messages = Subject[String]
    val otherMessages = Subject[String]
    val vNode = div(data <-- messages)(data <-- otherMessages)

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    otherMessages.next("otherMessage")
    node.children(0).getAttribute("data") shouldBe "otherMessage"

    messages.next("message") // should be ignored
    node.children(0).getAttribute("data") shouldBe "otherMessage"

    otherMessages.next("genus")
    node.children(0).getAttribute("data") shouldBe "genus"
  }

  it should "update merged node styles correctly" in {
    val messages = Subject[String]
    val otherMessages = Subject[String]
    val vNode = div(stl("color") <-- messages)(stl("color") <-- otherMessages)

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    otherMessages.next("red")
    node.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

    messages.next("blue") // should be ignored
    node.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

    otherMessages.next("green")
    node.children(0).asInstanceOf[html.Element].style.color shouldBe "green"
  }

}
