package outwatch

import cats.effect.IO
import monix.reactive.subjects.{BehaviorSubject, PublishSubject, Var}
import org.scalajs.dom.window.localStorage
import org.scalajs.dom.{document, html, Element}
import outwatch.Deprecated.IgnoreWarnings.initEvent
import outwatch.dom._
import monix.execution.Ack.Continue
import monix.reactive.{Observable, Observer}
import outwatch.dom.helpers._
import outwatch.dom.dsl._
import outwatch.dom.helpers._
import snabbdom.{DataObject, Hooks, hFunction}
import org.scalajs.dom.window.localStorage
import org.scalatest.Assertion

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON

class OutWatchDomSpec extends JSDomAsyncSpec {
  implicit def ListToJsArray[T](list: Seq[T]): js.Array[T] = list.toJSArray

  def sendEvent(elem: Element, eventType: String) = {
    val event = document.createEvent("Events")
    initEvent(event)(eventType, canBubbleArg = true, cancelableArg = false)
    elem.dispatchEvent(event)
  }

  "Properties" should "be separated correctly" in {
    val properties = Seq(
      BasicAttr("hidden", "true"),
      InitHook(_ => ()),
      InsertHook(_ => ()),
      UpdateHook((_,_) => ()),
      InsertHook(_ => ()),
      DestroyHook(_ => ()),
      PrePatchHook((_,_) => ()),
      PostPatchHook((_,_) => ())
    )

    val seps = SeparatedModifiers.from(properties)
    import seps._

    initHook.isDefined shouldBe true
    insertHook.isDefined shouldBe true
    prePatchHook.isDefined shouldBe true
    updateHook.isDefined shouldBe true
    postPatchHook.isDefined shouldBe true
    destroyHook.isDefined shouldBe true
    attrs.get.values.size shouldBe 1
    keyOption.isEmpty shouldBe true
  }

