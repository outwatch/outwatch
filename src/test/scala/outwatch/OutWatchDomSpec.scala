package outwatch

import cats.effect.IO
import org.scalajs.dom.{document, html}
import org.scalatest.BeforeAndAfterEach
import outwatch.dom.helpers._, DomUtils.Children
import outwatch.dom.{StringModifier, _}
import rxscalajs.{Observable, Subject}
import snabbdom.{DataObject, hFunction}

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.scalajs.js.JSON

class OutWatchDomSpec extends UnitSpec with BeforeAndAfterEach {

  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  "Properties" should "be separated correctly" in {
    val properties = Seq(
      Attribute("hidden", "true"),
      InsertHook(Subject()),
      UpdateHook(Subject()),
      InsertHook(Subject()),
      DestroyHook(Subject()),
      PrePatchHook(Subject()),
      PostPatchHook(Subject())
    )

    val DomUtils.SeparatedProperties(inserts, prepatch, updates, postpatch, deletes, attributes, keys) = DomUtils.separateProperties(properties)

    inserts.length shouldBe 2
    prepatch.length shouldBe 1
    updates.length shouldBe 1
    postpatch.length shouldBe 1
    deletes.length shouldBe 1
    attributes.length shouldBe 1
    keys.length shouldBe 0
  }

