package outwatch

import cats.Monoid
import cats.implicits._
import cats.effect.{IO, SyncIO}
import org.scalajs.dom.window.localStorage
import org.scalajs.dom.{Element, Event, document, html}
import outwatch.helpers._
import outwatch.dsl._
import snabbdom.{DataObject, Hooks, VNodeProxy}
import org.scalatest.Assertion
import outwatch.interpreter._
import colibri._
import org.scalajs.dom.EventInit

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON
import colibri.Subject

final case class Fixture(proxy: VNodeProxy)
class OutwatchDomSpec extends JSDomAsyncSpec {

  implicit def ListToJsArray[T](list: Seq[T]): js.Array[T] = list.toJSArray

  def sendEvent(elem: Element, eventType: String) = {
    val event = new Event(eventType, new EventInit {
      bubbles = true
      cancelable = false
    })
    elem.dispatchEvent(event)
  }

  def newProxy(tagName: String, dataObject: DataObject, string: String) = new VNodeProxy {
    sel = tagName
    data = dataObject
    text = string
  }

  def newProxy(tagName: String, dataObject: DataObject, childProxies: js.UndefOr[js.Array[VNodeProxy]] = js.undefined) = new VNodeProxy {
    sel = tagName
    data = dataObject
    children = childProxies
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

    val propertiesArr = new MutableNestedArray[StaticVModifier]
    properties.foreach(propertiesArr.push(_))
    val seps = SeparatedModifiers.from(propertiesArr)
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

  "VModifiers" should "be separated correctly" in {
    val modifiers = Seq(
      BasicAttr("class", "red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      new StringVNode("Test"),
      div(),
      CompositeModifier(
        Seq[VModifier](
          div(),
          attributes.`class` := "blue",
          attributes.onClick.as(1) doAction {},
          attributes.hidden <-- Observable(false)
        )
      ),
      VModifier(Observable.empty)
    )


    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    emitters.get.values.size shouldBe 1
    attrs.get.values.size shouldBe 1
    streamable.subscribables.isEmpty shouldBe false
    proxies.get.length shouldBe 3
  }

  it should "be separated correctly with children" in {
    val modifiers: Seq[VModifier] = Seq(
      BasicAttr("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input",  _ => ()),
      VModifier(Observable.empty),
      VModifier(Observable.empty),
      Emitter("keyup",  _ => ()),
      StringVNode("text"),
      div()
    )
    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    emitters.get.values.size shouldBe 3
    attrs.get.values.size shouldBe 1
    streamable.subscribables.isEmpty shouldBe false
    proxies.get.length shouldBe 2
  }

  it should "be separated correctly with string children" in {
    val modifiers: Seq[VModifier] = Seq(
      BasicAttr("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input",  _ => ()),
      Emitter("keyup",  _ => ()),
      VModifier(Observable.empty),
      VModifier(Observable.empty),
      StringVNode("text"),
      StringVNode("text2")
    )

    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    emitters.get.values.size shouldBe 3
    attrs.get.values.size shouldBe 1
    streamable.subscribables.isEmpty shouldBe false
    proxies.get.length shouldBe 2
  }

  it should "be separated correctly with children and properties" in {

    val modifiers = Seq(
      BasicAttr("class","red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input", _ => ()),
      UpdateHook((_,_) => ()),
      VModifier(Observable.empty),
      VModifier(Observable.empty),
      VModifier(Observable.empty),
      Emitter("keyup", _ => ()),
      InsertHook(_ => ()),
      PrePatchHook((_,_) => ()),
      PostPatchHook((_,_) => ()),
      StringVNode("text")
    )

    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
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
    streamable.subscribables.isEmpty shouldBe false
    proxies.get.length shouldBe 1
  }

  val fixture = Fixture(
    newProxy(
      "div",
      new DataObject {
        attrs = js.Dictionary[Attr.Value]("class" -> "red", "id" -> "msg")
        hook = Hooks.empty
      },
      js.Array(newProxy("span", new DataObject { hook = Hooks.empty }, "Hello"))
    )
  )

  it should "run effect modifiers once" in {
    val list = new collection.mutable.ArrayBuffer[String]

    val vtree = div(
      IO {
        list += "child1"
        VModifier(Observable(div()))
      },
      IO {
        list += "child2"
        VModifier(Observable.empty)
      },
      IO {
        list += "children1"
        VModifier(Observable.empty)
      },
      IO {
        list += "children2"
        VModifier(Observable.empty)
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
      _ <- Outwatch.renderInto[IO](node, vtree)
    } yield {

      list should contain theSameElementsAs List(
        "child1", "child2", "children1", "children2", "attr1", "attr2"
      )

    }

    test
  }

  it should "not provide unique key for child nodes if stream is present" in {
    val mods = Seq(
      VModifier(Observable.empty),
      div(idAttr := "1"),
      div(idAttr := "2"),
      div(), div()
    )

    val streamable = NativeModifiers.from(mods, RenderConfig.ignoreError)
    val seps =  SeparatedModifiers.from(streamable.modifiers)
    import seps._

    proxies.get.length shouldBe 4
    streamable.subscribables.isEmpty shouldBe false

    val proxy = SnabbdomOps.toSnabbdom(div(mods), RenderConfig.ignoreError)
    proxy.key.isDefined shouldBe false

    val key1 = proxy.children.get(0).key
    val key2 = proxy.children.get(1).key
    val key3 = proxy.children.get(2).key
    val key4 = proxy.children.get(3).key

    key1.isDefined shouldBe false
    key2.isDefined shouldBe false
    key3.isDefined shouldBe false
    key4.isDefined shouldBe false
  }

  it should "keep existing key for child nodes" in {
    val mods = Seq(
      Key(1234),
      VModifier(Observable.empty),
      div()(Key(5678))
    )

    val streamable = NativeModifiers.from(mods, RenderConfig.ignoreError)
    val seps = SeparatedModifiers.from(streamable.modifiers)
    import seps._

    proxies.get.length shouldBe 1
    streamable.subscribables.isEmpty shouldBe false

    val proxy = SnabbdomOps.toSnabbdom(div(mods), RenderConfig.ignoreError)
    proxy.key.toOption  shouldBe Some(1234)

    proxy.children.get(0).key.toOption shouldBe Some(5678)
  }

  "VTrees" should "be constructed correctly" in {

    val attributes = List(BasicAttr("class", "red"), BasicAttr("id", "msg"))
    val message = "Hello"
    val child = span(message)
    val vtree = div(attributes.head, attributes(1), child)

    val proxy = fixture.proxy

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(proxy)
  }

  it should "be correctly created with the HyperscriptHelper" in {
    val attributes = List(BasicAttr("class", "red"), BasicAttr("id", "msg"))
    val message = "Hello"
    val child = span(message)
    val vtree = div(attributes.head, attributes(1), child)

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(fixture.proxy)
  }


  it should "run its effect modifiers once!" in {
    IO(Subject.replayLatest[String]()).flatMap { stringHandler =>

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


      Outwatch.renderInto[IO](node, vtree).map { _ =>

        ioCounter shouldBe 1
        handlerCounter shouldBe 0
        stringHandler.unsafeOnNext("pups")
        ioCounter shouldBe 1
        handlerCounter shouldBe 1

      }
    }
  }

  it should "run its effect modifiers once in CompositeModifier!" in {
    IO(Subject.replayLatest[String]()).flatMap { stringHandler =>

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

      Outwatch.renderInto[IO](node, vtree).map { _ =>

        ioCounter shouldBe 1
        handlerCounter shouldBe 0
        stringHandler.unsafeOnNext("pups")
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

      Outwatch.renderInto[IO](n, vtree).map { _ =>

          val patchedNode = document.getElementById(id)
          patchedNode.childElementCount shouldBe 1
          patchedNode.classList.contains(cls) shouldBe true
          patchedNode.children(0).innerHTML shouldBe message
      }

    }

  }

  it should "be replaced if they contain changeables (SyncIO)" in {

    def page(num: Int): VModifier = for {
      pageNum <- SyncIO(Subject.behavior(num))
    } yield div( idAttr := "page",
      num match {
        case 1 =>
          div(pageNum)
        case 2 =>
          div(pageNum)
      }
    )


    val pageHandler = Subject.publish[Int]()

    val vtree = div(
      div(pageHandler.map(page))
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      n <- node
      _ <- Outwatch.renderInto[IO](n, vtree)
      _ <- pageHandler.onNextIO(1)
      domNode = document.getElementById("page")
      _ = domNode.textContent shouldBe "1"
      _ <- pageHandler.onNextIO(2)
      _ = domNode.textContent shouldBe "2"
    } yield succeed
  }

  it should "be replaced if they contain changeables (IO)" in {

    def page(num: Int): VModifier = for {
      pageNum <- IO(Subject.behavior(num))
    } yield div( idAttr := "page",
      num match {
        case 1 =>
          div(pageNum)
        case 2 =>
          div(pageNum)
      }
    )


    val pageHandler = Subject.publish[Int]()

    val vtree = div(
      div(pageHandler.map(page))
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    // import scala.concurrent.duration._

    for {
      n <- node
      _ <- Outwatch.renderInto[IO](n, vtree)
      _ <- pageHandler.onNextIO(1)
      domNode = document.getElementById("page")
      _ = domNode.textContent shouldBe "1"
      _ <- pageHandler.onNextIO(2)
      domNode2 = document.getElementById("page")
      _ = domNode2.textContent shouldBe "2"
    } yield succeed
  }

  "The HTML DSL" should "construct VTrees properly" in {
    val vtree = div(cls := "red", idAttr := "msg", span("Hello"))

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(fixture.proxy)
  }

  it should "construct VTrees with optional children properly" in {
    val vtree = div(cls := "red", idAttr := "msg",
      Option(span("Hello")),
      Option.empty[VModifier]
    )

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
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

    val attributes = js.Dictionary[Attr.Value]("a" -> true, "b" -> true, "c" -> false, "d" -> "true", "e" -> "true", "f" -> "false")
    val expected = newProxy("div", new DataObject { attrs = attributes; hook = Hooks.empty })

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
    snabbdomNode._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(expected)
  }

  it should "patch into the DOM properly" in {

    val message = "Test"
    val vtree = div(cls := "blue", idAttr := "test",
      span(message),
      ul(idAttr := "list",
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

      Outwatch.renderInto[IO](n, vtree).map {_ =>

        val patchedNode = document.getElementById("test")

        patchedNode.childElementCount shouldBe 2
        patchedNode.classList.contains("blue") shouldBe true
        patchedNode.children(0).innerHTML shouldBe message
        document.getElementById("list").childElementCount shouldBe 3
      }

    }
  }

  it should "change the value of a textfield" in {

    val messages = Subject.publish[String]()
    val vtree = div(
      input(attributes.value <-- messages, idAttr := "input")
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, vtree).map { _ =>

        val field = document.getElementById("input").asInstanceOf[html.Input]

        field.value shouldBe ""

        val message = "Hello"
        messages.unsafeOnNext(message)

        field.value shouldBe message

        val message2 = "World"
        messages.unsafeOnNext(message2)

        field.value shouldBe message2
      }

    }
  }

  it should "render child nodes in correct order" in {

    val messagesA = Subject.publish[String]()
    val messagesB = Subject.publish[String]()

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

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        messagesA.unsafeOnNext("1")
        messagesB.unsafeOnNext("2")

        n.innerHTML shouldBe "<div><span>A</span><span>1</span><span>B</span><span>2</span></div>"
      }

    }
  }

  it should "render child string-nodes in correct order" in {

    val messagesA = Subject.publish[String]()
    val messagesB = Subject.publish[String]()
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

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        n.innerHTML shouldBe "<div>AB</div>"

        messagesA.unsafeOnNext("1")
        n.innerHTML shouldBe "<div>A1B</div>"

        messagesB.unsafeOnNext("2")
        n.innerHTML shouldBe "<div>A1B2</div>"
      }

    }
  }

  it should "render child string-nodes in correct order, mixed with children" in {

    val messagesA = Subject.publish[String]()
    val messagesB = Subject.publish[String]()
    val messagesC = Subject.publish[Seq[VNode]]()

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

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        n.innerHTML shouldBe "<div>AB</div>"

        messagesA.unsafeOnNext("1")
        n.innerHTML shouldBe "<div>A1B</div>"

        messagesB.unsafeOnNext("2")
        n.innerHTML shouldBe "<div>A1B2</div>"

        messagesC.unsafeOnNext(Seq(div("5"), div("7")))
        n.innerHTML shouldBe "<div>A1<div>5</div><div>7</div>B2</div>"
      }

    }
  }

  it should "update merged nodes children correctly" in {

    val messages = Subject.publish[Seq[VNode]]()
    val otherMessages = Subject.publish[Seq[VNode]]()
    val vNode = div(messages)(otherMessages)


    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        otherMessages.unsafeOnNext(Seq(div("otherMessage")))
        n.children(0).innerHTML shouldBe "<div>otherMessage</div>"

        messages.unsafeOnNext(Seq(div("message")))
        n.children(0).innerHTML shouldBe "<div>message</div><div>otherMessage</div>"

        otherMessages.unsafeOnNext(Seq(div("genus")))
        n.children(0).innerHTML shouldBe "<div>message</div><div>genus</div>"
      }

    }
  }

  it should "update merged nodes separate children correctly" in {

    val messages = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNode = div(messages)(otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        n.children(0).innerHTML shouldBe ""

        otherMessages.unsafeOnNext("otherMessage")
        n.children(0).innerHTML shouldBe "otherMessage"

        messages.unsafeOnNext("message")
        n.children(0).innerHTML shouldBe "messageotherMessage"

        otherMessages.unsafeOnNext("genus")
        n.children(0).innerHTML shouldBe "messagegenus"
      }

    }
  }

  it should "partially render component even if parts not present" in {

    val messagesColor = Subject.publish[String]()
    val messagesBgColor = Subject.publish[String]()
    val childString = Subject.publish[String]()

    val vNode = div( idAttr := "inner",
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

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        val inner = document.getElementById("inner").asInstanceOf[html.Div]

        inner.innerHTML shouldBe ""
        inner.style.color shouldBe ""
        inner.style.backgroundColor shouldBe ""

        childString.unsafeOnNext("fish")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe ""
        inner.style.backgroundColor shouldBe ""

        messagesColor.unsafeOnNext("red")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe "red"
        inner.style.backgroundColor shouldBe ""

        messagesBgColor.unsafeOnNext("blue")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe "red"
        inner.style.backgroundColor shouldBe "blue"

      }

    }
  }

  it should "partially render component even if parts not present2" in {

    val messagesColor = Subject.publish[String]()
    val childString = Subject.publish[String]()

    val vNode = div( idAttr := "inner",
      color <-- messagesColor,
      childString
    )

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        val inner = document.getElementById("inner").asInstanceOf[html.Div]

        inner.innerHTML shouldBe ""
        inner.style.color shouldBe ""

        childString.unsafeOnNext("fish")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe ""

        messagesColor.unsafeOnNext("red")
        inner.innerHTML shouldBe "fish"
        inner.style.color shouldBe "red"
      }

    }
  }

  it should "update reused vnodes correctly" in {

    val messages = Subject.publish[String]()
    val vNode = div(data.ralf := true, messages)
    val container = div(vNode, vNode)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, container).map { _ =>

        messages.unsafeOnNext("message")
        n.children(0).children(0).innerHTML shouldBe "message"
        n.children(0).children(1).innerHTML shouldBe "message"

        messages.unsafeOnNext("bumo")
        n.children(0).children(0).innerHTML shouldBe "bumo"
        n.children(0).children(1).innerHTML shouldBe "bumo"
      }

    }
  }

  it should "update merged nodes correctly (render reuse)" in {

    val messages = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNodeTemplate = div(messages)
    val vNode = vNodeTemplate(otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    val test = for {

      node1 <- node
       _ <- Outwatch.renderInto[IO](node1, vNodeTemplate)

      node2 <- node
       _ <- Outwatch.renderInto[IO](node2, vNode)

    } yield {

      messages.unsafeOnNext("gurkon")
      otherMessages.unsafeOnNext("otherMessage")
      node1.children(0).innerHTML shouldBe "gurkon"
      node2.children(0).innerHTML shouldBe "gurkonotherMessage"

      messages.unsafeOnNext("message")
      node1.children(0).innerHTML shouldBe "message"
      node2.children(0).innerHTML shouldBe "messageotherMessage"

      otherMessages.unsafeOnNext("genus")
      node1.children(0).innerHTML shouldBe "message"
      node2.children(0).innerHTML shouldBe "messagegenus"
    }

    test
  }

  it should "update merged node attributes correctly" in {

    val messages = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNode = div(data.noise <-- messages)(data.noise <-- otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        otherMessages.unsafeOnNext("otherMessage")
        n.children(0).getAttribute("data-noise") shouldBe "otherMessage"

        messages.unsafeOnNext("message") // should be ignored
        n.children(0).getAttribute("data-noise") shouldBe "otherMessage"

        otherMessages.unsafeOnNext("genus")
        n.children(0).getAttribute("data-noise") shouldBe "genus"
      }

    }
  }

  it should "update merged node styles written with style() correctly" in {

    val messages = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNode = div(style("color") <-- messages)(style("color") <-- otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        otherMessages.unsafeOnNext("red")
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

        messages.unsafeOnNext("blue") // should be ignored
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

        otherMessages.unsafeOnNext("green")
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "green"
      }

    }
  }

  it should "update merged node styles correctly" in {

    val messages = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNode = div(color <-- messages)(color <-- otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        otherMessages.unsafeOnNext("red")
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

        messages.unsafeOnNext("blue") // should be ignored
        n.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

        otherMessages.unsafeOnNext("green")
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

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        n.innerHTML shouldBe "<div><span>one</span><span>two</span><span>three</span></div>"
      }

    }
  }

  it should "render nodes with only attribute receivers properly" in {

    val classes = Subject.publish[String]()
    val vNode = button( className <-- classes, "Submit")

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    node.flatMap { n =>

      Outwatch.renderInto[IO](n, vNode).map { _ =>

        classes.unsafeOnNext("active")

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

      Outwatch.renderInto[IO](n, vNode).map { _ =>

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

      Outwatch.renderReplace[IO](n, vNode).map {_ =>

        n.selected shouldBe true
        n.disabled shouldBe true
      }

    }
  }

  it should "correctly work with Render conversions" in {

    val test: IO[Assertion] = for {
      n1 <- IO(document.createElement("div"))

       _ <- Outwatch.renderReplace[IO](n1, div("one"))
       _ = n1.innerHTML shouldBe "one"

       _ <- Outwatch.renderReplace[IO](n1, div(Some("one")))
       _ = n1.innerHTML shouldBe "one"

      n2 <- IO(document.createElement("div"))

       _ <- Outwatch.renderReplace[IO](n2, div(None:Option[Int]))
       _ = n2.innerHTML shouldBe ""

      _ <- Outwatch.renderReplace[IO](n1, div(1))
      _ = n1.innerHTML shouldBe "1"

      _ <- Outwatch.renderReplace[IO](n1, div(1.0))
      _ = n1.innerHTML shouldBe "1"

      _ <- Outwatch.renderReplace[IO](n1, div(Seq("one", "two")))
      _ = n1.innerHTML shouldBe "onetwo"

      _ <- Outwatch.renderReplace[IO](n1, div(Seq(1, 2)))
      _ = n1.innerHTML shouldBe "12"

      _ <- Outwatch.renderReplace[IO](n1, div(Seq(1.0, 2.0)))
      _ = n1.innerHTML shouldBe "12"

    } yield succeed

    test
  }

  "Children stream" should "work for string sequences" in {

    val myStrings: Observable[Seq[String]] = Observable(Seq("a", "b"))
    val node = div(idAttr := "strings", myStrings)

    Outwatch.renderInto[IO]("#app", node).map { _ =>

      val element = document.getElementById("strings")
      element.innerHTML shouldBe "ab"

    }
  }

  "Children stream" should "work for double/boolean/long/int" in {

    val node = div(idAttr := "strings",
      1.1, true, 133L, 7
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>

      val element = document.getElementById("strings")
      element.innerHTML shouldBe "1.1true1337"

    }
  }

  "Child stream" should "work for string options" in {

    IO(Subject.behavior(Option("a"))).flatMap { myOption =>

      val node = div(idAttr := "strings", myOption)

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "a"

        myOption.unsafeOnNext(None)
        element.innerHTML shouldBe ""

      }

    }
  }

  it should "work for vnode options" in {

    IO(Subject.behavior(Option(div("a")))).flatMap { myOption =>

      val node = div(idAttr := "strings", myOption)

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div>a</div>"

        myOption.unsafeOnNext(None)
        element.innerHTML shouldBe ""

      }

    }
  }

  "Modifier stream" should "work for modifier" in {

    IO(Subject.behavior[VModifier](Seq(cls := "hans", b("stark")))).flatMap { myHandler =>

      val node = div(idAttr := "strings",
        div(VModifier(myHandler))
      )

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe """<div class="hans"><b>stark</b></div>"""

        myHandler.unsafeOnNext(Option(idAttr := "fair"))
        element.innerHTML shouldBe """<div id="fair"></div>"""

      }

    }
  }

  it should "work for multiple mods" in {

    val test: IO[Assertion] = IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>

      val node = div(idAttr := "strings",
        div(myHandler, "bla")
      )

      Outwatch.renderInto[IO]("#app", node).flatMap { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div>bla</div>"

        myHandler.unsafeOnNext(cls := "hans")
        element.innerHTML shouldBe """<div class="hans">bla</div>"""

        IO(Subject.replayLatest[VModifier]()).map { innerHandler =>

          myHandler.unsafeOnNext(div(
            innerHandler,
            cls := "no?",
            "yes?"
          ))
          element.innerHTML shouldBe """<div><div class="no?">yes?</div>bla</div>"""

          innerHandler.unsafeOnNext(Seq(span("question:"), idAttr := "heidi"))
          element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question:</span>yes?</div>bla</div>"""

          myHandler.unsafeOnNext(div(
            innerHandler,
            cls := "no?",
            "yes?",
            b("go!")
          ))

          element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question:</span>yes?<b>go!</b></div>bla</div>"""

          innerHandler.unsafeOnNext(Seq(span("question and answer:"), idAttr := "heidi"))
          element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question and answer:</span>yes?<b>go!</b></div>bla</div>"""

          myHandler.unsafeOnNext(Seq(span("nope")))
          element.innerHTML shouldBe """<div><span>nope</span>bla</div>"""

          innerHandler.unsafeOnNext(b("me?"))
          element.innerHTML shouldBe """<div><span>nope</span>bla</div>"""

        }
      }

    }

    test
  }

  it should "work for nested stream modifier" in {

    IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>

      val node = div(idAttr := "strings",
        div(myHandler)
      )

      Outwatch.renderInto[IO]("#app", node).flatMap { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        IO(Subject.replayLatest[VModifier]()).flatMap { innerHandler =>

          myHandler.unsafeOnNext(innerHandler)
          element.innerHTML shouldBe """<div></div>"""

          innerHandler.unsafeOnNext(VModifier(cls := "hans", "1"))
          element.innerHTML shouldBe """<div class="hans">1</div>"""

          IO(Subject.replayLatest[VModifier]()).map { innerHandler2 =>

          myHandler.unsafeOnNext(innerHandler2)
          element.innerHTML shouldBe """<div></div>"""

          myHandler.unsafeOnNext(CompositeModifier(VModifier(innerHandler2) :: Nil))
          element.innerHTML shouldBe """<div></div>"""

          myHandler.unsafeOnNext(CompositeModifier(VModifier(innerHandler2) :: Nil))

          myHandler.unsafeOnNext(CompositeModifier(StringVNode("pete") :: VModifier(innerHandler2) :: Nil))
          element.innerHTML shouldBe """<div>pete</div>"""

          innerHandler2.unsafeOnNext(VModifier(idAttr := "dieter", "r"))
          element.innerHTML shouldBe """<div id="dieter">peter</div>"""

          innerHandler.unsafeOnNext(b("me?"))
          element.innerHTML shouldBe """<div id="dieter">peter</div>"""

          myHandler.unsafeOnNext(span("the end"))
          element.innerHTML shouldBe """<div><span>the end</span></div>"""

          }
        }
      }

    }
  }

  it should "work for nested stream modifier and default value" in {

    var numPatches = 0

    IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>

      val node: VNode = div(idAttr := "strings",
        div(
          onSnabbdomPrePatch doAction { numPatches += 1 },
          myHandler.prepend(VModifier("initial"))
        )
      )

      Outwatch.renderInto[IO]("#app", node).flatMap { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div>initial</div>"
        numPatches shouldBe 0

      IO(Subject.replayLatest[VModifier]()).flatMap { innerHandler =>

        numPatches shouldBe 0

        myHandler.unsafeOnNext(innerHandler.prepend(BasicAttr("initial", "2")))
        element.innerHTML shouldBe """<div initial="2"></div>"""
        numPatches shouldBe 2

        innerHandler.unsafeOnNext(BasicAttr("attr", "3"))
        element.innerHTML shouldBe """<div attr="3"></div>"""
        numPatches shouldBe 3

        IO(Subject.replayLatest[VModifier]()).map {innerHandler2 =>
          myHandler.unsafeOnNext(innerHandler2.prepend(VModifier("initial3")))
          element.innerHTML shouldBe """<div>initial3</div>"""
          numPatches shouldBe 5

          myHandler.unsafeOnNext(CompositeModifier(VModifier(innerHandler2.prepend(VModifier("initial4"))) :: Nil))
          element.innerHTML shouldBe """<div>initial4</div>"""
          numPatches shouldBe 7

          myHandler.unsafeOnNext(CompositeModifier(StringVNode("pete") :: VModifier(innerHandler2) :: Nil))
          element.innerHTML shouldBe """<div>pete</div>"""
          numPatches shouldBe 8

          innerHandler2.unsafeOnNext(VModifier(idAttr := "dieter", "r"))
          element.innerHTML shouldBe """<div id="dieter">peter</div>"""
          numPatches shouldBe 9

          innerHandler.unsafeOnNext("me?")
          element.innerHTML shouldBe """<div id="dieter">peter</div>"""
          numPatches shouldBe 9

          myHandler.unsafeOnNext(span("the end"))
          element.innerHTML shouldBe """<div><span>the end</span></div>"""
          numPatches shouldBe 10
      }}}}
  }

  it should "work for deeply nested handlers" in {

    val test: IO[Assertion] = IO(Subject.behavior(0)).flatMap { a =>

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

      Outwatch.renderInto[IO]("#app", node).map {_ =>

        val element = document.getElementById("app")
        element.innerHTML shouldBe "<div><div><div>0</div></div></div>"

        a.unsafeOnNext(1)
        element.innerHTML shouldBe "<div><div><div>1</div></div></div>"

      }
    }

    test
  }

  it should "work for nested stream modifier and empty default and start with" in {

    var numPatches = 0

    IO(Subject.behavior[VModifier]("initial")).flatMap { myHandler =>

    val node = div(idAttr := "strings",
      div(
        onSnabbdomPrePatch doAction { numPatches += 1 },
        myHandler
      )
    )

    Outwatch.renderInto[IO]("#app", node).flatMap { _ =>

      val element = document.getElementById("strings")
      element.innerHTML shouldBe "<div>initial</div>"
      numPatches shouldBe 0

      IO(Subject.replayLatest[VModifier]()).flatMap { innerHandler =>
      myHandler.unsafeOnNext(innerHandler.startWith(BasicAttr("initial", "2") :: Nil))
      element.innerHTML shouldBe """<div initial="2"></div>"""
      numPatches shouldBe 2

      innerHandler.unsafeOnNext(BasicAttr("attr", "3"))
      element.innerHTML shouldBe """<div attr="3"></div>"""
      numPatches shouldBe 3

        IO(Subject.replayLatest[VModifier]()).map { innerHandler2 =>
        myHandler.unsafeOnNext(innerHandler2.startWith(VModifier("initial3") :: Nil))
        element.innerHTML shouldBe """<div>initial3</div>"""
        numPatches shouldBe 5

        myHandler.unsafeOnNext(CompositeModifier(VModifier(innerHandler2.prepend(VModifier("initial4"))) :: Nil))
        element.innerHTML shouldBe """<div>initial4</div>"""
        numPatches shouldBe 7

        myHandler.unsafeOnNext(CompositeModifier(StringVNode("pete") :: VModifier(innerHandler2) :: Nil))
        element.innerHTML shouldBe """<div>pete</div>"""
        numPatches shouldBe 8

        innerHandler2.unsafeOnNext(VModifier(idAttr := "dieter", "r"))
        element.innerHTML shouldBe """<div id="dieter">peter</div>"""
        numPatches shouldBe 9

        innerHandler.unsafeOnNext("me?")
        element.innerHTML shouldBe """<div id="dieter">peter</div>"""
        numPatches shouldBe 9

        myHandler.unsafeOnNext(span("the end"))
        element.innerHTML shouldBe """<div><span>the end</span></div>"""
        numPatches shouldBe 10

    }}}}
  }

  it should "work for stream modifier and streaming default value (subscriptions are canceled properly)" in {

    IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>
    IO(Subject.replayLatest[VModifier]()).flatMap { innerHandler =>

      val outerTriggers = new scala.collection.mutable.ArrayBuffer[VModifier]
      val innerTriggers = new scala.collection.mutable.ArrayBuffer[VModifier]

      val node = div(idAttr := "strings",
        div(
          myHandler.map { x => outerTriggers += x; x }.prepend(VModifier(innerHandler.map { x => innerTriggers += x; x }.prepend(VModifier("initial"))))
        )
      )

      Outwatch.renderInto[IO]("#app", node).flatMap {_ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div>initial</div>"
        outerTriggers.size shouldBe 0
        innerTriggers.size shouldBe 0

        innerHandler.unsafeOnNext(VModifier("hi!"))
        element.innerHTML shouldBe """<div>hi!</div>"""
        outerTriggers.size shouldBe 0
        innerTriggers.size shouldBe 1

        myHandler.unsafeOnNext(VModifier("test"))
        element.innerHTML shouldBe """<div>test</div>"""
        outerTriggers.size shouldBe 1
        innerTriggers.size shouldBe 1

        myHandler.unsafeOnNext(Observable(BasicAttr("initial", "2")))
        element.innerHTML shouldBe """<div initial="2"></div>"""
        outerTriggers.size shouldBe 2
        innerTriggers.size shouldBe 1

        innerHandler.unsafeOnNext(VModifier("me?"))
        element.innerHTML shouldBe """<div initial="2"></div>"""
        outerTriggers.size shouldBe 2
        innerTriggers.size shouldBe 1

        myHandler.unsafeOnNext(innerHandler.map { x => innerTriggers += x; x }.prepend(VModifier.empty))
        element.innerHTML shouldBe """<div>me?</div>"""
        outerTriggers.size shouldBe 3
        innerTriggers.size shouldBe 2

        innerHandler.unsafeOnNext(BasicAttr("attr", "3"))
        element.innerHTML shouldBe """<div attr="3"></div>"""
        outerTriggers.size shouldBe 3
        innerTriggers.size shouldBe 3

        val innerTriggers2 = new scala.collection.mutable.ArrayBuffer[VModifier]
        val innerTriggers3 = new scala.collection.mutable.ArrayBuffer[VModifier]

        IO(Subject.replayLatest[VModifier]()).flatMap { innerHandler2 =>
        IO(Subject.replayLatest[VModifier]()).map { innerHandler3 =>

            innerHandler.unsafeOnNext(innerHandler2.map { x => innerTriggers2 += x; x }.prepend(VModifier(innerHandler3.map { x => innerTriggers3 += x; x })))
            element.innerHTML shouldBe """<div></div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 4
            innerTriggers2.size shouldBe 0
            innerTriggers3.size shouldBe 0

            innerHandler2.unsafeOnNext(VModifier("2"))
            element.innerHTML shouldBe """<div>2</div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 4
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            innerHandler.unsafeOnNext(EmptyModifier)
            element.innerHTML shouldBe """<div></div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            innerHandler2.unsafeOnNext(VModifier("me?"))
            element.innerHTML shouldBe """<div></div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            innerHandler3.unsafeOnNext(VModifier("me?"))
            element.innerHTML shouldBe """<div></div>"""
            outerTriggers.size shouldBe 3
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            myHandler.unsafeOnNext(VModifier("go away"))
            element.innerHTML shouldBe """<div>go away</div>"""
            outerTriggers.size shouldBe 4
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

            innerHandler.unsafeOnNext(VModifier("me?"))
            element.innerHTML shouldBe """<div>go away</div>"""
            outerTriggers.size shouldBe 4
            innerTriggers.size shouldBe 5
            innerTriggers2.size shouldBe 1
            innerTriggers3.size shouldBe 0

      }}}}}
  }

  it should "be able to render basic handler" in {
    val counter: VModifier = button(
      idAttr := "click",
     IO(Subject.behavior(0)).map { handler =>
        VModifier(onClick(handler.map(_ + 1)) --> handler, handler)
      }
    )

    val vtree = div(counter)

    Outwatch.renderInto[IO]("#app", vtree).map { _ =>
      val element = document.getElementById("click")

      element.innerHTML shouldBe "0"

      sendEvent(element, "click")
      element.innerHTML shouldBe "1"

      sendEvent(element, "click")
      element.innerHTML shouldBe "2"
    }
  }

  it should "be able to render basic handler with scan" in {
    val counter: VModifier = button(
      idAttr := "click",
      IO(Subject.replayLatest[Int]()).map { handler =>
        VModifier(onClick.asScan0(0)(_ + 1) --> handler, handler)
      }
    )

    val vtree = div(counter)

    Outwatch.renderInto[IO]("#app", vtree).map { _ =>
      val element = document.getElementById("click")

      element.innerHTML shouldBe "0"

      sendEvent(element, "click")
      element.innerHTML shouldBe "1"

      sendEvent(element, "click")
      element.innerHTML shouldBe "2"
    }
  }

  it should "to render basic handler with scan directly from EmitterBuilder" in {
    val counter: VModifier = button(
      idAttr := "click",
      onClick.asScan0(0)(_ + 1).handled(VModifier(_))
    )

    val vtree = div(counter)

    Outwatch.renderInto[IO]("#app", vtree).map { _ =>
      val element = document.getElementById("click")

      element.innerHTML shouldBe "0"

      sendEvent(element, "click")
      element.innerHTML shouldBe "1"

      sendEvent(element, "click")
      element.innerHTML shouldBe "2"
    }
  }

  it should "work for not overpatching keep proxy from previous patch" in {
    val handler = Subject.replayLatest[Int]()
    val handler2 = Subject.replayLatest[Int]()

    var inserted = 0
    var destroyed = 0
    var patched = 0
    var inserted2 = 0
    var uninserted2 = 0
    var patched2 = 0
    var insertedHeinz = 0
    var uninsertedHeinz = 0
    var patchedHeinz = 0
    var insertedKlara = 0
    var uninsertedKlara = 0
    var patchedKlara = 0

    val node = div(
      idAttr := "strings",
      handler.map(i => if (i == 0) VModifier.empty else div(i, onSnabbdomPostPatch.doAction { patched += 1 }, onSnabbdomInsert.doAction { inserted += 1 }, onSnabbdomDestroy.doAction{  destroyed += 1 })),
      div("heinz", onSnabbdomPostPatch.doAction { patchedHeinz += 1 }, onSnabbdomInsert.doAction { insertedHeinz += 1 }, onSnabbdomDestroy.doAction{  uninsertedHeinz += 1 }),
      handler2.map(i => if (i == 0) VModifier.empty else div(i, onSnabbdomPostPatch.doAction { patched2 += 1 }, onSnabbdomInsert.doAction { inserted2 += 1 }, onSnabbdomDestroy.doAction{  uninserted2 += 1 })),
      div("klara", onSnabbdomPostPatch.doAction { patchedKlara += 1 }, onSnabbdomInsert.doAction { insertedKlara += 1 }, onSnabbdomDestroy.doAction{  uninsertedKlara += 1 }),
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>

      val element = document.getElementById("strings")

      element.innerHTML shouldBe """<div>heinz</div><div>klara</div>"""
      inserted shouldBe 0
      destroyed shouldBe 0
      patched shouldBe 0
      inserted2 shouldBe 0
      uninserted2 shouldBe 0
      patched2 shouldBe 0
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0

      handler.unsafeOnNext(1)
      element.innerHTML shouldBe """<div>1</div><div>heinz</div><div>klara</div>"""
      inserted shouldBe 1
      destroyed shouldBe 0
      patched shouldBe 0
      inserted2 shouldBe 0
      uninserted2 shouldBe 0
      patched2 shouldBe 0
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0

      handler2.unsafeOnNext(99)
      element.innerHTML shouldBe """<div>1</div><div>heinz</div><div>99</div><div>klara</div>"""
      inserted shouldBe 1
      destroyed shouldBe 0
      patched shouldBe 0
      inserted2 shouldBe 1
      uninserted2 shouldBe 0
      patched2 shouldBe 0
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0

      handler2.unsafeOnNext(0)
      element.innerHTML shouldBe """<div>1</div><div>heinz</div><div>klara</div>"""
      inserted shouldBe 1
      destroyed shouldBe 0
      patched shouldBe 0
      inserted2 shouldBe 1
      uninserted2 shouldBe 1
      patched2 shouldBe 0
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0

      handler.unsafeOnNext(0)
      element.innerHTML shouldBe """<div>heinz</div><div>klara</div>"""
      inserted shouldBe 1
      destroyed shouldBe 1
      patched shouldBe 0
      inserted2 shouldBe 1
      uninserted2 shouldBe 1
      patched2 shouldBe 0
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0

      handler2.unsafeOnNext(3)
      element.innerHTML shouldBe """<div>heinz</div><div>3</div><div>klara</div>"""
      inserted shouldBe 1
      destroyed shouldBe 1
      patched shouldBe 0
      inserted2 shouldBe 2
      uninserted2 shouldBe 1
      patched2 shouldBe 0
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0

      handler.unsafeOnNext(2)
      element.innerHTML shouldBe """<div>2</div><div>heinz</div><div>3</div><div>klara</div>"""
      inserted shouldBe 2
      destroyed shouldBe 1
      patched shouldBe 0
      inserted2 shouldBe 2
      uninserted2 shouldBe 1
      patched2 shouldBe 0
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0

      handler.unsafeOnNext(18)
      element.innerHTML shouldBe """<div>18</div><div>heinz</div><div>3</div><div>klara</div>"""
      inserted shouldBe 2
      destroyed shouldBe 1
      patched shouldBe 1
      inserted2 shouldBe 2
      uninserted2 shouldBe 1
      patched2 shouldBe 0
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0

      handler2.unsafeOnNext(19)
      element.innerHTML shouldBe """<div>18</div><div>heinz</div><div>19</div><div>klara</div>"""
      inserted shouldBe 2
      destroyed shouldBe 1
      patched shouldBe 1
      inserted2 shouldBe 2
      uninserted2 shouldBe 1
      patched2 shouldBe 1
      insertedHeinz shouldBe 1
      uninsertedHeinz shouldBe 0
      patchedHeinz shouldBe 0
      insertedKlara shouldBe 1
      uninsertedKlara shouldBe 0
      patchedKlara shouldBe 0
    }
  }

  it should "work for nested observables with seq modifiers " in {

    IO(Subject.behavior("b")).flatMap { innerHandler =>
    IO(Subject.behavior(Seq[VModifier]("a", data.test := "v", innerHandler))).flatMap { outerHandler =>

      val node = div(
        idAttr := "strings",
        outerHandler
      )

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.outerHTML shouldBe """<div id="strings" data-test="v">ab</div>"""

        innerHandler.unsafeOnNext("c")
        element.outerHTML shouldBe """<div id="strings" data-test="v">ac</div>"""

        outerHandler.unsafeOnNext(Seq[VModifier]("meh"))
        element.outerHTML shouldBe """<div id="strings">meh</div>"""
      }

    }}
  }

  it should "work for nested observables with seq modifiers and attribute stream" in {

    IO(Subject.replayLatest[String]()).flatMap { innerHandler =>
    IO(Subject.behavior(Seq[VModifier]("a", data.test := "v", href <-- innerHandler))).flatMap { outerHandler =>

      val node = div(
        idAttr := "strings",
        outerHandler
      )

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.outerHTML shouldBe """<div id="strings" data-test="v">a</div>"""

        innerHandler.unsafeOnNext("c")
        element.outerHTML shouldBe """<div id="strings" data-test="v" href="c">a</div>"""

        innerHandler.unsafeOnNext("d")
        element.outerHTML shouldBe """<div id="strings" data-test="v" href="d">a</div>"""

        outerHandler.unsafeOnNext(Seq[VModifier]("meh"))
        element.outerHTML shouldBe """<div id="strings">meh</div>"""
      }

    }}
  }

  it should "work for double nested stream modifier" in {

    IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>

      val node = div(idAttr := "strings",
        div(myHandler)
      )

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        myHandler.unsafeOnNext(Observable[VModifier](Observable[VModifier](cls := "hans")))
        element.innerHTML shouldBe """<div class="hans"></div>"""
      }

    }
  }

  it should "work for triple nested stream modifier" in {

    IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>

      val node = div(idAttr := "strings",
        div(myHandler)
      )

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        myHandler.unsafeOnNext(Observable[VModifier](Observable[VModifier](Observable(cls := "hans"))))
        element.innerHTML shouldBe """<div class="hans"></div>"""
      }

    }
  }

  it should "work for multiple nested stream modifier" in {

    IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>

      val node = div(idAttr := "strings",
        div(myHandler)
      )

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        myHandler.unsafeOnNext(Observable[VModifier](VModifier(Observable[VModifier]("a"), Observable(span("b")))))
        element.innerHTML shouldBe """<div>a<span>b</span></div>"""
      }

    }
  }

  it should "work for nested attribute stream receiver" in {

    IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>

      val node = div(idAttr := "strings",
        div(myHandler)
      )

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe "<div></div>"

        myHandler.unsafeOnNext(cls <-- Observable("hans"))
        element.innerHTML shouldBe """<div class="hans"></div>"""
      }

    }
  }

  it should "work for nested emitter" in {

    IO(Subject.replayLatest[VModifier]()).flatMap { myHandler =>

      val node = div(idAttr := "strings",
        div(idAttr := "click", myHandler)
      )

      Outwatch.renderInto[IO]("#app", node).map { _ =>

        val element = document.getElementById("strings")
        element.innerHTML shouldBe """<div id="click"></div>"""

        var clickCounter = 0
        myHandler.unsafeOnNext(onClick doAction (clickCounter += 1))
        element.innerHTML shouldBe """<div id="click"></div>"""

        clickCounter shouldBe 0
        sendEvent(document.getElementById("click"), "click")
        clickCounter shouldBe 1

        sendEvent(document.getElementById("click"), "click")
        clickCounter shouldBe 2

        myHandler.unsafeOnNext(VModifier.empty)
        element.innerHTML shouldBe """<div id="click"></div>"""

        sendEvent(document.getElementById("click"), "click")
        clickCounter shouldBe 2
      }

    }
  }

  it should "work for streaming accum attributes" in {

    IO(Subject.behavior("second")).flatMap { myClasses =>
    IO(Subject.replayLatest[String]()).flatMap { myClasses2 =>

      val node = div(
        idAttr := "strings",
        div(
          cls := "first",
          myClasses.map { cls := _ },
          Seq[VModifier](
            cls <-- myClasses2
          )
        )
      )

      Outwatch.renderInto[IO]("#app", node).map {_ =>
        val element = document.getElementById("strings")

        element.innerHTML shouldBe """<div class="first second"></div>"""

        myClasses2.unsafeOnNext("third")
        element.innerHTML shouldBe """<div class="first second third"></div>"""

        myClasses2.unsafeOnNext("more")
        element.innerHTML shouldBe """<div class="first second more"></div>"""

        myClasses.unsafeOnNext("yeah")
        element.innerHTML shouldBe """<div class="first yeah more"></div>"""
      }

    }}
  }

  "LocalStorage" should "provide a handler" in {

    val key = "banana"
    val triggeredHandlerEvents = mutable.ArrayBuffer.empty[Option[String]]

    assert(localStorage.getItem(key) == null)

    util.LocalStorage.handler[IO](key).flatMap { storageHandler =>

      storageHandler.unsafeForeach{e => triggeredHandlerEvents += e}
      assert(localStorage.getItem(key) == null)
      assert(triggeredHandlerEvents.toList == List(None))

      storageHandler.unsafeOnNext(Some("joe"))
      assert(localStorage.getItem(key) == "joe")
      assert(triggeredHandlerEvents.toList == List(None, Some("joe")))

      var initialValue:Option[String] = null

      util.LocalStorage.handler[IO](key).map { sh =>

        sh.unsafeForeach {initialValue = _}
        assert(initialValue == Some("joe"))

        storageHandler.unsafeOnNext(None)
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
        storageHandler.unsafeOnNext(None)
        assert(localStorage.getItem(key) == null)
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None, Some("split"), None))

        storageHandler.unsafeOnNext(Some("rhabarbar"))
        assert(localStorage.getItem(key) == "rhabarbar")
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None, Some("split"), None, Some("rhabarbar")))

        // localStorage.clear() from another window
        dispatchStorageEvent(null, null, null)
        assert(localStorage.getItem(key) == null)
        assert(triggeredHandlerEvents.toList == List(None, Some("joe"), None, Some("split"), None, Some("rhabarbar"), None))
      }
    }
  }

  "Observer/Observable types" should "work for behavior Subject" in {

    var mounts = 0
    var unmounts = 0
    var updates = 0

    for {
       myHandler <- IO(Subject.behavior(-1))
      clsHandler <- IO(Subject.behavior("one"))
            node = div(
                      idAttr := "strings",
                      myHandler,
                      cls <-- clsHandler,
                      onClick.asDelay(0) --> myHandler,
                      onDomMount doAction  { mounts += 1 },
                      onDomUnmount doAction  { unmounts += 1 },
                      onDomUpdate doAction  { updates += 1 }
                    )
          _ <- Outwatch.renderInto[IO]("#app", node)
    } yield {

      val element = document.getElementById("strings")
      element.outerHTML shouldBe """<div id="strings" class="one">-1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 0

      myHandler.unsafeOnNext(1)
      element.outerHTML shouldBe """<div id="strings" class="one">1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 1

      sendEvent(document.getElementById("strings"), "click")

      element.outerHTML shouldBe """<div id="strings" class="one">0</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 2

      clsHandler.unsafeOnNext("two")
      element.outerHTML shouldBe """<div id="strings" class="two">0</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 3
    }
  }

  it should "work for empty publish Subject" in {

    var mounts = 0
    var unmounts = 0
    var updates = 0

    for {
       myHandler <- IO(Subject.publish[Int]())
      clsHandler <- IO(Subject.publish[String]())
            node = div(
                      idAttr := "strings",
                      myHandler,
                      cls <-- clsHandler,
                      onClick.as(0) --> myHandler,
                      onDomMount doAction  { mounts += 1 },
                      onDomUnmount doAction  { unmounts += 1 },
                      onDomUpdate doAction  { updates += 1 }
                    )
          _ <- Outwatch.renderInto[IO]("#app", node)
    } yield {

      val element = document.getElementById("strings")
      element.outerHTML shouldBe """<div id="strings"></div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 0

      myHandler.unsafeOnNext(-1)
      element.outerHTML shouldBe """<div id="strings">-1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 1

      clsHandler.unsafeOnNext("one")
      element.outerHTML shouldBe """<div id="strings" class="one">-1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 2

      myHandler.unsafeOnNext(1)
      element.outerHTML shouldBe """<div id="strings" class="one">1</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 3

      sendEvent(document.getElementById("strings"), "click")

      element.outerHTML shouldBe """<div id="strings" class="one">0</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 4

      clsHandler.unsafeOnNext("two")
      element.outerHTML shouldBe """<div id="strings" class="two">0</div>"""
      mounts shouldBe 1
      unmounts shouldBe 0
      updates shouldBe 5
    }
  }

  "ChildCommand" should "work in handler" in {
    val cmds = Subject.replayLatest[ChildCommand]()

    val node = div(
      idAttr := "strings",
      cmds
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")
      element.innerHTML shouldBe ""

      cmds.unsafeOnNext(ChildCommand.ReplaceAll(b("Hello World") :: span("!") :: Nil))
      element.innerHTML shouldBe """<b>Hello World</b><span>!</span>"""

      cmds.unsafeOnNext(ChildCommand.Insert(1, p("and friends", dsl.key := 42)))
      element.innerHTML shouldBe """<b>Hello World</b><p>and friends</p><span>!</span>"""

      cmds.unsafeOnNext(ChildCommand.Move(1, 2))
      element.innerHTML shouldBe """<b>Hello World</b><span>!</span><p>and friends</p>"""

      cmds.unsafeOnNext(ChildCommand.MoveId(ChildCommand.ChildId.Key(42), 1))
      element.innerHTML shouldBe """<b>Hello World</b><p>and friends</p><span>!</span>"""

      cmds.unsafeOnNext(ChildCommand.RemoveId(ChildCommand.ChildId.Key(42)))
      element.innerHTML shouldBe """<b>Hello World</b><span>!</span>"""
    }
  }

  it should "work in handler with element reference" in {
    val initialChildren =
      p(
        "How much?",
        dsl.key := "question-1",
        idAttr := "id-1"
      ) ::
        p(
          "Why so cheap?",
          dsl.key := "question-2",
          idAttr := "id-2"

        ) ::
        Nil

    val cmds = Subject.behavior[ChildCommand](ChildCommand.ReplaceAll(initialChildren))

    val node = div(
      "Questions?",
      div(
        idAttr := "strings",
        onClick.map(ev => ChildCommand.RemoveId(ChildCommand.ChildId.Element(ev.target))) --> cmds,
        cmds
      )
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")
      element.innerHTML shouldBe """<p id="id-1">How much?</p><p id="id-2">Why so cheap?</p>"""

      sendEvent(document.getElementById("id-1"), "click")

      element.innerHTML shouldBe """<p id="id-2">Why so cheap?</p>"""

      cmds.unsafeOnNext(ChildCommand.InsertBeforeId(ChildCommand.ChildId.Key("question-2"), div("spam")))
      element.innerHTML shouldBe """<div>spam</div><p id="id-2">Why so cheap?</p>"""

      cmds.unsafeOnNext(ChildCommand.MoveId(ChildCommand.ChildId.Key("question-2"), 0))
      element.innerHTML shouldBe """<p id="id-2">Why so cheap?</p><div>spam</div>"""

      sendEvent(document.getElementById("id-2"), "click")
      element.innerHTML shouldBe """<div>spam</div>"""
    }
  }

  it should "work in value observable" in {
    val cmds = Subject.replayLatest[ChildCommand]()

    val node = div(
      idAttr := "strings",
      cmds.prepend(ChildCommand.ReplaceAll(div("huch") :: Nil))
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")
      element.innerHTML shouldBe "<div>huch</div>"

      cmds.unsafeOnNext(ChildCommand.Prepend(b("nope")))
      element.innerHTML shouldBe """<b>nope</b><div>huch</div>"""

      cmds.unsafeOnNext(ChildCommand.Move(0, 1))
      element.innerHTML shouldBe """<div>huch</div><b>nope</b>"""

      cmds.unsafeOnNext(ChildCommand.Replace(1, b("yes")))
      element.innerHTML shouldBe """<div>huch</div><b>yes</b>"""
    }
  }

  "Emitter subscription" should "be correctly subscribed" in {

    val clicks = Subject.behavior[Int](0)

    var incCounter =  0
    var mapCounter = 0
    val innerMod  = onClick.transformLift(_ => clicks.map { c => mapCounter += 1; c }) doAction { incCounter += 1 }
    val modHandler = Subject.behavior(innerMod)

    val innerNode = div(modHandler)
    val nodeHandler = Subject.behavior(innerNode)

    val node = div(
      idAttr := "strings",
      nodeHandler
    )

    incCounter shouldBe 0
    mapCounter shouldBe 0

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      incCounter shouldBe 1
      mapCounter shouldBe 1

      clicks.unsafeOnNext(1)
      incCounter shouldBe 2
      mapCounter shouldBe 2

      modHandler.unsafeOnNext(VModifier.empty)
      incCounter shouldBe 2
      mapCounter shouldBe 2

      clicks.unsafeOnNext(2)
      incCounter shouldBe 2
      mapCounter shouldBe 2

      modHandler.unsafeOnNext(innerMod)
      incCounter shouldBe 3
      mapCounter shouldBe 3

      clicks.unsafeOnNext(3)
      incCounter shouldBe 4
      mapCounter shouldBe 4

      nodeHandler.unsafeOnNext(span)
      incCounter shouldBe 4
      mapCounter shouldBe 4

      clicks.unsafeOnNext(4)
      incCounter shouldBe 4
      mapCounter shouldBe 4
    }
  }

  it should "be correctly subscribed for emitterbuilder.asLatestEmitter" in {

    var aCounter =  0
    var bCounter = 0
    var lastValue: String = null

    val aEvent = Subject.publish[String]()
    val bEvent = Subject.publish[String]()
    val innerNode =
      input(EmitterBuilder.fromSource(aEvent).map { x => aCounter += 1; x }.asLatestEmitter(EmitterBuilder.fromSource(bEvent).map { x => bCounter += 1; x }) foreach { str =>
        lastValue = str
      })

    val handler = Subject.behavior[VModifier](innerNode)
    val node = div(
      idAttr := "strings",
      handler
    )

    aEvent.unsafeOnNext("nope?")
    bEvent.unsafeOnNext("nope?")
    aCounter shouldBe 0
    bCounter shouldBe 0
    lastValue shouldBe null

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      aCounter shouldBe 0
      bCounter shouldBe 0
      lastValue shouldBe null

      aEvent.unsafeOnNext("a")
      aCounter shouldBe 1
      bCounter shouldBe 0
      lastValue shouldBe null

      bEvent.unsafeOnNext("b")
      aCounter shouldBe 1
      bCounter shouldBe 1
      lastValue shouldBe null

      aEvent.unsafeOnNext("a2")
      aCounter shouldBe 2
      bCounter shouldBe 1
      lastValue shouldBe "b"

      bEvent.unsafeOnNext("ahja")
      aCounter shouldBe 2
      bCounter shouldBe 2
      lastValue shouldBe "b"

      aEvent.unsafeOnNext("oh")
      aCounter shouldBe 3
      bCounter shouldBe 2
      lastValue shouldBe "ahja"

      handler.unsafeOnNext(VModifier.empty)

      aEvent.unsafeOnNext("hmm?")
      aCounter shouldBe 3
      bCounter shouldBe 2
      lastValue shouldBe "ahja"

      bEvent.unsafeOnNext("no?")
      aCounter shouldBe 3
      bCounter shouldBe 2
      lastValue shouldBe "ahja"

    }
  }

  "Thunk" should "work" in {
    val myString: Subject[String] = Subject.replayLatest[String]()

    var mountCount = 0
    var preupdateCount = 0
    var updateCount = 0
    var unmountCount = 0
    var renderFnCounter = 0
    val node = div(
      idAttr := "strings",
      myString.map { myString =>
        b(idAttr := "bla").thunk("component")(myString){
          renderFnCounter += 1
          VModifier(cls := "b", myString, onDomMount.doAction { mountCount += 1 }, onDomPreUpdate.doAction { preupdateCount += 1 }, onDomUpdate.doAction { updateCount += 1 }, onDomUnmount.doAction { unmountCount += 1 })
        }
      },
      b("something else")
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      mountCount shouldBe 0
      preupdateCount shouldBe 0
      updateCount shouldBe 0
      unmountCount shouldBe 0
      element.innerHTML shouldBe "<b>something else</b>"

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      mountCount shouldBe 1
      preupdateCount shouldBe 0
      updateCount shouldBe 0
      unmountCount shouldBe 0
      element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      mountCount shouldBe 1
      preupdateCount shouldBe 1
      updateCount shouldBe 1
      unmountCount shouldBe 0
      element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("hai!")
      renderFnCounter shouldBe 2
      mountCount shouldBe 2
      preupdateCount shouldBe 1
      updateCount shouldBe 1
      unmountCount shouldBe 1
      element.innerHTML shouldBe """<b id="bla" class="b">hai!</b><b>something else</b>"""

      myString.unsafeOnNext("fuchs.")
      renderFnCounter shouldBe 3
      mountCount shouldBe 3
      preupdateCount shouldBe 1
      updateCount shouldBe 1
      unmountCount shouldBe 2
      element.innerHTML shouldBe """<b id="bla" class="b">fuchs.</b><b>something else</b>"""
    }
  }

  it should "work with equals" in {
    val myString: Subject[String] = Subject.replayLatest[String]()

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
      idAttr := "strings",
      myString.map { myString =>
        b(idAttr := "bla").thunk("component")(new Wrap(myString)) {
          renderFnCounter += 1
          Seq[VModifier](cls := "b", myString)
        }
      },
      b("something else")
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      equalsCounter shouldBe 0
      element.innerHTML shouldBe "<b>something else</b>"

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      equalsCounter shouldBe 0
      element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      equalsCounter shouldBe 1
      element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("hai!")
      renderFnCounter shouldBe 2
      equalsCounter shouldBe 2
      element.innerHTML shouldBe """<b id="bla" class="b">hai!</b><b>something else</b>"""
    }
  }

  it should "work with inner stream" in {
    val myString: Subject[String] = Subject.replayLatest[String]()
    val myThunk: Subject[Unit] = Subject.replayLatest[Unit]()
    

    var renderFnCounter = 0
    val node = div(
      idAttr := "strings",
      myThunk.map { _ =>
        b.thunkStatic("component") {
          renderFnCounter += 1
          myString.map { str =>
            p(dsl.key := str, str)
          }
        }
      }
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      element.innerHTML shouldBe ""

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b></b>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b></b>"

      myString.unsafeOnNext("ok?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b><p>ok?</p></b>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b><p>ok?</p></b>"

      myString.unsafeOnNext("hope so")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b><p>hope so</p></b>"

      myString.unsafeOnNext("ohai")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b><p>ohai</p></b>"
    }
  }

  it should "work with inner and adjacent stream" in {
    val myString: Subject[String] = Subject.replayLatest[String]()
    val myOther: Subject[String] = Subject.replayLatest[String]()
    val myThunk: Subject[Int] = Subject.replayLatest[Int]()

    var renderFnCounter = 0
    val node = div(
      idAttr := "strings",
      myThunk.map { i =>
        b.thunk("component")(i) {
          renderFnCounter += 1
          VModifier(
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

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      element.innerHTML shouldBe ""

      myThunk.unsafeOnNext(0)
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0</b>"

      myThunk.unsafeOnNext(0)
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0</b>"

      myString.unsafeOnNext("ok?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0<p>ok?</p></b>"

      myString.unsafeOnNext("got it?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0<p>got it?</p></b>"

      myOther.unsafeOnNext("nope")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<b>0<p>got it?</p></b>nope"

      myThunk.unsafeOnNext(1)
      renderFnCounter shouldBe 2
      element.innerHTML shouldBe "<b>1<p>got it?</p></b>nope"

      myThunk.unsafeOnNext(2)
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>got it?</p></b>nope"

      myOther.unsafeOnNext("yes")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>got it?</p></b>yes"

      myString.unsafeOnNext("sad?")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>sad?</p></b>yes"

      myString.unsafeOnNext("hungry?")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>hungry?</p></b>yes"

      myString.unsafeOnNext("thursty?")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>thursty?</p></b>yes"

      myOther.unsafeOnNext("maybe")
      renderFnCounter shouldBe 3
      element.innerHTML shouldBe "<b>2<p>thursty?</p></b>maybe"

      myThunk.unsafeOnNext(3)
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<p>thursty?</p></b>maybe"

      myString.unsafeOnNext("")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<span>nope</span></b>maybe"

      myOther.unsafeOnNext("come on")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<span>nope</span></b>come on"

      myThunk.unsafeOnNext(3)
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<span>nope</span></b>come on"

      myOther.unsafeOnNext("hey?")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<span>nope</span></b>hey?"

      myString.unsafeOnNext("oh")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<p>oh</p></b>hey?"

      myOther.unsafeOnNext("ah")
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<p>oh</p></b>ah"

      myThunk.unsafeOnNext(3)
      renderFnCounter shouldBe 4
      element.innerHTML shouldBe "<b>3<p>oh</p></b>ah"

      myThunk.unsafeOnNext(4)
      renderFnCounter shouldBe 5
      element.innerHTML shouldBe "<b>4<p>oh</p></b>ah"
    }
  }

  it should "work with nested inner stream" in {
    val myString: Subject[String] = Subject.replayLatest[String]()
    val myInner: Subject[String] = Subject.replayLatest[String]()
    val myInnerOther: Subject[String] = Subject.replayLatest[String]()
    val myOther: Subject[String] = Subject.replayLatest[String]()
    val myThunk: Subject[Unit] = Subject.replayLatest[Unit]()
    

    var renderFnCounter = 0
    val node = div(
      idAttr := "strings",
      myThunk.map { _ =>
        div(
          b.thunkStatic("component") {
            renderFnCounter += 1
            myString.map { str =>
              div(
                if (str.isEmpty) div("empty") else p.thunk("inner")(str)(VModifier(str, myInner)),
                myInnerOther
              )
            }
          },
          myOther
        )
      }
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      element.innerHTML shouldBe ""

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b></b></div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b></b></div>"

      myString.unsafeOnNext("ok?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      myString.unsafeOnNext("ok?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      myString.unsafeOnNext("hope so")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>hope so</p></div></b></div>"

      myString.unsafeOnNext("ohai")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ohai</p></div></b></div>"

      myString.unsafeOnNext("")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><div>empty</div></div></b></div>"

      myInner.unsafeOnNext("!")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><div>empty</div></div></b></div>"

      myString.unsafeOnNext("torst")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>torst!</p></div></b></div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>torst!</p></div></b></div>"

      myInner.unsafeOnNext("?")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>torst?</p></div></b></div>"

      myString.unsafeOnNext("gandalf")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b></div>"

      myString.unsafeOnNext("gandalf")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b></div>"

      myOther.unsafeOnNext("du")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b>du</div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b>du</div>"

      myInner.unsafeOnNext("!")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gandalf!</p></div></b>du</div>"

      myString.unsafeOnNext("fanta")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      myString.unsafeOnNext("fanta")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      myOther.unsafeOnNext("nee")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>nee</div>"

      myInnerOther.unsafeOnNext("muh")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>fanta!</p>muh</div></b>nee</div>"

      myString.unsafeOnNext("ghost")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      myString.unsafeOnNext("ghost")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      myOther.unsafeOnNext("life")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      myInnerOther.unsafeOnNext("muh")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      myString.unsafeOnNext("ghost")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      myOther.unsafeOnNext("seen")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>seen</div>"

      myInnerOther.unsafeOnNext("muh")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>seen</div>"

      myString.unsafeOnNext("")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><div>empty</div>muh</div></b>seen</div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><div>empty</div>muh</div></b>seen</div>"

      myString.unsafeOnNext("gott")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>muh</div></b>seen</div>"

      myOther.unsafeOnNext("dorf")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>muh</div></b>dorf</div>"

      myInnerOther.unsafeOnNext("ach")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      myString.unsafeOnNext("gott")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      myInner.unsafeOnNext("hans")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gotthans</p>ach</div></b>dorf</div>"

      myOther.unsafeOnNext("das")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gotthans</p>ach</div></b>das</div>"

      myInnerOther.unsafeOnNext("tank")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gotthans</p>tank</div></b>das</div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gotthans</p>tank</div></b>das</div>"

      myInner.unsafeOnNext("so")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gottso</p>tank</div></b>das</div>"

      myOther.unsafeOnNext("datt")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gottso</p>tank</div></b>datt</div>"

      myInnerOther.unsafeOnNext("ohje")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gottso</p>ohje</div></b>datt</div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>gottso</p>ohje</div></b>datt</div>"

      myString.unsafeOnNext("ende")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>datt</div>"

      myString.unsafeOnNext("ende")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>datt</div>"

      myOther.unsafeOnNext("fin")
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>fin</div>"

      myThunk.unsafeOnNext(())
      renderFnCounter shouldBe 1
      element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>fin</div>"
    }
  }

  it should "work with streams (switchMap)" in {
    val myString: Subject[String] = Subject.replayLatest[String]()
    val myId: Subject[String] = Subject.replayLatest[String]()
    val myInner: Subject[String] = Subject.replayLatest[String]()
    val myOther: Subject[VModifier] = Subject.replayLatest[VModifier]()
    val thunkContent: Subject[VModifier] = Subject.replayLatest[VModifier]()

    var renderFnCounter = 0
    var mounts = List.empty[Int]
    var preupdates = List.empty[Int]
    var updates = List.empty[Int]
    var unmounts = List.empty[Int]
    var counter = 0
    def mountHooks = {
      val c = counter
      counter += 1
      VModifier(onDomMount.doAction { mounts :+= c }, onDomPreUpdate.doAction { preupdates :+= c }, onDomUpdate.doAction { updates :+= c }, onDomUnmount.doAction { unmounts :+= c } )
    }
    val node = div(
      idAttr := "strings",
      myString.switchMap { myString =>
        if (myString == "empty") colibri.Observable(b.thunk("component")(myString)(VModifier("empty", mountHooks)))
        else myInner.map[VNode](s => div(s, mountHooks)).prepend(b(idAttr <-- myId).thunk("component")(myString) {
          renderFnCounter += 1
          VModifier(cls := "b", myString, mountHooks, thunkContent)
        })
      },
      myOther,
      b("something else")
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      mounts shouldBe Nil
      preupdates shouldBe Nil
      updates shouldBe Nil
      unmounts shouldBe Nil
      element.innerHTML shouldBe "<b>something else</b>"

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe Nil
      updates shouldBe Nil
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b">wal?</b><b>something else</b>"""

      myId.unsafeOnNext("tier")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe List(0)
      updates shouldBe List(0)
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe List(0, 0)
      updates shouldBe List(0, 0)
      unmounts shouldBe List()
      element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("hai!")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1)
      preupdates shouldBe List(0, 0)
      updates shouldBe List(0, 0)
      unmounts shouldBe List(0)
      element.innerHTML shouldBe """<b class="b" id="tier">hai!</b><b>something else</b>"""

      myId.unsafeOnNext("nope")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0)
      element.innerHTML shouldBe """<b class="b" id="nope">hai!</b><b>something else</b>"""

      myString.unsafeOnNext("empty")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1, 2)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1)
      element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      myId.unsafeOnNext("nothing")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1, 2)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1)
      element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      myString.unsafeOnNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="nothing" class="b">hans</b><b>something else</b>"""

      myId.unsafeOnNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3)
      updates shouldBe List(0, 0, 1, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      myString.unsafeOnNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      thunkContent.unsafeOnNext(p(dsl.key := "1", "el dieter"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter</p></b><b>something else</b>"""

      thunkContent.unsafeOnNext(p(dsl.key := "2", "el dieter II"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><b>something else</b>"""

      myOther.unsafeOnNext(div("baem!"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><div>baem!</div><b>something else</b>"""

      myInner.unsafeOnNext("meh")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3, 4)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2, 3)
      element.innerHTML shouldBe """<div>meh</div><div>baem!</div><b>something else</b>"""

      myOther.unsafeOnNext(div("fini"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3, 4)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2, 3)
      element.innerHTML shouldBe """<div>meh</div><div>fini</div><b>something else</b>"""
    }
  }

  it should "work with streams (flatMap)" in {
    val myString: Subject[String] = Subject.replayLatest[String]()
    val myId: Subject[String] = Subject.replayLatest[String]()
    val myInner: Subject[String] = Subject.replayLatest[String]()
    val myOther: Subject[VModifier] = Subject.replayLatest[VModifier]()
    val thunkContent: Subject[VModifier] = Subject.replayLatest[VModifier]()

    var renderFnCounter = 0
    var mounts = List.empty[Int]
    var preupdates = List.empty[Int]
    var updates = List.empty[Int]
    var unmounts = List.empty[Int]
    var counter = 0
    def mountHooks = {
      val c = counter
      counter += 1
      VModifier(onDomMount.doAction { mounts :+= c }, onDomPreUpdate.doAction { preupdates :+= c }, onDomUpdate.doAction { updates :+= c }, onDomUnmount.doAction { unmounts :+= c } )
    }
    val node = div(
      idAttr := "strings",
      myString.switchMap { myString =>
        if (myString == "empty") Observable(b.thunk("component")(myString)(VModifier("empty", mountHooks)))
        else myInner.map[VNode](s => div(s, mountHooks)).prepend(b(idAttr <-- myId).thunk("component")(myString) {
          renderFnCounter += 1
          VModifier(cls := "b", myString, mountHooks, thunkContent)
        })
      },
      myOther,
      b("something else")
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      mounts shouldBe Nil
      preupdates shouldBe Nil
      updates shouldBe Nil
      unmounts shouldBe Nil
      element.innerHTML shouldBe "<b>something else</b>"

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe Nil
      updates shouldBe Nil
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b">wal?</b><b>something else</b>"""

      myId.unsafeOnNext("tier")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe List(0)
      updates shouldBe List(0)
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe List(0, 0)
      updates shouldBe List(0, 0)
      unmounts shouldBe List()
      element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("hai!")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1)
      preupdates shouldBe List(0, 0)
      updates shouldBe List(0, 0)
      unmounts shouldBe List(0)
      element.innerHTML shouldBe """<b class="b" id="tier">hai!</b><b>something else</b>"""

      myId.unsafeOnNext("nope")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0)
      element.innerHTML shouldBe """<b class="b" id="nope">hai!</b><b>something else</b>"""

      myString.unsafeOnNext("empty")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1, 2)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1)
      element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      myId.unsafeOnNext("nothing")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1, 2)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1)
      element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      myString.unsafeOnNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="nothing" class="b">hans</b><b>something else</b>"""

      myId.unsafeOnNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3)
      updates shouldBe List(0, 0, 1, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      myString.unsafeOnNext("hans")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      thunkContent.unsafeOnNext(p(dsl.key := "1", "el dieter"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter</p></b><b>something else</b>"""

      thunkContent.unsafeOnNext(p(dsl.key := "2", "el dieter II"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><b>something else</b>"""

      myOther.unsafeOnNext(div("baem!"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><div>baem!</div><b>something else</b>"""

      myInner.unsafeOnNext("meh")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3, 4)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2, 3)
      element.innerHTML shouldBe """<div>meh</div><div>baem!</div><b>something else</b>"""

      myOther.unsafeOnNext(div("fini"))
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3, 4)
      preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      unmounts shouldBe List(0, 1, 2, 3)
      element.innerHTML shouldBe """<div>meh</div><div>fini</div><b>something else</b>"""
    }
  }

  it should "work with streams" in {
    val myString: Subject[String] = Subject.replayLatest[String]()
    val myId: Subject[String] = Subject.replayLatest[String]()
    val myInner: Subject[String] = Subject.replayLatest[String]()
    val myOther: Subject[VModifier] = Subject.replayLatest[VModifier]()
    val thunkContent: Subject[VModifier] = Subject.replayLatest[VModifier]()

    var renderFnCounter = 0
    var mounts = List.empty[Int]
    var preupdates = List.empty[Int]
    var updates = List.empty[Int]
    var unmounts = List.empty[Int]
    var counter = 0
    def mountHooks = {
      val c = counter
      counter += 1
      VModifier(onDomMount.doAction { mounts :+= c }, onDomPreUpdate.doAction { preupdates :+= c }, onDomUpdate.doAction { updates :+= c }, onDomUnmount.doAction { unmounts :+= c } )
    }
    val node = div(
      idAttr := "strings",
      myString.map { myString =>
        if (myString == "empty") VModifier(b.thunk("component")(myString)(VModifier("empty", mountHooks)))
        else VModifier(myInner.map[VNode](s => div(s, mountHooks)).prepend(b(idAttr <-- myId).thunk("component")(myString) {
          renderFnCounter += 1
          VModifier(cls := "b", myString, mountHooks, thunkContent)
        }))
      },
      myOther,
      b("something else")
    )

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val element = document.getElementById("strings")

      renderFnCounter shouldBe 0
      mounts shouldBe Nil
      preupdates shouldBe Nil
      updates shouldBe Nil
      unmounts shouldBe Nil
      element.innerHTML shouldBe "<b>something else</b>"

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe Nil
      updates shouldBe Nil
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b">wal?</b><b>something else</b>"""

      myId.unsafeOnNext("tier")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe List(0)
      updates shouldBe List(0)
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      myId.unsafeOnNext("tier")
      renderFnCounter shouldBe 1
      mounts shouldBe List(0)
      preupdates shouldBe List(0, 0)
      updates shouldBe List(0, 0)
      unmounts shouldBe Nil
      element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("wal?")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1)
      preupdates shouldBe List(0, 0)
      updates shouldBe List(0, 0)
      unmounts shouldBe List(0)
      element.innerHTML shouldBe """<b id="tier" class="b">wal?</b><b>something else</b>"""

      myId.unsafeOnNext("tier")
      renderFnCounter shouldBe 2
      mounts shouldBe List(0, 1)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0)
      element.innerHTML shouldBe """<b id="tier" class="b">wal?</b><b>something else</b>"""

      myString.unsafeOnNext("hai!")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2)
      preupdates shouldBe List(0, 0, 1)
      updates shouldBe List(0, 0, 1)
      unmounts shouldBe List(0, 1)
      element.innerHTML shouldBe """<b id="tier" class="b">hai!</b><b>something else</b>"""

      myId.unsafeOnNext("nope")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2)
      preupdates shouldBe List(0, 0, 1, 2)
      updates shouldBe List(0, 0, 1, 2)
      unmounts shouldBe List(0, 1)
      element.innerHTML shouldBe """<b id="nope" class="b">hai!</b><b>something else</b>"""

      myString.unsafeOnNext("empty")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 2)
      updates shouldBe List(0, 0, 1, 2)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      myId.unsafeOnNext("nothing")
      renderFnCounter shouldBe 3
      mounts shouldBe List(0, 1, 2, 3)
      preupdates shouldBe List(0, 0, 1, 2)
      updates shouldBe List(0, 0, 1, 2)
      unmounts shouldBe List(0, 1, 2)
      element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      myString.unsafeOnNext("hans")
      renderFnCounter shouldBe 4
      mounts shouldBe List(0, 1, 2, 3, 4)
      preupdates shouldBe List(0, 0, 1, 2)
      updates shouldBe List(0, 0, 1, 2)
      unmounts shouldBe List(0, 1, 2, 3)
      element.innerHTML shouldBe """<b id="nothing" class="b">hans</b><b>something else</b>"""

      myId.unsafeOnNext("hans")
      renderFnCounter shouldBe 4
      mounts shouldBe List(0, 1, 2, 3, 4)
      preupdates shouldBe List(0, 0, 1, 2, 4)
      updates shouldBe List(0, 0, 1, 2, 4)
      unmounts shouldBe List(0, 1, 2, 3)
      element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      myString.unsafeOnNext("hans")
      renderFnCounter shouldBe 5
      mounts shouldBe List(0, 1, 2, 3, 4, 5)
      preupdates shouldBe List(0, 0, 1, 2, 4)
      updates shouldBe List(0, 0, 1, 2, 4)
      unmounts shouldBe List(0, 1, 2, 3, 4)
      element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      thunkContent.unsafeOnNext(p(dsl.key := "1", "el dieter"))
      renderFnCounter shouldBe 5
      mounts shouldBe List(0, 1, 2, 3, 4, 5)
      preupdates shouldBe List(0, 0, 1, 2, 4, 5)
      updates shouldBe List(0, 0, 1, 2, 4, 5)
      unmounts shouldBe List(0, 1, 2, 3, 4)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter</p></b><b>something else</b>"""

      thunkContent.unsafeOnNext(p(dsl.key := "2", "el dieter II"))
      renderFnCounter shouldBe 5
      mounts shouldBe List(0, 1, 2, 3, 4, 5)
      preupdates shouldBe List(0, 0, 1, 2, 4, 5, 5)
      updates shouldBe List(0, 0, 1, 2, 4, 5, 5)
      unmounts shouldBe List(0, 1, 2, 3, 4)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><b>something else</b>"""

      myOther.unsafeOnNext(div("baem!"))
      renderFnCounter shouldBe 5
      mounts shouldBe List(0, 1, 2, 3, 4, 5)
      preupdates shouldBe List(0, 0, 1, 2, 4, 5, 5)
      updates shouldBe List(0, 0, 1, 2, 4, 5, 5)
      unmounts shouldBe List(0, 1, 2, 3, 4)
      element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><div>baem!</div><b>something else</b>"""

      myInner.unsafeOnNext("meh")
      renderFnCounter shouldBe 5
      mounts shouldBe List(0, 1, 2, 3, 4, 5, 6)
      preupdates shouldBe List(0, 0, 1, 2, 4, 5, 5)
      updates shouldBe List(0, 0, 1, 2, 4, 5, 5)
      unmounts shouldBe List(0, 1, 2, 3, 4, 5)
      element.innerHTML shouldBe """<div>meh</div><div>baem!</div><b>something else</b>"""

      myOther.unsafeOnNext(div("fini"))
      renderFnCounter shouldBe 5
      mounts shouldBe List(0, 1, 2, 3, 4, 5, 6)
      preupdates shouldBe List(0, 0, 1, 2, 4, 5, 5)
      updates shouldBe List(0, 0, 1, 2, 4, 5, 5)
      unmounts shouldBe List(0, 1, 2, 3, 4, 5)
      element.innerHTML shouldBe """<div>meh</div><div>fini</div><b>something else</b>"""
    }
  }

  // TODO: this test does not actually fail if it is wrong, but you just see an error message in the console output. Fix when merging error observable in OutwatchTracing.
  "Nested VNode" should "work without outdated patch" in {
    val myList: Subject[List[String]] = Subject.behavior[List[String]](List("hi"))
    val isSynced: Subject[Int] = Subject.behavior[Int](-1)
    import scala.concurrent.duration._

    var renderFnCounter = 0
    val node = div(
      div(
        idAttr := "strings",
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
      button("Edit", idAttr := "edit-button",
        onClick.asLatest(myList).map(l => l.init ++ l.lastOption.map(_ + "!")) --> myList,
        onClick.asLatest(isSynced).map(_ + 1) --> isSynced
      )
    )

    Outwatch.renderInto[IO]("#app", node).flatMap { _ =>
      val editButton = document.getElementById("edit-button")

      for {
        _ <- IO.unit
        _ = sendEvent(editButton, "click")
        _ <- IO.sleep(1.seconds)
        _ = sendEvent(editButton, "click")
        _ <- IO.sleep(1.seconds)
      } yield succeed
    }
  }

  // TODO: this test does not actually fail if it is wrong, but you just see an error message in the console output. Fix when merging error observable in OutwatchTracing.
  it should "work without outdated patch in thunk" in {
    val myList: Subject[List[String]] = Subject.behavior[List[String]](List("hi"))
    val isSynced: Subject[Int] = Subject.behavior[Int](-1)
    import scala.concurrent.duration._

    var renderFnCounter = 0
    val node = div(
      div(
        idAttr := "strings",
        myList.map(_.indices.map { i =>
          val counter = renderFnCounter
          renderFnCounter += 1
          div.thunkStatic(i) {

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
      button("Edit", idAttr := "edit-button",
        onClick.asLatest(myList).map(l => l.init ++ l.lastOption.map(_ + "!")) --> myList,
        onClick.asLatest(isSynced).map(_ + 1) --> isSynced
      )
    )

    Outwatch.renderInto[IO]("#app", node).flatMap { _ =>
      val editButton = document.getElementById("edit-button")

      for {
        _ <- IO.unit
        _ = sendEvent(editButton, "click")
        _ <- IO.sleep(1.seconds)
        _ = sendEvent(editButton, "click")
        _ <- IO.sleep(1.seconds)
      } yield succeed
    }
  }

  "Custom Emitter builder" should "work with events" in {
    import scala.concurrent.duration._

    val clickableView: EmitterBuilder.Sync[Boolean, VModifier] = EmitterBuilder[Boolean, VModifier] { sink =>
      VModifier(
        display.flex,
        minWidth := "0px",

        onMouseDown.as(true) --> sink,
        onMouseUp.as(false) --> sink
      )
    }

    for {
      handler <- IO(Subject.replayLatest[String]())
      node = div(
        idAttr := "strings",
        clickableView.map {
          case true => "yes"
          case false => "no"
        } --> handler,
        handler
      )
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _ = element.innerHTML shouldBe ""

      _ = sendEvent(element, "mousedown")
      _ <- IO.sleep(0.1.seconds)
      _ = element.innerHTML shouldBe "yes"

      _ = sendEvent(element, "mouseup")
      _ <- IO.sleep(0.1.seconds)
      _ = element.innerHTML shouldBe "no"
    } yield succeed
  }

  it should "work with events as combined emitterbuidler" in {
    import scala.concurrent.duration._

    val clickableView: EmitterBuilder.Sync[Boolean, VModifier] = EmitterBuilder[Boolean, VModifier] { sink =>
      VModifier(
        display.flex,
        minWidth := "0px",

        Monoid[EmitterBuilder[Boolean, VModifier]].combine(onMouseDown.as(true), onMouseUp.as(false)) --> sink
      )
    }

    for {
      handler <- IO(Subject.replayLatest[String]())
      node = div(
        idAttr := "strings",
        clickableView.map {
          case true => "yes"
          case false => "no"
        } --> handler,
        handler
      )
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _ = element.innerHTML shouldBe ""

      _ = sendEvent(element, "mousedown")
      _ <- IO.sleep(0.1.seconds)
      _ = element.innerHTML shouldBe "yes"

      _ = sendEvent(element, "mouseup")
      _ <- IO.sleep(0.1.seconds)
      _ = element.innerHTML shouldBe "no"
    } yield succeed
  }

  it should "report exception when rendering with RenderConfig.showError" in {

    case class MyException(value: String) extends Throwable {
      override def toString() = value
    }

    val ioException = MyException("io")
    val observableException = MyException("observable")

    val node = div(
      idAttr := "strings",
      "hallo: ",
      IO.raiseError[String](ioException),
      Observable.raiseError[String](observableException)
    )

    var errors = List.empty[Throwable]
    val cancelable = OutwatchTracing.error.unsafeForeach { throwable =>
      errors = throwable :: errors
    }

    for {
      _ <- Outwatch.renderInto[IO]("#app", node, RenderConfig.showError)
      element = document.getElementById("strings")

      _ = errors shouldBe List(ioException, observableException).reverse

      _ = element.innerHTML shouldBe """hallo: <div style="background-color: rgb(253, 242, 245); color: rgb(215, 0, 34); padding: 5px; display: inline-block;">ERROR: io</div><div style="background-color: rgb(253, 242, 245); color: rgb(215, 0, 34); padding: 5px; display: inline-block;">ERROR: observable</div>"""

      _ = cancelable.unsafeCancel()
    } yield succeed
  }

  it should "not report exception when rendering with RenderConfig.ignoreError" in {

    case class MyException(value: String) extends Throwable {
      override def toString() = value
    }

    val ioException = MyException("io")
    val observableException = MyException("observable")

    val node = div(
      idAttr := "strings",
      "hallo: ",
      IO.raiseError[String](ioException),
      Observable.raiseError[String](observableException)
    )

    var errors = List.empty[Throwable]
    val cancelable = OutwatchTracing.error.unsafeForeach { throwable =>
      errors = throwable :: errors
    }

    for {
      _ <- Outwatch.renderInto[IO]("#app", node, RenderConfig.ignoreError)
      element = document.getElementById("strings")

      _ = errors shouldBe List(ioException, observableException).reverse

      _ = element.innerHTML shouldBe """hallo: """

      _ = cancelable.unsafeCancel()
    } yield succeed
  }

  "Events while patching" should "fire for the correct dom node" in {

    val otherDiv = Subject.replayLatest[VNode]()
    var insertedFirst = 0
    var insertedSecond = 0
    var mountedFirst = 0
    var mountedSecond = 0

    val node = div(
      otherDiv.map(_(
        onSnabbdomInsert doAction {insertedFirst += 1},
        onDomMount doAction {mountedFirst += 1}
      )),
      div(
        onSnabbdomInsert doAction {insertedSecond += 1},
        onDomMount doAction {mountedSecond += 1}
      )
    )

    Outwatch.renderInto[SyncIO]("#app", node).unsafeRunSync()

    insertedFirst shouldBe 0
    mountedFirst shouldBe 0
    insertedSecond shouldBe 1
    mountedSecond shouldBe 1

    otherDiv.unsafeOnNext(div())

    insertedFirst shouldBe 1
    mountedFirst shouldBe 1
    insertedSecond shouldBe 1
    mountedSecond shouldBe 1

    otherDiv.unsafeOnNext(div("hallo"))

    insertedFirst shouldBe 1
    mountedFirst shouldBe 2
    insertedSecond shouldBe 1
    mountedSecond shouldBe 1
  }


  it should "fire for the correct dom node 2" in {
    // in this case, without keys, snabbdom patches the second node

    val otherDiv = Subject.behavior[Option[VNode]](None)
    var insertedFirst = 0
    var insertedSecond = 0
    var mountedFirst = 0
    var mountedSecond = 0

    val node = div(
      idAttr := "Foo",
      otherDiv.map(otherDiv => div(
        otherDiv.map(_(
          onSnabbdomInsert doAction {insertedFirst += 1},
          onDomMount doAction {mountedFirst += 1}
        )),
        div(
          onSnabbdomInsert doAction {insertedSecond += 1},
          onDomMount doAction {mountedSecond += 1}
        )
      ))
    )

    Outwatch.renderInto[SyncIO]("#app", node).unsafeRunSync()

    insertedFirst shouldBe 0
    mountedFirst shouldBe 0
    insertedSecond shouldBe 1
    mountedSecond shouldBe 1

    otherDiv.unsafeOnNext(Some(div()))

    insertedFirst shouldBe 0
    mountedFirst shouldBe 1
    insertedSecond shouldBe 2
    mountedSecond shouldBe 2

    otherDiv.unsafeOnNext(Some(div("hallo")))

    insertedFirst shouldBe 0
    mountedFirst shouldBe 2
    insertedSecond shouldBe 2
    mountedSecond shouldBe 3
  }
}