  "VDomModifiers" should "be separated correctly" in {
    val modifiers = Seq(
      BasicAttr("class", "red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      new StringVNode("Test"),
      div(),
      CompositeModifier(
        Seq(
          div(),
          attributes.`class` := "blue",
          attributes.onClick(1) foreach {},
          attributes.hidden <-- Observable(false)
        )
      ),
      ModifierStreamReceiver(ValueObservable.empty)
    )

    val streamable = NativeModifiers.from(modifiers)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    emitters.get.values.size shouldBe 1
    attrs.get.values.size shouldBe 1
    streamable.observable.isEmpty shouldBe false
    proxies.get.length shouldBe 3
  }

  it should "be separated correctly with children" in {
    val modifiers: Seq[VDomModifier] = Seq(
      BasicAttr("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input",  _ => ()),
      ModifierStreamReceiver(ValueObservable.empty),
      ModifierStreamReceiver(ValueObservable.empty),
      Emitter("keyup",  _ => ()),
      StringVNode("text"),
      div()
    )
    val streamable = NativeModifiers.from(modifiers)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    emitters.get.values.size shouldBe 3
    attrs.get.values.size shouldBe 1
    streamable.observable.isEmpty shouldBe false
    proxies.get.length shouldBe 2
  }

  it should "be separated correctly with string children" in {
    val modifiers: Seq[VDomModifier] = Seq(
      BasicAttr("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input",  _ => ()),
      Emitter("keyup",  _ => ()),
      ModifierStreamReceiver(ValueObservable.empty),
      ModifierStreamReceiver(ValueObservable.empty),
      StringVNode("text"),
      StringVNode("text2")
    )

    val streamable = NativeModifiers.from(modifiers)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    emitters.get.values.size shouldBe 3
    attrs.get.values.size shouldBe 1
    streamable.observable.isEmpty shouldBe false
    proxies.get.length shouldBe 2
  }

  it should "be separated correctly with children and properties" in {

    val modifiers = Seq(
      BasicAttr("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input", _ => ()),
      UpdateHook((_,_) => ()),
      ModifierStreamReceiver(ValueObservable.empty),
      ModifierStreamReceiver(ValueObservable.empty),
      ModifierStreamReceiver(ValueObservable.empty),
      Emitter("keyup", _ => ()),
      InsertHook(_ => ()),
      PrePatchHook((_,_) => ()),
      PostPatchHook((_,_) => ()),
      StringVNode("text")
    )

    val streamable = NativeModifiers.from(modifiers)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    emitters.get.keys.toList shouldBe List("click", "input", "keyup")
    insertHook.isDefined shouldBe true
    prePatchHook.isDefined shouldBe true
    updateHook.isDefined shouldBe true
    postPatchHook.isDefined shouldBe true
    destroyHook.isDefined shouldBe false
    attrs.get.values.size shouldBe 1
    keyOption.isEmpty shouldBe true
    streamable.observable.isEmpty shouldBe false
    proxies.get.length shouldBe 1
  }

  val fixture = new {
    val proxy = hFunction(
      "div",
      new DataObject {
        attrs = js.Dictionary[Attr.Value]("class" -> "red", "id" -> "msg")
        hook = Hooks.empty
      },
      js.Array(hFunction("span", new DataObject { hook = Hooks.empty }, "Hello"))
    )
  }

  it should "run effect modifiers once" in {
    val list = new collection.mutable.ArrayBuffer[String]

    val vtree = div(
      IO {
        list += "child1"
        ModifierStreamReceiver(ValueObservable(div()))
      },
      IO {
        list += "child2"
        ModifierStreamReceiver(ValueObservable.empty)
      },
      IO {
        list += "children1"
        ModifierStreamReceiver(ValueObservable.empty)
      },
      IO {
        list += "children2"
        ModifierStreamReceiver(ValueObservable.empty)
      },
      div(
        IO {
          list += "attr1"
          BasicAttr("attr1", "peter")
        },
        Seq(
          IO {
            list += "attr2"
            BasicAttr("attr2", "hans")
          }
        )
      )
    )

    val test = for {

      node <- IO {
        val node = document.createElement("div")
        document.body.appendChild(node)
        node
      }

      _ <- IO(list.isEmpty shouldBe true)
      _ <- OutWatch.renderInto(node, vtree)
    } yield {

      list should contain theSameElementsAs List(
        "child1", "child2", "children1", "children2", "attr1", "attr2"
      )

    }

    test
  }

  it should "not provide unique key for child nodes if stream is present" in {
    val mods = Seq(
      ModifierStreamReceiver(ValueObservable.empty),
      div(id := "1"),
      div(id := "2")
      // div(), div() //TODO: this should also work, but key is derived from hashCode of VTree (which in this case is equal)
    )

    val streamable = NativeModifiers.from(mods)
    val seps =  SeparatedModifiers.from(streamable.modifiers)
    import seps._

    proxies.get.length shouldBe 2
    streamable.observable.isEmpty shouldBe false

    val proxy = SnabbdomOps.toSnabbdom(div(mods))
    proxy.key.isDefined shouldBe false

    val key1 = proxy.children.get(0).key
    val key2 = proxy.children.get(1).key

    key1.isDefined shouldBe false
    key2.isDefined shouldBe false
  }

  it should "keep existing key for child nodes" in {
    val mods = Seq(
      Key(1234),
      ModifierStreamReceiver(ValueObservable.empty),
      div()(Key(5678))
    )

    val streamable = NativeModifiers.from(mods)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    proxies.get.length shouldBe 1
    streamable.observable.isEmpty shouldBe false

    val proxy = SnabbdomOps.toSnabbdom(div(mods))
    proxy.key.toOption  shouldBe Some(1234)

    proxy.children.get(0).key.toOption shouldBe Some(5678)
  }

  "VTrees" should "be constructed correctly" in {

    val attributes = List(BasicAttr("class", "red"), BasicAttr("id", "msg"))
    val message = "Hello"
    val child = span(message)
    val vtree = div(attributes.head, attributes(1), child)

    val proxy = fixture.proxy

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(proxy)
  }

  it should "be correctly created with the HyperscriptHelper" in {
    val attributes = List(BasicAttr("class", "red"), BasicAttr("id", "msg"))
    val message = "Hello"
    val child = span(message)
    val vtree = div(attributes.head, attributes(1), child)

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(fixture.proxy)
  }


  it should "run its effect modifiers once!" in {
    Handler.create[String].flatMap { stringHandler =>

      var ioCounter = 0
      var handlerCounter = 0
      stringHandler { _ =>
        handlerCounter += 1
      }

      val vtree = div(
        div(
          IO {
            ioCounter += 1
            BasicAttr("hans", "")
          }
        ),
        stringHandler
      )

      val node = document.createElement("div")
      document.body.appendChild(node)

      ioCounter shouldBe 0
      handlerCounter shouldBe 0


      OutWatch.renderInto(node, vtree).map { _ =>

        ioCounter shouldBe 1
        handlerCounter shouldBe 0
        stringHandler.onNext("pups")
        ioCounter shouldBe 1
        handlerCounter shouldBe 1

      }
    }
  }

  it should "run its effect modifiers once in CompositeModifier!" in {
    Handler.create[String].flatMap { stringHandler =>

      var ioCounter = 0
      var handlerCounter = 0
      stringHandler { _ =>
        handlerCounter += 1
      }

      val vtree = div(
        div(Seq(
          IO {
            ioCounter += 1
            BasicAttr("hans", "")
          }
        )),
        stringHandler
      )

      val node = document.createElement("div")
      document.body.appendChild(node)

      ioCounter shouldBe 0
      handlerCounter shouldBe 0

      OutWatch.renderInto(node, vtree).map { _ =>

        ioCounter shouldBe 1
        handlerCounter shouldBe 0
        stringHandler.onNext("pups")
        ioCounter shouldBe 1
        handlerCounter shouldBe 1

      }

    }
  }

  it should "be correctly patched into the DOM" in {

    val id = "msg"
    val cls = "red"
    val attributes = List(BasicAttr("class", cls), BasicAttr("id", id))
    val message = "Hello"
    val child = span(message)
    val vtree = div(attributes.head, attributes(1), child)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vtree).map { _ =>

          val patchedNode = document.getElementById(id)
          patchedNode.childElementCount shouldBe 1
          patchedNode.classList.contains(cls) shouldBe true
          patchedNode.children(0).innerHTML shouldBe message
      }

    }

  }

  it should "be replaced if they contain changeables" in {

    def page(num: Int): IO[VNode] = for {
      pageNum <- Handler.create[Int](num)
    } yield div( id := "page",
      num match {
        case 1 =>
          div(pageNum)
        case 2 =>
          div(pageNum)
      }
    )


    val pageHandler = PublishSubject[Int]

    val vtree = div(
      div(pageHandler.map(page))
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vtree).map { _ =>

        pageHandler.onNext(1)

        val domNode = document.getElementById("page")

        domNode.textContent shouldBe "1"

        pageHandler.onNext(2)

        domNode.textContent shouldBe "2"
      }

    }
  }

  "The HTML DSL" should "construct VTrees properly" in {
    import outwatch.dom._

    val vtree = div(cls := "red", id := "msg", span("Hello"))

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(fixture.proxy)
  }

  it should "construct VTrees with optional children properly" in {
    import outwatch.dom._

    val vtree = div(cls := "red", id := "msg",
      Option(span("Hello")),
      Option.empty[VDomModifier]
    )

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(fixture.proxy)
  }

  it should "construct VTrees with boolean attributes" in {

    def boolBuilder(name: String) = new BasicAttrBuilder[Boolean](name, identity)
    def stringBuilder(name: String) = new BasicAttrBuilder[Boolean](name, _.toString)

    val vtree = div(
      boolBuilder("a"),
      boolBuilder("b") := true,
      boolBuilder("c") := false,
      stringBuilder("d"),
      stringBuilder("e") := true,
      stringBuilder("f") := false
    )

    val attributes = js.Dictionary[dom.Attr.Value]("a" -> true, "b" -> true, "c" -> false, "d" -> "true", "e" -> "true", "f" -> "false")
    val expected = hFunction("div", new DataObject { attrs = attributes; hook = Hooks.empty })

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree)
    snabbdomNode._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(expected)
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

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap {n =>

      OutWatch.renderInto(n, vtree).map {_ =>

        val patchedNode = document.getElementById("test")

        patchedNode.childElementCount shouldBe 2
        patchedNode.classList.contains("blue") shouldBe true
        patchedNode.children(0).innerHTML shouldBe message
        document.getElementById("list").childElementCount shouldBe 3
      }

    }
  }

  it should "change the value of a textfield" in {

    val messages = PublishSubject[String]
    val vtree = div(
      input(attributes.value <-- messages, id := "input")
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vtree).map { _ =>

        val field = document.getElementById("input").asInstanceOf[html.Input]

        field.value shouldBe ""

        val message = "Hello"
        messages.onNext(message)

        field.value shouldBe message

        val message2 = "World"
        messages.onNext(message2)

        field.value shouldBe message2
      }

    }
  }

  it should "render child nodes in correct order" in {

    val messagesA = PublishSubject[String]
    val messagesB = PublishSubject[String]

    val vNode = div(
      span("A"),
      messagesA.map(span(_)),
      span("B"),
      messagesB.map(span(_))
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        messagesA.onNext("1")
        messagesB.onNext("2")

        n.innerHTML shouldBe "<div><span>A</span><span>1</span><span>B</span><span>2</span></div>"
      }

    }
  }

  it should "render child string-nodes in correct order" in {

    val messagesA = PublishSubject[String]
    val messagesB = PublishSubject[String]
    val vNode = div(
      "A",
      messagesA,
      "B",
      messagesB
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        n.innerHTML shouldBe "<div>AB</div>"

        messagesA.onNext("1")
        n.innerHTML shouldBe "<div>A1B</div>"

        messagesB.onNext("2")
        n.innerHTML shouldBe "<div>A1B2</div>"
      }

    }
  }

  it should "render child string-nodes in correct order, mixed with children" in {

    val messagesA = PublishSubject[String]
    val messagesB = PublishSubject[String]
    val messagesC = PublishSubject[Seq[VNode]]

    val vNode = div(
      "A",
      messagesA,
      messagesC,
      "B",
      messagesB
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        n.innerHTML shouldBe "<div>AB</div>"

        messagesA.onNext("1")
        n.innerHTML shouldBe "<div>A1B</div>"

        messagesB.onNext("2")
        n.innerHTML shouldBe "<div>A1B2</div>"

        messagesC.onNext(Seq(div("5"), div("7")))
        n.innerHTML shouldBe "<div>A1<div>5</div><div>7</div>B2</div>"
      }

    }
  }

  it should "update merged nodes children correctly" in {

    val messages = PublishSubject[Seq[VNode]]
    val otherMessages = PublishSubject[Seq[VNode]]
    val vNode = div(messages)(otherMessages)


    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        otherMessages.onNext(Seq(div("otherMessage")))
        n.children(0).innerHTML shouldBe "<div>otherMessage</div>"

        messages.onNext(Seq(div("message")))
        n.children(0).innerHTML shouldBe "<div>message</div><div>otherMessage</div>"

        otherMessages.onNext(Seq(div("genus")))
        n.children(0).innerHTML shouldBe "<div>message</div><div>genus</div>"
      }

    }
  }

  it should "update merged nodes separate children correctly" in {

    val messages = PublishSubject[String]
    val otherMessages = PublishSubject[String]
    val vNode = div(messages)(otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        n.children(0).innerHTML shouldBe ""

        otherMessages.onNext("otherMessage")
        n.children(0).innerHTML shouldBe "otherMessage"

        messages.onNext("message")
        n.children(0).innerHTML shouldBe "messageotherMessage"

        otherMessages.onNext("genus")
        n.children(0).innerHTML shouldBe "messagegenus"
      }

    }
  }

  it should "partially render component even if parts not present" in {

    val messagesColor = PublishSubject[String]
    val messagesBgColor = PublishSubject[String]
    val childString = PublishSubject[String]

    val vNode = div( id := "inner",
      color <-- messagesColor,
      backgroundColor <-- messagesBgColor,
      childString
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        val inner = document.getElementById("inner").asInstanceOf[html.Div]

        inner.innerHTML shouldBe ""
        inner.style.color shouldBe ""
        inner.style.backgroundColor shouldBe ""

        childString.onNext("fish")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe ""
        inner.style.backgroundColor shouldBe ""

        messagesColor.onNext("red")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe "red"
        inner.style.backgroundColor shouldBe ""

        messagesBgColor.onNext("blue")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe "red"
        inner.style.backgroundColor shouldBe "blue"

      }

    }
  }

  it should "partially render component even if parts not present2" in {

    val messagesColor = PublishSubject[String]
    val childString = PublishSubject[String]

    val vNode = div( id := "inner",
      color <-- messagesColor,
      childString
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        val inner = document.getElementById("inner").asInstanceOf[html.Div]

        inner.innerHTML shouldBe ""
        inner.style.color shouldBe ""

        childString.onNext("fish")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe ""

        messagesColor.onNext("red")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe "red"
      }

    }
  }

  it should "update reused vnodes correctly" in {

    val messages = PublishSubject[String]
    val vNode = div(data.ralf := true, messages)
    val container = div(vNode, vNode)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, container).map { _ =>

        messages.onNext("message")
        n.children(0).children(0).innerHTML shouldBe "message"
        n.children(0).children(1).innerHTML shouldBe "message"

        messages.onNext("bumo")
        n.children(0).children(0).innerHTML shouldBe "bumo"
        n.children(0).children(1).innerHTML shouldBe "bumo"
      }

    }
  }

  it should "update merged nodes correctly (render reuse)" in {

    val messages = PublishSubject[String]
    val otherMessages = PublishSubject[String]
    val vNodeTemplate = div(messages)
    val vNode = vNodeTemplate(otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    val test = for {

      node1 <- node
       _ <- OutWatch.renderInto(node1, vNodeTemplate)

      node2 <- node
       _ <- OutWatch.renderInto(node2, vNode)

    } yield {

      messages.onNext("gurkon")
      otherMessages.onNext("otherMessage")
      node1.children(0).innerHTML shouldBe "gurkon"
      node2.children(0).innerHTML shouldBe "gurkonotherMessage"

      messages.onNext("message")
      node1.children(0).innerHTML shouldBe "message"
      node2.children(0).innerHTML shouldBe "messageotherMessage"

      otherMessages.onNext("genus")
      node1.children(0).innerHTML shouldBe "message"
      node2.children(0).innerHTML shouldBe "messagegenus"
    }

    test
  }

  it should "update merged node attributes correctly" in {

    val messages = PublishSubject[String]
    val otherMessages = PublishSubject[String]
    val vNode = div(data.noise <-- messages)(data.noise <-- otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        otherMessages.onNext("otherMessage")
        n.children(0).getAttribute("data-noise") shouldBe "otherMessage"

        messages.onNext("message") // should be ignored
        n.children(0).getAttribute("data-noise") shouldBe "otherMessage"

        otherMessages.onNext("genus")
        n.children(0).getAttribute("data-noise") shouldBe "genus"
      }

    }
  }

  it should "update merged node styles written with style() correctly" in {

    val messages = PublishSubject[String]
    val otherMessages = PublishSubject[String]
    val vNode = div(style("color") <-- messages)(style("color") <-- otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        otherMessages.onNext("red")
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

        messages.onNext("blue") // should be ignored
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

        otherMessages.onNext("green")
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "green"
      }

    }
  }

  it should "update merged node styles correctly" in {

    val messages = PublishSubject[String]
    val otherMessages = PublishSubject[String]
    val vNode = div(color <-- messages)(color <-- otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        otherMessages.onNext("red")
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

        messages.onNext("blue") // should be ignored
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

        otherMessages.onNext("green")
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "green"
      }

    }
  }


  it should "render composite VNodes properly" in {

    val items = Seq("one", "two", "three")
    val vNode = div(items.map(item => span(item)))

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        n.innerHTML shouldBe "<div><span>one</span><span>two</span><span>three</span></div>"
      }

    }
  }

  it should "render nodes with only attribute receivers properly" in {

    val classes = PublishSubject[String]
    val vNode = button( className <-- classes, "Submit")

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        classes.onNext("active")

        n.innerHTML shouldBe """<button class="active">Submit</button>"""
      }

    }
  }

  it should "work with custom tags" in {

    val vNode = div(htmlTag("main")())

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      OutWatch.renderInto(n, vNode).map { _ =>

        n.innerHTML shouldBe "<div><main></main></div>"
      }

    }
  }

  it should "work with un-assigned booleans attributes and props" in {

    val vNode = option(selected, disabled)

    val node = IO {
      val node = document.createElement("option").asInstanceOf[html.Option]
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      n.selected shouldBe false
      n.disabled shouldBe false

      OutWatch.renderReplace(n, vNode).map {_ =>

        n.selected shouldBe true
        n.disabled shouldBe true
      }

    }
  }

  it should "correctly work with AsVDomModifier conversions" in {

    val test: IO[Assertion] = for {
      n1 <- IO(document.createElement("div"))

       _ <- OutWatch.renderReplace(n1, div("one"))
       _ = n1.innerHTML shouldBe "one"

       _ <- OutWatch.renderReplace(n1, div(Some("one")))
       _ = n1.innerHTML shouldBe "one"

      n2 <- IO(document.createElement("div"))

       _ <- OutWatch.renderReplace(n2, div(None:Option[Int]))
       _ = n2.innerHTML shouldBe ""

      _ <- OutWatch.renderReplace(n1, div(1))
      _ = n1.innerHTML shouldBe "1"

      _ <- OutWatch.renderReplace(n1, div(1.0))
      _ = n1.innerHTML shouldBe "1"

      _ <- OutWatch.renderReplace(n1, div(Seq("one", "two")))
      _ = n1.innerHTML shouldBe "onetwo"

      _ <- OutWatch.renderReplace(n1, div(Seq(1, 2)))
      _ = n1.innerHTML shouldBe "12"

      _ <- OutWatch.renderReplace(n1, div(Seq(1.0, 2.0)))
      _ = n1.innerHTML shouldBe "12"

    } yield succeed

    test
  }

  "Children stream" should "work for string sequences" in {

    val myStrings: Observable[Seq[String]] = Observable(Seq("a", "b"))
    val node = div(id := "strings", ValueObservable.from(myStrings))

    OutWatch.renderInto("#app", node).map { _ =>

      val element = document.getElementById("strings")
      element.innerHTML shouldBe "ab"

    }
  }

  "Children stream" should "work for double/boolean/long/int" in {

    val node = div(id := "strings",
      1.1, true, 133L, 7
    )

    OutWatch.renderInto("#app", node).map { _ =>

      val element = document.getElementById("strings")
      element.innerHTML shouldBe "1.1true1337"

    }
  }

  "Child stream" should "work for string options" in {

    Handler.create(Option("a")).flatMap { myOption =>

      val node = div(id := "strings", myOption)

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "a"

        myOption.onNext(None)
        element.innerHTML shouldBe ""

      }

    }
  }

  it should "work for vnode options" in {

    Handler.create(Option(div("a"))).flatMap { myOption =>

      val node = div(id := "strings", myOption)

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div>a</div>"

        myOption.onNext(None)
        element.innerHTML shouldBe ""

      }

    }
  }

  "Modifier stream" should "work for modifier" in {

    Handler.create[VDomModifier](Seq(cls := "hans", b("stark"))).flatMap { myHandler =>

      val node = div(id := "strings",
        div(VDomModifier(myHandler))
      )

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe """<div class="hans"><b>stark</b></div>"""

        myHandler.onNext(Option(id := "fair"))
        element.innerHTML shouldBe """<div id="fair"></div>"""

      }

    }
  }

  it should "work for multiple mods" in {

    val test: IO[Assertion] = Handler.create[VDomModifier].flatMap { myHandler =>

      val node = div(id := "strings",
        div(myHandler, "bla")
      )

      OutWatch.renderInto("#app", node).flatMap { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div>bla</div>"

        myHandler.onNext(cls := "hans")
        element.innerHTML shouldBe """<div class="hans">bla</div>"""

        Handler.create[VDomModifier].map { innerHandler =>

          myHandler.onNext(div(
            innerHandler,
            cls := "no?",
            "yes?"
          ))
          element.innerHTML shouldBe """<div><div class="no?">yes?</div>bla</div>"""

          innerHandler.onNext(Seq(span("question:"), id := "heidi"))
          element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question:</span>yes?</div>bla</div>"""

          myHandler.onNext(div(
            innerHandler,
            cls := "no?",
            "yes?",
            b("go!")
          ))

          element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question:</span>yes?<b>go!</b></div>bla</div>"""

          innerHandler.onNext(Seq(span("question and answer:"), id := "heidi"))
          element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question and answer:</span>yes?<b>go!</b></div>bla</div>"""

          myHandler.onNext(Seq(span("nope")))
          element.innerHTML shouldBe """<div><span>nope</span>bla</div>"""

          innerHandler.onNext(b("me?"))
          element.innerHTML shouldBe """<div><span>nope</span>bla</div>"""

        }
      }

    }

    test
  }

  it should "work for nested modifier stream receiver" in {

    Handler.create[VDomModifier].flatMap { myHandler =>

      val node = div(id := "strings",
        div(myHandler)
      )

      OutWatch.renderInto("#app", node).flatMap { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        Handler.create[VDomModifier].flatMap { innerHandler =>

          myHandler.onNext(innerHandler)
          element.innerHTML shouldBe """<div></div>"""

          innerHandler.onNext(VDomModifier(cls := "hans", "1"))
          element.innerHTML shouldBe """<div class="hans">1</div>"""

          Handler.create[VDomModifier].map { innerHandler2 =>

          myHandler.onNext(innerHandler2)
          element.innerHTML shouldBe """<div></div>"""

          myHandler.onNext(IO.pure(CompositeModifier(ModifierStreamReceiver(innerHandler2) :: Nil)))
          element.innerHTML shouldBe """<div></div>"""

          myHandler.onNext(CompositeModifier(ModifierStreamReceiver(innerHandler2) :: Nil))

          myHandler.onNext(CompositeModifier(StringVNode("pete") :: ModifierStreamReceiver(innerHandler2) :: Nil))
          element.innerHTML shouldBe """<div>pete</div>"""

          innerHandler2.onNext(VDomModifier(id := "dieter", "r"))
          element.innerHTML shouldBe """<div id="dieter">peter</div>"""

          innerHandler.onNext(b("me?"))
          element.innerHTML shouldBe """<div id="dieter">peter</div>"""

          myHandler.onNext(span("the end"))
          element.innerHTML shouldBe """<div><span>the end</span></div>"""

          }
        }
      }

    }
  }

  it should "work for nested modifier stream receiver and default value" in {

    var numPatches = 0

    Handler.create[VDomModifier].flatMap { myHandler =>

      val node: VNode = div(id := "strings",
        div(
          onSnabbdomPrePatch foreach { numPatches += 1 },
          myHandler.prepend(VDomModifier("initial"))
        )
      )

      OutWatch.renderInto("#app", node).flatMap { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div>initial</div>"
        numPatches shouldBe 0

      Handler.create[VDomModifier].flatMap { innerHandler =>

        myHandler.onNext(innerHandler.prepend(BasicAttr("initial", "2")))
        element.innerHTML shouldBe """<div initial="2"></div>"""
        numPatches shouldBe 1

        innerHandler.onNext(BasicAttr("attr", "3"))
        element.innerHTML shouldBe """<div attr="3"></div>"""
        numPatches shouldBe 2

        Handler.create[VDomModifier].map {innerHandler2 =>
          myHandler.onNext(innerHandler2.prepend(VDomModifier("initial3")))
          element.innerHTML shouldBe """<div>initial3</div>"""
          numPatches shouldBe 3

          myHandler.onNext(IO.pure(CompositeModifier(ModifierStreamReceiver(innerHandler2.prepend(VDomModifier("initial4"))) :: Nil)))
          element.innerHTML shouldBe """<div>initial4</div>"""
          numPatches shouldBe 4

          myHandler.onNext(IO.pure(CompositeModifier(StringVNode("pete") :: ModifierStreamReceiver(innerHandler2) :: Nil)))
          element.innerHTML shouldBe """<div>pete</div>"""
          numPatches shouldBe 5

          innerHandler2.onNext(VDomModifier(id := "dieter", "r"))
          element.innerHTML shouldBe """<div id="dieter">peter</div>"""
          numPatches shouldBe 6

          innerHandler.onNext("me?")
          element.innerHTML shouldBe """<div id="dieter">peter</div>"""
          numPatches shouldBe 6

          myHandler.onNext(span("the end"))
          element.innerHTML shouldBe """<div><span>the end</span></div>"""
          numPatches shouldBe 7
      }}}}
  }

  it should "work for deeply nested handlers" in {

    val test: IO[Assertion] = Handler.create[Int](0).flatMap { a =>

      val b = a.map(_.toString)
      val node =
        div(
          a.map(_ =>
              div(
                b.map(b =>
                  div(b)
                )
              )
          )
        )

      OutWatch.renderInto("#app", node).map {_ =>

        val element = document.getElementById("app")
        element.innerHTML shouldBe "<div><div><div>0</div></div></div>"

        a.onNext(1)
        element.innerHTML shouldBe "<div><div><div>1</div></div></div>"

      }
    }

    test
  }

  it should "work for nested modifier stream receiver and empty default and start with" in {

    var numPatches = 0

    Handler.create[VDomModifier]("initial").flatMap { myHandler =>

    val node = div(id := "strings",
      div(
        onSnabbdomPrePatch foreach { numPatches += 1 },
        myHandler
      )
    )

    OutWatch.renderInto("#app", node).flatMap { _ =>

      val element = document.getElementById("strings")
      element.innerHTML shouldBe "<div>initial</div>"
      numPatches shouldBe 0

      Handler.create[VDomModifier].flatMap { innerHandler =>
      myHandler.onNext(innerHandler.startWith(BasicAttr("initial", "2") :: Nil))
      element.innerHTML shouldBe """<div initial="2"></div>"""
      numPatches shouldBe 1

      innerHandler.onNext(BasicAttr("attr", "3"))
      element.innerHTML shouldBe """<div attr="3"></div>"""
      numPatches shouldBe 2

        Handler.create[VDomModifier].map { innerHandler2 =>
        myHandler.onNext(innerHandler2.startWith(VDomModifier("initial3") :: Nil))
        element.innerHTML shouldBe """<div>initial3</div>"""
        numPatches shouldBe 3

        myHandler.onNext(IO.pure(CompositeModifier(ModifierStreamReceiver(innerHandler2.startWith(VDomModifier("initial4") :: Nil)) :: Nil)))
        element.innerHTML shouldBe """<div>initial4</div>"""
        numPatches shouldBe 4

        myHandler.onNext(IO.pure(CompositeModifier(StringVNode("pete") :: ModifierStreamReceiver(innerHandler2) :: Nil)))
        element.innerHTML shouldBe """<div>pete</div>"""
        numPatches shouldBe 5

        innerHandler2.onNext(VDomModifier(id := "dieter", "r"))
        element.innerHTML shouldBe """<div id="dieter">peter</div>"""
        numPatches shouldBe 6

        innerHandler.onNext("me?")
        element.innerHTML shouldBe """<div id="dieter">peter</div>"""
        numPatches shouldBe 6

        myHandler.onNext(span("the end"))
        element.innerHTML shouldBe """<div><span>the end</span></div>"""
        numPatches shouldBe 7

    }}}}
  }

  it should "work for modifier stream receiver and streaming default value (subscriptions are canceled properly)" in {

    Handler.create[VDomModifier].flatMap { myHandler =>
    Handler.create[VDomModifier].flatMap { innerHandler =>

      val outerTriggers = new scala.collection.mutable.ArrayBuffer[VDomModifier]
      val innerTriggers = new scala.collection.mutable.ArrayBuffer[VDomModifier]

      val node = div(id := "strings",
        div(
          myHandler.map { x => outerTriggers += x; x }.prepend(
            VDomModifier(innerHandler.map { x => innerTriggers += x; x }.prepend(VDomModifier("initial")))
          )
        )
      )

      OutWatch.renderInto("#app", node).flatMap {_ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div>initial</div>"
        outerTriggers.size shouldBe 0
        innerTriggers.size shouldBe 0

        innerHandler.onNext(VDomModifier("hi!"))
        element.innerHTML shouldBe """<div>hi!</div>"""
        outerTriggers.size shouldBe 0
        innerTriggers.size shouldBe 1

        myHandler.onNext(VDomModifier("test"))
        element.innerHTML shouldBe """<div>test</div>"""
        outerTriggers.size shouldBe 1
        innerTriggers.size shouldBe 1

        myHandler.onNext(ValueObservable(BasicAttr("initial", "2")))
        element.innerHTML shouldBe """<div initial="2"></div>"""
        outerTriggers.size shouldBe 2
        innerTriggers.size shouldBe 1

        innerHandler.onNext(VDomModifier("me?"))
        element.innerHTML shouldBe """<div initial="2"></div>"""
        outerTriggers.size shouldBe 2
        innerTriggers.size shouldBe 1

        myHandler.onNext(innerHandler.map { x => innerTriggers += x; x }.prepend(VDomModifier.empty))
        element.innerHTML shouldBe """<div>me?</div>"""
        outerTriggers.size shouldBe 3
        innerTriggers.size shouldBe 2

        innerHandler.onNext(BasicAttr("attr", "3"))
        element.innerHTML shouldBe """<div attr="3"></div>"""
        outerTriggers.size shouldBe 3
        innerTriggers.size shouldBe 3

        val innerTriggers2 = new scala.collection.mutable.ArrayBuffer[VDomModifier]
        val innerTriggers3 = new scala.collection.mutable.ArrayBuffer[VDomModifier]

        Handler.create[VDomModifier].flatMap { innerHandler2 =>
        Handler.create[VDomModifier].map { innerHandler3 =>

            innerHandler.onNext(innerHandler2.map { x => innerTriggers2 += x; x }.prepend(VDomModifier(innerHandler3.map { x => innerTriggers3 += x; x })))
            element.innerHTML shouldBe """<div></div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 4
            innerTriggers2.size shouldBe 0
            innerTriggers3.size shouldBe 0

            innerHandler2.onNext(VDomModifier("2"))
            element.innerHTML shouldBe """<div>2</div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 4
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            innerHandler.onNext(IO.pure(EmptyModifier))
            element.innerHTML shouldBe """<div></div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            innerHandler2.onNext(VDomModifier("me?"))
            element.innerHTML shouldBe """<div></div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            innerHandler3.onNext(VDomModifier("me?"))
            element.innerHTML shouldBe """<div></div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            myHandler.onNext(VDomModifier("go away"))
            element.innerHTML shouldBe """<div>go away</div>"""
            outerTriggers.size shouldBe 4
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            innerHandler.onNext(VDomModifier("me?"))
            element.innerHTML shouldBe """<div>go away</div>"""
            outerTriggers.size shouldBe 4
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

      }}}}}
  }

  it should "work for nested observables with seq modifiers " in {

    Handler.create("b").flatMap { innerHandler =>
    Handler.create(Seq[VDomModifier]("a", data.test := "v", innerHandler)).flatMap { outerHandler =>

      val node = div(
        id := "strings",
        outerHandler
      )

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.outerHTML shouldBe """<div id="strings" data-test="v">ab</div>"""

        innerHandler.onNext("c")
        element.outerHTML shouldBe """<div id="strings" data-test="v">ac</div>"""

        outerHandler.onNext(Seq[VDomModifier]("meh"))
        element.outerHTML shouldBe """<div id="strings">meh</div>"""
      }

    }}
  }

  it should "work for nested observables with seq modifiers and attribute stream" in {

    Handler.create[String].flatMap { innerHandler =>
    Handler.create(Seq[VDomModifier]("a", data.test := "v", href <-- innerHandler)).flatMap { outerHandler =>

      val node = div(
        id := "strings",
        outerHandler
      )

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.outerHTML shouldBe """<div id="strings" data-test="v">a</div>"""

        innerHandler.onNext("c")
        element.outerHTML shouldBe """<div id="strings" data-test="v" href="c">a</div>"""

        innerHandler.onNext("d")
        element.outerHTML shouldBe """<div id="strings" data-test="v" href="d">a</div>"""

        outerHandler.onNext(Seq[VDomModifier]("meh"))
        element.outerHTML shouldBe """<div id="strings">meh</div>"""
      }

    }}
  }

  it should "work for double nested modifier stream receiver" in {

    Handler.create[VDomModifier].flatMap { myHandler =>

      val node = div(id := "strings",
        div(myHandler)
      )

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        myHandler.onNext(Observable[VDomModifier](Observable[VDomModifier](cls := "hans")))
        element.innerHTML shouldBe """<div class="hans"></div>"""
      }

    }
  }

  it should "work for triple nested modifier stream receiver" in {

    Handler.create[VDomModifier].flatMap { myHandler =>

      val node = div(id := "strings",
        div(myHandler)
      )

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        myHandler.onNext(Observable[VDomModifier](Observable[VDomModifier](Observable(cls := "hans"))))
        element.innerHTML shouldBe """<div class="hans"></div>"""
      }

    }
  }

  it should "work for multiple nested modifier stream receiver" in {

    Handler.create[VDomModifier].flatMap { myHandler =>

      val node = div(id := "strings",
        div(myHandler)
      )

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        myHandler.onNext(Observable[VDomModifier](VDomModifier(Observable[VDomModifier]("a"), Observable(span("b")))))
        element.innerHTML shouldBe """<div>a<span>b</span></div>"""
      }

    }
  }

  it should "work for nested attribute stream receiver" in {

    Handler.create[VDomModifier].flatMap { myHandler =>

      val node = div(id := "strings",
        div(myHandler)
      )

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        myHandler.onNext(cls <-- Observable("hans"))
        element.innerHTML shouldBe """<div class="hans"></div>"""
      }

    }
  }

  it should "work for nested emitter" in {

    Handler.create[VDomModifier].flatMap { myHandler =>

      val node = div(id := "strings",
        div(id := "click", myHandler)
      )

      OutWatch.renderInto("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe """<div id="click"></div>"""

        var clickCounter = 0
        myHandler.onNext(onClick foreach (_ => clickCounter += 1))
        element.innerHTML shouldBe """<div id="click"></div>"""

        clickCounter shouldBe 0
        sendEvent(document.getElementById("click"), "click")
        clickCounter shouldBe 1

        sendEvent(document.getElementById("click"), "click")
        clickCounter shouldBe 2

        myHandler.onNext(VDomModifier.empty)
        element.innerHTML shouldBe """<div id="click"></div>"""

        sendEvent(document.getElementById("click"), "click")
        clickCounter shouldBe 2
      }

    }
  }

  it should "work for streaming accum attributes" in {

    Handler.create[String]("second").flatMap { myClasses =>
    Handler.create[String].flatMap { myClasses2 =>

      val node = div(
        id := "strings",
        div(
          cls := "first",
          myClasses.map { cls := _ },
          Seq[VDomModifier](
            cls <-- myClasses2
          )
        )
      )

      OutWatch.renderInto("#app", node).map {_ =>
        val element = document.getElementById("strings")

        element.innerHTML shouldBe """<div class="first second"></div>"""

        myClasses2.onNext("third")
        element.innerHTML shouldBe """<div class="first second third"></div>"""

        myClasses2.onNext("more")
        element.innerHTML shouldBe """<div class="first second more"></div>"""

        myClasses.onNext("yeah")
        element.innerHTML shouldBe """<div class="first yeah more"></div>"""
      }

    }}
  }

  "LocalStorage" should "provide a handler" in {

    val key = "banana"
    val triggeredHandlerEvents = mutable.ArrayBuffer.empty[Option[String]]

    assert(localStorage.getItem(key) == null)

    util.LocalStorage.handler(key).flatMap { storageHandler =>

      storageHandler.foreach{e => triggeredHandlerEvents += e}
      assert(localStorage.getItem(key) == null)
      assert(triggeredHandlerEvents.toList == List(None))

      storageHandler.onNext(Some("joe"))
      assert(localStorage.getItem(key) == "joe")
      assert(triggeredHandlerEvents.toList == List(None, Some("joe")))

      var initialValue:Option[String] = null

      util.LocalStorage.handler(key).map { sh =>

        sh.foreach {initialValue = _}
        assert(initialValue == Some("joe"))

        storageHandler.onNext(None)
        assert(localStorage.getItem(key) == null)
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None))

        // localStorage.setItem(key, "split") from another window
        dispatchStorageEvent(key, newValue = "split", null)
        assert(localStorage.getItem(key) == "split")
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None, Some("split")))

        // localStorage.removeItem(key) from another window
        dispatchStorageEvent(key, null, "split")
        assert(localStorage.getItem(key) == null)
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None, Some("split"), None))

        // only trigger handler if value changed
        storageHandler.onNext(None)
        assert(localStorage.getItem(key) == null)
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None, Some("split"), None))

        storageHandler.onNext(Some("rhabarbar"))
        assert(localStorage.getItem(key) == "rhabarbar")
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None, Some("split"), None, Some("rhabarbar")))

        // localStorage.clear() from another window
        dispatchStorageEvent(null, null, null)
        assert(localStorage.getItem(key) == null)
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None, Some("split"), None, Some("rhabarbar"), None))
      }
    }
  }

  "Observer/Observable types" should "work for Subject" in {

    var mounts = 0
    var unmounts = 0
    var updates = 0

    for {
       myHandler <- IO(BehaviorSubject(-1))
      clsHandler <- IO(BehaviorSubject("one"))
            node = div(
                      id := "strings",
                      myHandler,
                      cls <-- clsHandler,
                      onClick(0) --> myHandler,
                      onDomMount foreach  { mounts += 1 },
                      onDomUnmount foreach  { unmounts += 1 },
                      onDomUpdate foreach  { updates += 1 }
                    )
          _ <- OutWatch.renderInto("#app", node)
    } yield {

      val element = document.getElementById("strings")
      element.outerHTML shouldBe """<div id="strings" class="one">-1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 2

      myHandler.onNext(1)
      element.outerHTML shouldBe """<div id="strings" class="one">1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 3

      sendEvent(document.getElementById("strings"), "click")

      element.outerHTML shouldBe """<div id="strings" class="one">0</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 4

      clsHandler.onNext("two")
      element.outerHTML shouldBe """<div id="strings" class="two">0</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 5
    }
  }

  it should "work for Var" in {

    var mounts = 0
    var unmounts = 0
    var updates = 0

    for {
       myHandler <- IO(Var(-1))
      clsHandler <- IO(Var("one"))
            node = div(
                    id := "strings",
                    myHandler,
                    cls <-- clsHandler,
                    onClick(0) --> myHandler,
                    onDomMount foreach  { mounts += 1 },
                    onDomUnmount foreach  { unmounts += 1 },
                    onDomUpdate foreach  { updates += 1 }
                  )
                _ <- OutWatch.renderInto("#app", node)
    } yield {
      val element = document.getElementById("strings")
      element.outerHTML shouldBe """<div id="strings" class="one">-1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 0

      myHandler := 1
      element.outerHTML shouldBe """<div id="strings" class="one">1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 1

      sendEvent(document.getElementById("strings"), "click")

      element.outerHTML shouldBe """<div id="strings" class="one">0</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 2

      clsHandler := "two"
      element.outerHTML shouldBe """<div id="strings" class="two">0</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 3
    }
  }

  "ChildCommand" should "work in handler" in {
    val cmds = Handler.unsafe[ChildCommand]

    val node = div(
      id := "strings",
      cmds
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")
      element.innerHTML shouldBe ""

      cmds.onNext(ChildCommand.ReplaceAll(b("Hello World") :: span("!") :: Nil))
      element.innerHTML shouldBe """<b>Hello World</b><span>!</span>"""

      cmds.onNext(ChildCommand.Insert(1, p("and friends", dsl.key := 42)))
      element.innerHTML shouldBe """<b>Hello World</b><p>and friends</p><span>!</span>"""

      cmds.onNext(ChildCommand.Move(1, 2))
      element.innerHTML shouldBe """<b>Hello World</b><span>!</span><p>and friends</p>"""

      cmds.onNext(ChildCommand.MoveId(ChildId.Key(42), 1))
      element.innerHTML shouldBe """<b>Hello World</b><p>and friends</p><span>!</span>"""

      cmds.onNext(ChildCommand.RemoveId(ChildId.Key(42)))
      element.innerHTML shouldBe """<b>Hello World</b><span>!</span>"""
    }
  }

  it should "work in handler with element reference" in {
    val initialChildren =
      p(
        "How much?",
        dsl.key := "question-1",
        id := "id-1"
      ) ::
        p(
          "Why so cheap?",
          dsl.key := "question-2",
          id := "id-2"

        ) ::
        Nil

    val cmds = Handler.unsafe[ChildCommand](ChildCommand.ReplaceAll(initialChildren))

    val node = div(
      "Questions?",
      div(
        id := "strings",
        onClick.map(ev => ChildCommand.RemoveId(ChildId.Element(ev.target))) --> cmds,
        cmds
      )
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")
      element.innerHTML shouldBe """<p id="id-1">How much?</p><p id="id-2">Why so cheap?</p>"""

      sendEvent(document.getElementById("id-1"), "click")

      element.innerHTML shouldBe """<p id="id-2">Why so cheap?</p>"""

      cmds.onNext(ChildCommand.InsertBeforeId(ChildId.Key("question-2"), div("spam")))
      element.innerHTML shouldBe """<div>spam</div><p id="id-2">Why so cheap?</p>"""

      cmds.onNext(ChildCommand.MoveId(ChildId.Key("question-2"), 0))
      element.innerHTML shouldBe """<p id="id-2">Why so cheap?</p><div>spam</div>"""

      sendEvent(document.getElementById("id-2"), "click")
      element.innerHTML shouldBe """<div>spam</div>"""
    }
  }

  it should "work in value observable" in {
    val cmds = Handler.unsafe[ChildCommand]

    val node = div(
      id := "strings",
      cmds.prepend(ChildCommand.ReplaceAll(div("huch") :: Nil))
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")
      element.innerHTML shouldBe "<div>huch</div>"

      cmds.onNext(ChildCommand.Prepend(b("nope")))
      element.innerHTML shouldBe """<b>nope</b><div>huch</div>"""

      cmds.onNext(ChildCommand.Move(0, 1))
      element.innerHTML shouldBe """<div>huch</div><b>nope</b>"""

      cmds.onNext(ChildCommand.Replace(1, b("yes")))
      element.innerHTML shouldBe """<div>huch</div><b>yes</b>"""
    }
  }

  "Emitter subscription" should "be correctly subscribed" in {

    val clicks = Handler.unsafe[Int](0)

    var incCounter =  0
    var mapCounter = 0
    val innerMod  = onClick.transform(_ => clicks.observable.map { c => mapCounter += 1; c }) foreach { incCounter += 1 }
    val modHandler = Handler.unsafe[VDomModifier](innerMod)

    val innerNode = div(modHandler)
    val nodeHandler = Handler.unsafe[VNode](innerNode)

    val node = div(
      id := "strings",
      nodeHandler
    )

    incCounter shouldBe 0
    mapCounter shouldBe 0

    OutWatch.renderInto("#app", node).map { _ =>
      incCounter shouldBe 1
      mapCounter shouldBe 1

      clicks.onNext(1)
      incCounter shouldBe 2
      mapCounter shouldBe 2

      modHandler.onNext(VDomModifier.empty)
      incCounter shouldBe 2
      mapCounter shouldBe 2

      clicks.onNext(2)
      incCounter shouldBe 2
      mapCounter shouldBe 2

      modHandler.onNext(innerMod)
      incCounter shouldBe 3
      mapCounter shouldBe 3

      clicks.onNext(3)
      incCounter shouldBe 4
      mapCounter shouldBe 4

      nodeHandler.onNext(span)
      incCounter shouldBe 4
      mapCounter shouldBe 4

      clicks.onNext(4)
      incCounter shouldBe 4
      mapCounter shouldBe 4
    }
  }

  it should "be correctly subscribed for emitterbuilder.useLatest" in {

    var aCounter =  0
    var bCounter = 0
    var lastValue: String = null

    val aEvent = PublishSubject[String]
    val bEvent = PublishSubject[String]
    val innerNode =
      input(emitter(aEvent).map { x => aCounter += 1; x } .useLatest(emitter(bEvent).map { x => bCounter += 1; x }) foreach { str =>
        lastValue = str
      })

    val handler = Handler.unsafe[VDomModifier](innerNode)
    val node = div(
      id := "strings",
      handler
    )

    aEvent.onNext("nope?")
    bEvent.onNext("nope?")
    aCounter shouldBe 0
    bCounter shouldBe 0
    lastValue shouldBe null

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")
      val child = element.children(0)
      aCounter shouldBe 0
      bCounter shouldBe 0
      lastValue shouldBe null

      aEvent.onNext("a")
      aCounter shouldBe 1
      bCounter shouldBe 0
      lastValue shouldBe null

      bEvent.onNext("b")
      aCounter shouldBe 1
      bCounter shouldBe 1
      lastValue shouldBe null

      aEvent.onNext("a2")
      aCounter shouldBe 2
      bCounter shouldBe 1
      lastValue shouldBe "b"

      bEvent.onNext("ahja")
      aCounter shouldBe 2
      bCounter shouldBe 2
      lastValue shouldBe "b"

      aEvent.onNext("oh")
      aCounter shouldBe 3
      bCounter shouldBe 2
      lastValue shouldBe "ahja"

      handler.onNext(VDomModifier.empty)

      aEvent.onNext("hmm?")
      aCounter shouldBe 3
      bCounter shouldBe 2
      lastValue shouldBe "ahja"

      bEvent.onNext("no?")
      aCounter shouldBe 3
      bCounter shouldBe 2
      lastValue shouldBe "ahja"

    }
  }

  "Thunk" should "work" in {
    val myString: Handler[String] = Handler.unsafe[String]

    var mountCount = 0
    var preupdateCount = 0
    var updateCount = 0
    var unmountCount = 0
    var renderFnCounter = 0
    val node = div(
      id := "strings",
      myString.map { myString =>
        b(id := "bla").thunk("component")(myString){
          renderFnCounter += 1
          VDomModifier(cls := "b", myString, onDomMount.foreach { mountCount += 1 }, onDomPreUpdate.foreach { preupdateCount += 1 }, onDomUpdate.foreach { updateCount += 1 }, onDomUnmount.foreach { unmountCount += 1 })
        }
      },
      b("something else")
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      mountCount shouldBe 0
      preupdateCount shouldBe 0
      updateCount shouldBe 0
      unmountCount shouldBe 0
      element.innerHTML shouldBe "<b>something else</b>"

      myString.onNext("wal?")
      renderFnCounter shouldBe 1
      mountCount shouldBe 1
      preupdateCount shouldBe 0
      updateCount shouldBe 0
      unmountCount shouldBe 0
      element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      myString.onNext("wal?")
      renderFnCounter shouldBe 1
      mountCount shouldBe 1
      preupdateCount shouldBe 1
      updateCount shouldBe 1
      unmountCount shouldBe 0
      element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      myString.onNext("hai!")
      renderFnCounter shouldBe 2
      mountCount shouldBe 2
      preupdateCount shouldBe 1
      updateCount shouldBe 1
      unmountCount shouldBe 1
      element.innerHTML shouldBe """<b id="bla" class="b">hai!</b><b>something else</b>"""

      myString.onNext("fuchs.")
      renderFnCounter shouldBe 3
      mountCount shouldBe 3
      preupdateCount shouldBe 1
      updateCount shouldBe 1
      unmountCount shouldBe 2
      element.innerHTML shouldBe """<b id="bla" class="b">fuchs.</b><b>something else</b>"""
    }
  }

  it should "work with equals" in {
    val myString: Handler[String] = Handler.unsafe[String]

    var equalsCounter = 0
    class Wrap(val s: String) {
      override def equals(other: Any) = {
        equalsCounter += 1
        other match {
          case w: Wrap => s == w.s
          case _ => false
        }
      }
    }

    var renderFnCounter = 0
    val node = div(
      id := "strings",
      myString.map { myString =>
        b(id := "bla").thunk("component")(new Wrap(myString)) {
          renderFnCounter += 1
          Seq[VDomModifier](cls := "b", myString)
        }
      },
      b("something else")
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      equalsCounter shouldBe 0
      element.innerHTML shouldBe "<b>something else</b>"

      myString.onNext("wal?")
      renderFnCounter shouldBe 1
      equalsCounter shouldBe 0
      element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      myString.onNext("wal?")
      renderFnCounter shouldBe 1
      equalsCounter shouldBe 1
      element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      myString.onNext("hai!")
      renderFnCounter shouldBe 2
      equalsCounter shouldBe 2
      element.innerHTML shouldBe """<b id="bla" class="b">hai!</b><b>something else</b>"""
    }
  }

  it should "work with inner stream" in {
    val myString: Handler[String] = Handler.unsafe[String]
    val myThunk: Handler[Unit] = Handler.unsafe[Unit]

    var renderFnCounter = 0
    val node = div(
      id := "strings",
      myThunk.map { _ =>
        b.static("component") {
          renderFnCounter += 1
          myString.map { str =>
            p(dsl.key := str, str)
          }
        }
      }
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      element.innerHTML shouldBe ""

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b></b>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b></b>"

      myString.onNext("ok?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b><p>ok?</p></b>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b><p>ok?</p></b>"

      myString.onNext("hope so")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b><p>hope so</p></b>"

      myString.onNext("ohai")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b><p>ohai</p></b>"
    }
  }

  it should "work with inner and adjacent stream" in {
    val myString: Handler[String] = Handler.unsafe[String]
    val myOther: Handler[String] = Handler.unsafe[String]
    val myThunk: Handler[Int] = Handler.unsafe[Int]

    var renderFnCounter = 0
    val node = div(
      id := "strings",
      myThunk.map { i =>
        b.thunk("component")(i) {
          renderFnCounter += 1
          VDomModifier(
            i,
            myString.map { str =>
              if (str.isEmpty) span("nope")
              else p(dsl.key := str, str)
            }
          )
        }
      },
      myOther
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      element.innerHTML shouldBe ""

      myThunk.onNext(0)
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0</b>"

      myThunk.onNext(0)
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0</b>"

      myString.onNext("ok?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0<p>ok?</p></b>"

      myString.onNext("got it?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0<p>got it?</p></b>"

      myOther.onNext("nope")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0<p>got it?</p></b>nope"

      myThunk.onNext(1)
      renderFnCounter shouldBe 2
      element.innerHTML shouldBe "<b>1<p>got it?</p></b>nope"

      myThunk.onNext(2)
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>got it?</p></b>nope"

      myOther.onNext("yes")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>got it?</p></b>yes"

      myString.onNext("sad?")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>sad?</p></b>yes"

      myString.onNext("hungry?")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>hungry?</p></b>yes"

      myString.onNext("thursty?")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>thursty?</p></b>yes"

      myOther.onNext("maybe")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>thursty?</p></b>maybe"

      myThunk.onNext(3)
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<p>thursty?</p></b>maybe"

      myString.onNext("")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<span>nope</span></b>maybe"

      myOther.onNext("come on")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<span>nope</span></b>come on"

      myThunk.onNext(3)
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<span>nope</span></b>come on"

      myOther.onNext("hey?")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<span>nope</span></b>hey?"

      myString.onNext("oh")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<p>oh</p></b>hey?"

      myOther.onNext("ah")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<p>oh</p></b>ah"

      myThunk.onNext(3)
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<p>oh</p></b>ah"

      myThunk.onNext(4)
      renderFnCounter shouldBe 5
      element.innerHTML shouldBe "<b>4<p>oh</p></b>ah"
    }
  }

  it should "work with nested inner stream" in {
    val myString: Handler[String] = Handler.unsafe[String]
    val myInner: Handler[String] = Handler.unsafe[String]
    val myInnerOther: Handler[String] = Handler.unsafe[String]
    val myOther: Handler[String] = Handler.unsafe[String]
    val myThunk: Handler[Unit] = Handler.unsafe[Unit]

    var renderFnCounter = 0
    val node = div(
      id := "strings",
      myThunk.map { _ =>
        div(
          b.static("component") {
            renderFnCounter += 1
            myString.map { str =>
              div(
                if (str.isEmpty) div("empty") else p.thunk("inner")(str)(VDomModifier(str, myInner)),
                myInnerOther
              )
            }
          },
          myOther
        )
      }
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      element.innerHTML shouldBe ""

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b></b></div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b></b></div>"

      myString.onNext("ok?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      myString.onNext("ok?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      myString.onNext("hope so")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>hope so</p></div></b></div>"

      myString.onNext("ohai")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ohai</p></div></b></div>"

      myString.onNext("")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><div>empty</div></div></b></div>"

      myInner.onNext("!")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><div>empty</div></div></b></div>"

      myString.onNext("torst")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>torst!</p></div></b></div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>torst!</p></div></b></div>"

      myInner.onNext("?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>torst?</p></div></b></div>"

      myString.onNext("gandalf")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b></div>"

      myString.onNext("gandalf")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b></div>"

      myOther.onNext("du")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b>du</div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b>du</div>"

      myInner.onNext("!")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf!</p></div></b>du</div>"

      myString.onNext("fanta")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      myString.onNext("fanta")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      myOther.onNext("nee")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>nee</div>"

      myInnerOther.onNext("muh")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p>muh</div></b>nee</div>"

      myString.onNext("ghost")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      myString.onNext("ghost")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      myOther.onNext("life")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      myInnerOther.onNext("muh")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      myString.onNext("ghost")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      myOther.onNext("seen")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>seen</div>"

      myInnerOther.onNext("muh")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>seen</div>"

      myString.onNext("")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><div>empty</div>muh</div></b>seen</div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><div>empty</div>muh</div></b>seen</div>"

      myString.onNext("gott")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>muh</div></b>seen</div>"

      myOther.onNext("dorf")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>muh</div></b>dorf</div>"

      myInnerOther.onNext("ach")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      myString.onNext("gott")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      myInner.onNext("hans")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gotthans</p>ach</div></b>dorf</div>"

      myOther.onNext("das")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gotthans</p>ach</div></b>das</div>"

      myInnerOther.onNext("tank")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gotthans</p>tank</div></b>das</div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gotthans</p>tank</div></b>das</div>"

      myInner.onNext("so")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gottso</p>tank</div></b>das</div>"

      myOther.onNext("datt")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gottso</p>tank</div></b>datt</div>"

      myInnerOther.onNext("ohje")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gottso</p>ohje</div></b>datt</div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gottso</p>ohje</div></b>datt</div>"

      myString.onNext("ende")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>datt</div>"

      myString.onNext("ende")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>datt</div>"

      myOther.onNext("fin")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>fin</div>"

      myThunk.onNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>fin</div>"
    }
  }

  it should "work with streams" in {
    val myString: Handler[String] = Handler.unsafe[String]
    val myId: Handler[String] = Handler.unsafe[String]
    val myInner: Handler[String] = Handler.unsafe[String]
    val myOther: Handler[VDomModifier] = Handler.unsafe[VDomModifier]
    val thunkContent: Handler[VDomModifier] = Handler.unsafe[VDomModifier]

    var renderFnCounter = 0
    var mounts = List.empty[Int]
    var preupdates = List.empty[Int]
    var updates = List.empty[Int]
    var unmounts = List.empty[Int]
    var counter = 0
    def mountHooks = {
      val c = counter
      counter += 1
      VDomModifier(onDomMount.foreach { mounts :+= c }, onDomPreUpdate.foreach { preupdates :+= c }, onDomUpdate.foreach { updates :+= c }, onDomUnmount.foreach { unmounts :+= c } )
    }
    val node = div(
      id := "strings",
      myString.map { myString =>
        if (myString == "empty") b.thunk("component")(myString)(VDomModifier("empty", mountHooks)) :VDomModifier
        else myInner.map[VNode](s => div(s, mountHooks)).prepend(b(id <-- myId).thunk("component")(myString) {
          renderFnCounter += 1
          VDomModifier(cls := "b", myString, mountHooks, thunkContent)
        }): VDomModifier
      },
      myOther,
      b("something else")
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      mounts shouldBe Nil
      preupdates shouldBe Nil
      updates shouldBe Nil
      unmounts shouldBe Nil
      element.innerHTML shouldBe "<b>something else</b>"

      myString.onNext("wal?")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe Nil
      updates shouldBe Nil
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b">wal?</b><b>something else</b>"""

      myId.onNext("tier")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe List(0)
      updates shouldBe List(0)
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      myString.onNext("wal?")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe List(0, 0)
      updates shouldBe List(0, 0)
      unmounts shouldBe List()
      element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      myString.onNext("hai!")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1)
      preupdates shouldBe List(0, 0)
      updates shouldBe List(0, 0)
      unmounts shouldBe List(0)
      element.innerHTML shouldBe """<b class="b" id="tier">hai!</b><b>something else</b>"""

      myId.onNext("nope")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0)
      element.innerHTML shouldBe """<b class="b" id="nope">hai!</b><b>something else</b>"""

      myString.onNext("empty")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1, 2)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1)
      element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      myId.onNext("nothing")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1, 2)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1)
      element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      myString.onNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="nothing" class="b">hans</b><b>something else</b>"""

      myId.onNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3)
      updates shouldBe List(0, 0, 1, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      myString.onNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      thunkContent.onNext(p(dsl.key := "1", "el dieter"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter</p></b><b>something else</b>"""

      thunkContent.onNext(p(dsl.key := "2", "el dieter II"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><b>something else</b>"""

      myOther.onNext(div("baem!"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><div>baem!</div><b>something else</b>"""

      myInner.onNext("meh")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3, 4)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2, 3)
      element.innerHTML shouldBe """<div>meh</div><div>baem!</div><b>something else</b>"""

      myOther.onNext(div("fini"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3, 4)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2, 3)
      element.innerHTML shouldBe """<div>meh</div><div>fini</div><b>something else</b>"""
    }
  }

  // TODO: this test does not actually fail if it is wrong, but you just see an error message in the console output. Fix when merging error observable in OutwatchTracing.
  "Nested VNode" should "work without outdated patch" in {
    val myList: Handler[List[String]] = Handler.unsafe[List[String]](List("hi"))
    val isSynced: Handler[Int] = Handler.unsafe[Int](-1)
    import scala.concurrent.duration._

    var renderFnCounter = 0
    val node = div(
      div(
        id := "strings",
        myList.map(_.indices.map { i =>
          val counter = renderFnCounter
          renderFnCounter += 1
          div {

            val node = myList.map { list => list(i) }
            val syncedIcon = isSynced.map { isSynced =>
              if(i <= isSynced) div(dsl.key := "something", "a", counter) else div("b", counter)
            }

            node.map { node =>
              div(
                div(node),
                syncedIcon,
                counter
              )
            }
          }
        })
      ),
      button("Edit", id := "edit-button",
        onClick(myList).map(l => l.init ++ l.lastOption.map(_ + "!")) --> myList,
        onClick(isSynced).map(_ + 1) --> isSynced
      )
    )

    OutWatch.renderInto("#app", node).unsafeToFuture.flatMap { _ =>
      val element = document.getElementById("strings")
      val editButton = document.getElementById("edit-button")

      for {
        _ <- monix.eval.Task.unit.runToFuture
        _ = sendEvent(editButton, "click")
        _ <- monix.eval.Task.unit.delayResult(1 seconds).runToFuture
        _ = sendEvent(editButton, "click")
        _ <- monix.eval.Task.unit.delayResult(1 seconds).runToFuture
      } yield succeed
    }
  }

  // TODO: this test does not actually fail if it is wrong, but you just see an error message in the console output. Fix when merging error observable in OutwatchTracing.
  it should "work without outdated patch in thunk" in {
    val myList: Handler[List[String]] = Handler.unsafe[List[String]](List("hi"))
    val isSynced: Handler[Int] = Handler.unsafe[Int](-1)
    import scala.concurrent.duration._

    var renderFnCounter = 0
    val node = div(
      div(
        id := "strings",
        myList.map(_.indices.map { i =>
          val counter = renderFnCounter
          renderFnCounter += 1
          div.static(i) {

            val node = myList.map { list => list(i) }
            val syncedIcon = isSynced.map { isSynced =>
              if(i <= isSynced) div(dsl.key := "something", "a", counter) else div("b", counter)
            }

            node.map { node =>
              div(
                div(node),
                syncedIcon,
                counter
              )
            }
          }
        })
      ),
      button("Edit", id := "edit-button",
        onClick(myList).map(l => l.init ++ l.lastOption.map(_ + "!")) --> myList,
        onClick(isSynced).map(_ + 1) --> isSynced
      )
    )

    OutWatch.renderInto("#app", node).unsafeToFuture.flatMap { _ =>
      val element = document.getElementById("strings")
      val editButton = document.getElementById("edit-button")

      for {
        _ <- monix.eval.Task.unit.runToFuture
        _ = sendEvent(editButton, "click")
        _ <- monix.eval.Task.unit.delayResult(1 seconds).runToFuture
        _ = sendEvent(editButton, "click")
        _ <- monix.eval.Task.unit.delayResult(1 seconds).runToFuture
      } yield succeed
    }
  }

  "Custom Emitter builder" should "work with events" in {
    import scala.concurrent.duration._

    val clickableView: EmitterBuilder[Boolean, VDomModifier] = EmitterBuilder.ofModifier { sink: Observer[Boolean] =>
      VDomModifier(
        display.flex,
        minWidth := "0px",

        onMouseDown(true) --> sink,
        onMouseUp(false) --> sink
      )
    }

    for {
      handler <- Handler.create[String]
      node = div(
        id := "strings",
        clickableView.map {
          case true => "yes"
          case false => "no"
        } --> handler,
        handler
      )
      _ <- OutWatch.renderInto("#app", node)

      element = document.getElementById("strings")
      _ = element.innerHTML shouldBe ""

      _ = sendEvent(element, "mousedown")
      _ <- monix.eval.Task.unit.delayResult(0.1 seconds).toIO
      _ = element.innerHTML shouldBe "yes"

      _ = sendEvent(element, "mouseup")
      _ <- monix.eval.Task.unit.delayResult(0.1 seconds).toIO
      _ = element.innerHTML shouldBe "no"
    } yield succeed
  }
}