  "VDomModifiers" should "be separated correctly" in {
    val modifiers = Seq(
      Attribute("class", "red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      new StringModifier("Test"),
      div().unsafeRunSync(),
      CompositeModifier(
        Seq(
          div(),
          Attributes.`class` := "blue",
          Attributes.onClick(1) --> Sink.create[Int](_ => IO.pure(())),
          Attributes.hidden <-- Observable.of(false)
        ).map(_.unsafeRunSync())
      ),
      AttributeStreamReceiver("hidden",Observable.of())
    )

    val DomUtils.SeparatedModifiers(emitters, receivers, properties, Children.VNodes(childNodes, streamStatus)) = DomUtils.separateModifiers(modifiers)

    emitters.length shouldBe 2
    receivers.length shouldBe 2
    properties.length shouldBe 2
    childNodes.length shouldBe 3
    streamStatus.numChild shouldBe 0
    streamStatus.numChildren shouldBe 0
  }

  it should "be separated correctly with children" in {
    val modifiers = Seq(
      Attribute("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input",  _ => ()),
      AttributeStreamReceiver("hidden",Observable.of()),
      AttributeStreamReceiver("disabled",Observable.of()),
      Emitter("keyup",  _ => ()),
      StringModifier("text"),
      div
    )

    val DomUtils.SeparatedModifiers(emitters, receivers, properties, Children.VNodes(childNodes, streamStatus)) = DomUtils.separateModifiers(modifiers)

    emitters.length shouldBe 3
    receivers.length shouldBe 2
    properties.length shouldBe 1
    childNodes.length shouldBe 2
    streamStatus.numChild shouldBe 0
    streamStatus.numChildren shouldBe 0
  }

  it should "be separated correctly with string children" in {
    val modifiers: Seq[VDomModifier_] = Seq(
      Attribute("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input",  _ => ()),
      Emitter("keyup",  _ => ()),
      AttributeStreamReceiver("hidden",Observable.of()),
      AttributeStreamReceiver("disabled",Observable.of()),
      StringModifier("text"),
      StringVNode("text2")
    )

    val DomUtils.SeparatedModifiers(emitters, receivers, properties, Children.StringModifiers(stringMods)) = DomUtils.separateModifiers(modifiers)

    emitters.length shouldBe 3
    receivers.length shouldBe 2
    properties.length shouldBe 1
    stringMods.map(_.string) should contain theSameElementsAs(List(
      "text", "text2"
    ))
  }

  it should "be separated correctly with children and properties" in {
    val modifiers = Seq(
      Attribute("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input", _ => ()),
      UpdateHook(Subject()),
      AttributeStreamReceiver("hidden",Observable.of()),
      AttributeStreamReceiver("disabled",Observable.of()),
      ChildrenStreamReceiver(Observable.of()),
      Emitter("keyup", _ => ()),
      InsertHook(Subject()),
      PrePatchHook(Subject()),
      PostPatchHook(Subject()),
      StringModifier("text")
    )

    val DomUtils.SeparatedModifiers(emitters, receivers, properties, Children.VNodes(childNodes, streamStatus)) = DomUtils.separateModifiers(modifiers)

    val DomUtils.SeparatedProperties(inserts, prepatch, updates, postpatch, deletes, attributes, keys) = DomUtils.separateProperties(properties)

    emitters.map(_.eventType) shouldBe List("click", "input", "keyup")
    emitters.length shouldBe 3
    inserts.length shouldBe 1
    prepatch.length shouldBe 1
    updates.length shouldBe 1
    postpatch.length shouldBe 1
    deletes.length shouldBe 0
    attributes.length shouldBe 1
    receivers.length shouldBe 2
    keys.length shouldBe 0
    childNodes.length shouldBe 2
    streamStatus.numChild shouldBe 0
    streamStatus.numChildren shouldBe 1
  }

  val fixture = new {
    val proxy = hFunction("div", DataObject(js.Dictionary("class" -> "red", "id" -> "msg"), js.Dictionary()), js.Array(
      hFunction("span", DataObject(js.Dictionary(), js.Dictionary()), "Hello")
    ))
  }

  it should "be run once" in {
    val list = new collection.mutable.ArrayBuffer[String]

    val vtree = div(
      IO {
        list += "child1"
        ChildStreamReceiver(Observable.of())
      },
      IO {
        list += "child2"
        ChildStreamReceiver(Observable.of())
      },
      IO {
        list += "children1"
        ChildrenStreamReceiver(Observable.of())
      },
      IO {
        list += "children2"
        ChildrenStreamReceiver(Observable.of())
      },
      div(
        IO {
          list += "attr1"
          Attribute("attr1", "peter")
        },
        Seq(
          IO {
            list += "attr2"
            Attribute("attr2", "hans")
          }
        )
      )
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    list.isEmpty shouldBe true

    DomUtils.render(node, vtree).unsafeRunSync()

    list should contain theSameElementsAs List(
      "child1", "child2", "children1", "children2", "attr1", "attr2"
    )
  }

  it should "provide unique key for child nodes if stream is present" in {
    val mods = Seq(
      ChildrenStreamReceiver(Observable.of()),
      div(id := "1").unsafeRunSync(), div(id := "2").unsafeRunSync()
      // div().unsafeRunSync(), div().unsafeRunSync() //TODO: this should also work, but key is derived from hashCode of VTree (which in this case is equal)
    )

    val (Children.VNodes(childNodes, streamStatus), dataObject) = DomUtils.extractChildrenAndDataObject(mods)

    childNodes.size shouldBe 3
    streamStatus.numChild shouldBe 0
    streamStatus.numChildren shouldBe 1
    dataObject.key.nonEmpty shouldBe true

    val vtreeChildren = childNodes collect { case s: VTree => s }
    val vtreeChildren1 = vtreeChildren(0)
    val vtreeChildren2 = vtreeChildren(1)

    val (Children.Empty, dataObject1) = DomUtils.extractChildrenAndDataObject(vtreeChildren1.modifiers)
    val (Children.Empty, dataObject2) = DomUtils.extractChildrenAndDataObject(vtreeChildren2.modifiers)

    dataObject1.key.nonEmpty shouldBe true
    dataObject2.key.nonEmpty shouldBe true
    dataObject1.key.get should not be dataObject2.key.get
  }

  it should "keep existing key for child nodes" in {
    val mods = Seq(
      Key(1234),
      ChildrenStreamReceiver(Observable.of()),
      div()(IO.pure(Key(5678))).unsafeRunSync()
    )


    val (Children.VNodes(childNodes, streamStatus), dataObject) = DomUtils.extractChildrenAndDataObject(mods)

    childNodes.size shouldBe 2
    streamStatus.numChild shouldBe 0
    streamStatus.numChildren shouldBe 1
    dataObject.key.toOption shouldBe Some(1234)

    val vtreeChildren = childNodes collect { case s: VTree => s }
    val (Children.Empty, dataObject2) = DomUtils.extractChildrenAndDataObject(vtreeChildren.head.modifiers)

    dataObject2.key.toOption shouldBe Some(5678)
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


  it should "run its modifiers once!" in {
    val stringHandler = Handler.create[String]().unsafeRunSync()
    var ioCounter = 0
    var handlerCounter = 0
    stringHandler { _ =>
      handlerCounter += 1
    }

    val vtree = div(
      div(
        IO {
          ioCounter += 1
          Attribute("hans", "")
        }
      ),
      child <-- stringHandler
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    ioCounter shouldBe 0
    handlerCounter shouldBe 0
    DomUtils.render(node, vtree).unsafeRunSync()
    ioCounter shouldBe 1
    handlerCounter shouldBe 0
    stringHandler.observer.next("pups")
    ioCounter shouldBe 1
    handlerCounter shouldBe 1
  }

  it should "run its modifiers once in CompositeModifier!" in {
    val stringHandler = Handler.create[String]().unsafeRunSync()
    var ioCounter = 0
    var handlerCounter = 0
    stringHandler { _ =>
      handlerCounter += 1
    }

    val vtree = div(
      div(Seq(
        IO {
          ioCounter += 1
          Attribute("hans", "")
        }
      )),
      child <-- stringHandler
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    ioCounter shouldBe 0
    handlerCounter shouldBe 0
    DomUtils.render(node, vtree).unsafeRunSync()
    ioCounter shouldBe 1
    handlerCounter shouldBe 0
    stringHandler.observer.next("pups")
    ioCounter shouldBe 1
    handlerCounter shouldBe 1
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
      val pageNum = Handler.create[Int](num).unsafeRunSync()

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

    def boolBuilder(name: String) = new AttributeBuilder[Boolean](name, identity)
    def stringBuilder(name: String) = new AttributeBuilder[Boolean](name, _.toString)
    val vtree = div(
      boolBuilder("a"),
      boolBuilder("b") := true,
      boolBuilder("c") := false,
      stringBuilder("d"),
      stringBuilder("e") := true,
      stringBuilder("f") := false
    )

    val attrs = js.Dictionary[dom.Attr.Value]("a" -> true, "b" -> true, "c" -> false, "d" -> "true", "e" -> "true", "f" -> "false")
    val expected = hFunction("div", DataObject(attrs, js.Dictionary()))

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

  it should "render child nodes in correct order" in {
    val messagesA = Subject[String]
    val messagesB = Subject[String]
    val vNode = div(
      span("A"),
      child <-- messagesA.map(span(_)),
      span("B"),
      child <-- messagesB.map(span(_))
    )

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    messagesA.next("1")
    messagesB.next("2")

    node.innerHTML shouldBe "<div><span>A</span><span>1</span><span>B</span><span>2</span></div>"
  }

  it should "render child string-nodes in correct order" in {
    val messagesA = Subject[String]
    val messagesB = Subject[String]
    val vNode = div(
      "A",
      child <-- messagesA,
      "B",
      child <-- messagesB
    )

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    messagesA.next("1")
    messagesB.next("2")

    node.innerHTML shouldBe "<div>A1B2</div>"
  }

  it should "render child string-nodes in correct order, mixed with children" in {
    val messagesA = Subject[String]
    val messagesB = Subject[String]
    val messagesC = Subject[Seq[VNode]]
    val vNode = div(
      "A",
      child <-- messagesA,
      children <-- messagesC,
      "B",
      child <-- messagesB
    )

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    messagesA.next("1")
    messagesB.next("2")
    messagesC.next(Seq(div("5"), div("7")))

    node.innerHTML shouldBe "<div>A1<div>5</div><div>7</div>B2</div>"
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
    node.children(0).innerHTML shouldBe "<div>message</div><div>otherMessage</div>"

    otherMessages.next(Seq(div("genus")))
    node.children(0).innerHTML shouldBe "<div>message</div><div>genus</div>"
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
    val vNode = div(data.noise <-- messages)(data.noise <-- otherMessages)

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    otherMessages.next("otherMessage")
    node.children(0).getAttribute("data-noise") shouldBe "otherMessage"

    messages.next("message") // should be ignored
    node.children(0).getAttribute("data-noise") shouldBe "otherMessage"

    otherMessages.next("genus")
    node.children(0).getAttribute("data-noise") shouldBe "genus"
  }

  it should "update merged node styles written with style() correctly" in {
    val messages = Subject[String]
    val otherMessages = Subject[String]
    val vNode = div(style("color") <-- messages)(style("color") <-- otherMessages)

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

  it should "update merged node styles correctly" in {
    val messages = Subject[String]
    val otherMessages = Subject[String]
    val vNode = div(color <-- messages)(color <-- otherMessages)

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


  it should "render composite VNodes properly" in {
    val items = Seq("one", "two", "three")
    val vNode = div(items.map(item => span(item)))

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    node.innerHTML shouldBe "<div><span>one</span><span>two</span><span>three</span></div>"

  }

  it should "render nodes with only attribute receivers properly" in {
    val classes = Subject[String]
    val vNode = button( className <-- classes, "Submit")

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    classes.next("active")

    node.innerHTML shouldBe """<button class="active">Submit</button>"""
  }

  it should "work with custom tags" in {

    val vNode = div(tag("main")())

    val node = document.createElement("div")
    document.body.appendChild(node)

    DomUtils.render(node, vNode).unsafeRunSync()

    node.innerHTML shouldBe "<div><main></main></div>"
  }
}
