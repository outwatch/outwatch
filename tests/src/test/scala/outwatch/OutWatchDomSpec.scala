package outwatch

import cats.{Monoid, Show}
import cats.implicits._
import cats.effect.{IO, SyncIO}
import org.scalajs.dom.{document, html, Element, Event}
import outwatch.helpers._
import outwatch.dsl._
import snabbdom.{DataObject, Hooks, VNodeProxy}
import outwatch.interpreter._
import colibri._
import org.scalajs.dom.EventInit

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON
import colibri.Subject
import colibri.reactive._

final case class Fixture(proxy: VNodeProxy)
class OutwatchDomSpec extends JSDomAsyncSpec {

  implicit def ListToJsArray[T](list: Seq[T]): js.Array[T] = list.toJSArray

  implicit def unsafeSubscriptionOwner[T]: SubscriptionOwner[T] = new SubscriptionOwner[T] {
    def own(owner: T)(subscription: () => Cancelable): T = {
      subscription()
      owner
    }
  }

  def sendEvent(elem: Element, eventType: String) = {
    val event = new Event(
      eventType,
      new EventInit {
        bubbles = true
        cancelable = false
      },
    )
    elem.dispatchEvent(event)
  }

  def newProxy(tagName: String, dataObject: DataObject, string: String) = new VNodeProxy {
    sel = tagName
    data = dataObject
    text = string
  }

  def newProxy(tagName: String, dataObject: DataObject, childProxies: js.UndefOr[js.Array[VNodeProxy]] = js.undefined) =
    new VNodeProxy {
      sel = tagName
      data = dataObject
      children = childProxies
    }

  val nonEmptyObservable = Observable.fromEffect(IO.never)
  def nonEmptyObservable[A](values: A*) =
    Observable.merge(Observable.fromIterable(values), Observable.fromEffect(IO.never))

  "Properties" should "be separated correctly" in {
    val properties = Seq(
      BasicAttr("hidden", "true"),
      InitHook(_ => ()),
      InsertHook(_ => ()),
      UpdateHook((_, _) => ()),
      InsertHook(_ => ()),
      DestroyHook(_ => ()),
      PrePatchHook((_, _) => ()),
      PostPatchHook((_, _) => ()),
    )

    val propertiesArr = new MutableNestedArray[StaticVMod]
    properties.foreach(propertiesArr.push(_))
    val seps = SeparatedModifiers.from(propertiesArr, RenderConfig.default)
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

  "VMods" should "be separated correctly" in {
    val modifiers = Seq(
      BasicAttr("class", "red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      new StringVNode("Test"),
      div(),
      CompositeModifier(
        Seq[VMod](
          div(),
          attributes.`class` := "blue",
          attributes.onClick.as(1) doAction {},
          attributes.hidden <-- Observable(false),
        ),
      ),
      VMod(Observable.empty),
    )

    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
    val seps       = SeparatedModifiers.from(streamable.modifiers, RenderConfig.ignoreError)
    import seps._

    emitters.get.values.size shouldBe 1
    attrs.get.values.size shouldBe 2
    streamable.subscribables.toFlatArray.filterNot(_.isEmpty()).length shouldBe 0
    proxies.get.length shouldBe 3
  }

  it should "be separated correctly with non empty stream" in {
    val modifiers = Seq(
      BasicAttr("class", "red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      new StringVNode("Test"),
      div(),
      CompositeModifier(
        Seq[VMod](
          div(),
          attributes.`class` := "blue",
          attributes.onClick.as(1) doAction {},
          attributes.hidden <-- nonEmptyObservable(false),
        ),
      ),
      VMod(nonEmptyObservable),
    )

    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
    val seps       = SeparatedModifiers.from(streamable.modifiers, RenderConfig.ignoreError)
    import seps._

    emitters.get.values.size shouldBe 1
    attrs.get.values.size shouldBe 2
    streamable.subscribables.toFlatArray.filterNot(_.isEmpty()).length shouldBe 2
    proxies.get.length shouldBe 3
  }

  it should "be separated correctly with children" in {
    val modifiers: Seq[VMod] = Seq(
      BasicAttr("class", "red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input", _ => ()),
      VMod(Observable.empty),
      VMod(nonEmptyObservable),
      Emitter("keyup", _ => ()),
      StringVNode("text"),
      div(),
    )
    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
    val seps       = SeparatedModifiers.from(streamable.modifiers, RenderConfig.ignoreError)
    import seps._

    emitters.get.values.size shouldBe 3
    attrs.get.values.size shouldBe 1
    streamable.subscribables.toFlatArray.filterNot(_.isEmpty()).length shouldBe 1
    proxies.get.length shouldBe 2
  }

  it should "be separated correctly with string children" in {
    val modifiers: Seq[VMod] = Seq(
      BasicAttr("class", "red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input", _ => ()),
      Emitter("keyup", _ => ()),
      VMod(Observable.empty),
      VMod(nonEmptyObservable),
      StringVNode("text"),
      StringVNode("text2"),
    )

    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
    val seps       = SeparatedModifiers.from(streamable.modifiers, RenderConfig.ignoreError)
    import seps._

    emitters.get.values.size shouldBe 3
    attrs.get.values.size shouldBe 1
    streamable.subscribables.toFlatArray.filterNot(_.isEmpty()).length shouldBe 1
    proxies.get.length shouldBe 2
  }

  it should "be separated correctly with children and properties" in {

    val modifiers = Seq(
      BasicAttr("class", "red"),
      EmptyModifier,
      Emitter("click", _ => ()),
      Emitter("input", _ => ()),
      UpdateHook((_, _) => ()),
      VMod(Observable.empty),
      VMod(nonEmptyObservable),
      VMod(nonEmptyObservable),
      Emitter("keyup", _ => ()),
      InsertHook(_ => ()),
      PrePatchHook((_, _) => ()),
      PostPatchHook((_, _) => ()),
      StringVNode("text"),
    )

    val streamable = NativeModifiers.from(modifiers, RenderConfig.ignoreError)
    val seps       = SeparatedModifiers.from(streamable.modifiers, RenderConfig.ignoreError)
    import seps._

    emitters.get.keys.toList shouldBe List("click", "input", "keyup")
    insertHook.isDefined shouldBe true
    prePatchHook.isDefined shouldBe true
    updateHook.isDefined shouldBe true
    postPatchHook.isDefined shouldBe true
    destroyHook.isDefined shouldBe false
    attrs.get.values.size shouldBe 1
    keyOption.isEmpty shouldBe true
    streamable.subscribables.toFlatArray.filterNot(_.isEmpty()).length shouldBe 2
    proxies.get.length shouldBe 1
  }

  val fixture = Fixture(
    newProxy(
      "div",
      new DataObject {
        attrs = js.Dictionary[Attr.Value]("class" -> "red", "id" -> "msg")
        hook = Hooks.empty
      },
      js.Array(newProxy("span", new DataObject { hook = Hooks.empty }, "Hello")),
    ),
  )

  it should "run effect modifiers once" in {
    val list = new collection.mutable.ArrayBuffer[String]

    val vtree = div(
      IO {
        list += "child1"
        VMod(Observable(div()))
      },
      IO {
        list += "child2"
        VMod(Observable.empty)
      },
      IO {
        list += "child3"
        VMod(nonEmptyObservable(div()))
      },
      IO {
        list += "children1"
        VMod(nonEmptyObservable)
      },
      IO {
        list += "children2"
        VMod(nonEmptyObservable)
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
          },
        ),
      ),
    )

    for {

      node <- IO {
                val node = document.createElement("div")
                document.body.appendChild(node)
                node
              }

      _ <- IO(list.isEmpty shouldBe true)
      _ <- Outwatch.renderInto[IO](node, vtree)
      _ = list should contain theSameElementsAs List(
            "child1",
            "child2",
            "child3",
            "children1",
            "children2",
            "attr1",
            "attr2",
          )

    } yield succeed
  }

  it should "not provide unique key for child nodes" in {
    val mods = Seq(
      div(idAttr := "1"),
      div(idAttr := "2"),
      div(),
      div(),
    )

    val streamable = NativeModifiers.from(mods, RenderConfig.ignoreError)
    val seps       = SeparatedModifiers.from(streamable.modifiers, RenderConfig.ignoreError)
    import seps._

    proxies.get.length shouldBe 4
    streamable.subscribables.toFlatArray.filterNot(_.isEmpty()).length shouldBe 0

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

  it should "not provide unique key for child nodes if stream is present" in {
    val mods = Seq(
      VMod(nonEmptyObservable),
      div(idAttr := "1"),
      div(idAttr := "2"),
      div(),
      div(),
    )

    val streamable = NativeModifiers.from(mods, RenderConfig.ignoreError)
    val seps       = SeparatedModifiers.from(streamable.modifiers, RenderConfig.ignoreError)
    import seps._

    proxies.get.length shouldBe 4
    streamable.subscribables.toFlatArray.filterNot(_.isEmpty()).length shouldBe 1

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
      VMod(nonEmptyObservable),
      div()(Key(5678)),
    )

    val streamable = NativeModifiers.from(mods, RenderConfig.ignoreError)
    val seps       = SeparatedModifiers.from(streamable.modifiers, RenderConfig.ignoreError)
    import seps._

    proxies.get.length shouldBe 1
    streamable.subscribables.toFlatArray.filterNot(_.isEmpty()).length shouldBe 1

    val proxy = SnabbdomOps.toSnabbdom(div(mods), RenderConfig.ignoreError)
    proxy.key.toOption shouldBe Some(1234)

    proxy.children.get(0).key.toOption shouldBe Some(5678)
  }

  "VTrees" should "be constructed correctly" in {

    val attributes = List(BasicAttr("class", "red"), BasicAttr("id", "msg"))
    val message    = "Hello"
    val child      = span(message)
    val vtree      = div(attributes.head, attributes(1), child)

    val proxy = fixture.proxy

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(proxy)
  }

  it should "be correctly created with the HyperscriptHelper" in {
    val attributes = List(BasicAttr("class", "red"), BasicAttr("id", "msg"))
    val message    = "Hello"
    val child      = span(message)
    val vtree      = div(attributes.head, attributes(1), child)

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(fixture.proxy)
  }

  it should "run its effect modifiers once!" in {
    val stringHandler = Subject.replayLatest[String]()

    var ioCounter      = 0
    var handlerCounter = 0
    stringHandler.unsafeForeach { _ =>
      handlerCounter += 1
    }

    val vtree = div(
      div(
        IO {
          ioCounter += 1
          BasicAttr("hans", "")
        },
      ),
      stringHandler,
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    ioCounter shouldBe 0
    handlerCounter shouldBe 0

    for {
      _ <- Outwatch.renderInto[IO](node, vtree)

      _  = ioCounter shouldBe 1
      _  = handlerCounter shouldBe 0
      _ <- stringHandler.onNextIO("pups") *> IO.cede
      _  = ioCounter shouldBe 1
      _  = handlerCounter shouldBe 1
    } yield succeed
  }

  it should "run its effect modifiers once in CompositeModifier!" in {
    val stringHandler = Subject.replayLatest[String]()

    var ioCounter      = 0
    var handlerCounter = 0
    stringHandler.unsafeForeach { _ =>
      handlerCounter += 1
    }

    val vtree = div(
      div(
        Seq(
          IO {
            ioCounter += 1
            BasicAttr("hans", "")
          },
        ),
      ),
      stringHandler,
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    ioCounter shouldBe 0
    handlerCounter shouldBe 0

    for {
      _ <- Outwatch.renderInto[IO](node, vtree)

      _  = ioCounter shouldBe 1
      _  = handlerCounter shouldBe 0
      _ <- stringHandler.onNextIO("pups") *> IO.cede
      _  = ioCounter shouldBe 1
      _  = handlerCounter shouldBe 1

    } yield succeed
  }

  it should "be correctly patched into the DOM" in {

    val id         = "msg"
    val cls        = "red"
    val attributes = List(BasicAttr("class", cls), BasicAttr("id", id))
    val message    = "Hello"
    val child      = span(message)
    val vtree      = div(attributes.head, attributes(1), child)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vtree)

      patchedNode = document.getElementById(id)
      _           = patchedNode.childElementCount shouldBe 1
      _           = patchedNode.classList.contains(cls) shouldBe true
      _           = patchedNode.children(0).innerHTML shouldBe message
    } yield succeed
  }

  it should "be replaced if they contain changeables (SyncIO)" in {

    def page(num: Int): VMod = for {
      pageNum <- SyncIO(Subject.behavior(num))
    } yield div(
      idAttr := "page",
      num match {
        case 1 =>
          div(pageNum)
        case 2 =>
          div(pageNum)
      },
    )

    val pageHandler = Subject.publish[Int]()

    val vtree = div(
      div(pageHandler.map(page)),
    )

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _      <- Outwatch.renderInto[IO](node, vtree)
      _      <- pageHandler.onNextIO(1) *> IO.cede
      domNode = document.getElementById("page")
      _       = domNode.textContent shouldBe "1"
      _      <- pageHandler.onNextIO(2) *> IO.cede
      _       = domNode.textContent shouldBe "2"
    } yield succeed
  }

  it should "be replaced if they contain changeables (IO)" in {

    def page(num: Int): VMod = for {
      pageNum <- IO(Subject.behavior(num))
    } yield div(
      idAttr := "page",
      num match {
        case 1 =>
          div(pageNum)
        case 2 =>
          div(pageNum)
      },
    )

    val pageHandler = Subject.publish[Int]()

    val vtree = div(
      div(pageHandler.map(page)),
    )

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    // import scala.concurrent.duration._

    for {
      _       <- Outwatch.renderInto[IO](node, vtree)
      _       <- pageHandler.onNextIO(1) *> IO.cede
      domNode  = document.getElementById("page")
      _        = domNode.textContent shouldBe "1"
      _       <- pageHandler.onNextIO(2) *> IO.cede
      domNode2 = document.getElementById("page")
      _        = domNode2.textContent shouldBe "2"
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
    val vtree = div(cls := "red", idAttr := "msg", Option(span("Hello")), Option.empty[VMod])

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
    snabbdomNode._id = js.undefined
    snabbdomNode.children.get.head._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(fixture.proxy)
  }

  it should "construct VTrees with boolean attributes" in {

    def boolBuilder(name: String)   = new AttrBuilder.ToBasicAttr[Boolean](name, identity)
    def stringBuilder(name: String) = new AttrBuilder.ToBasicAttr[Boolean](name, _.toString)

    val vtree = div(
      boolBuilder("a"),
      boolBuilder("b") := true,
      boolBuilder("c") := false,
      stringBuilder("d"),
      stringBuilder("e") := true,
      stringBuilder("f") := false,
    )

    val attributes =
      js.Dictionary[Attr.Value]("a" -> true, "b" -> true, "c" -> false, "d" -> "true", "e" -> "true", "f" -> "false")
    val expected = newProxy("div", new DataObject { attrs = attributes; hook = Hooks.empty })

    val snabbdomNode = SnabbdomOps.toSnabbdom(vtree, RenderConfig.ignoreError)
    snabbdomNode._id = js.undefined
    JSON.stringify(snabbdomNode) shouldBe JSON.stringify(expected)
  }

  it should "patch into the DOM properly" in {

    val message = "Test"
    val vtree   = div(cls := "blue", idAttr := "test", span(message), ul(idAttr := "list", li("1"), li("2"), li("3")))

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    Outwatch.renderInto[IO](node, vtree).map { _ =>
      val patchedNode = document.getElementById("test")

      patchedNode.childElementCount shouldBe 2
      patchedNode.classList.contains("blue") shouldBe true
      patchedNode.children(0).innerHTML shouldBe message
      document.getElementById("list").childElementCount shouldBe 3
    }
  }

  it should "change the value of a textfield" in {

    val messages = Subject.publish[String]()
    val vtree = div(
      input(attributes.value <-- messages, idAttr := "input"),
    )

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vtree)

      field = document.getElementById("input").asInstanceOf[html.Input]

      _ = field.value shouldBe ""

      message = "Hello"
      _      <- messages.onNextIO(message) *> IO.cede

      _ = field.value shouldBe message

      message2 = "World"
      _       <- messages.onNextIO(message2) *> IO.cede

      _ = field.value shouldBe message2
    } yield succeed
  }

  it should "render child nodes in correct order" in {

    val messagesA = Subject.publish[String]()
    val messagesB = Subject.publish[String]()

    val vNode = div(
      span("A"),
      messagesA.map(span(_)),
      span("B"),
      messagesB.map(span(_)),
    )

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ <- messagesA.onNextIO("1") *> IO.cede
      _ <- messagesB.onNextIO("2") *> IO.cede

      _ = node.innerHTML shouldBe "<div><span>A</span><span>1</span><span>B</span><span>2</span></div>"
    } yield succeed
  }

  it should "render child string-nodes in correct order" in {

    val messagesA = Subject.publish[String]()
    val messagesB = Subject.publish[String]()
    val vNode = div(
      "A",
      messagesA,
      "B",
      messagesB,
    )

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ = node.innerHTML shouldBe "<div>AB</div>"

      _ <- messagesA.onNextIO("1") *> IO.cede
      _  = node.innerHTML shouldBe "<div>A1B</div>"

      _ <- messagesB.onNextIO("2") *> IO.cede
      _  = node.innerHTML shouldBe "<div>A1B2</div>"
    } yield succeed
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
      messagesB,
    )

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ = node.innerHTML shouldBe "<div>AB</div>"

      _ <- messagesA.onNextIO("1") *> IO.cede
      _  = node.innerHTML shouldBe "<div>A1B</div>"

      _ <- messagesB.onNextIO("2") *> IO.cede
      _  = node.innerHTML shouldBe "<div>A1B2</div>"

      _ <- messagesC.onNextIO(Seq(div("5"), div("7"))) *> IO.cede
      _  = node.innerHTML shouldBe "<div>A1<div>5</div><div>7</div>B2</div>"
    } yield succeed
  }

  it should "update merged nodes children correctly" in {

    val messages      = Subject.publish[Seq[VNode]]()
    val otherMessages = Subject.publish[Seq[VNode]]()
    val vNode         = div(messages)(otherMessages)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ <- otherMessages.onNextIO(Seq(div("otherMessage"))) *> IO.cede
      _  = node.children(0).innerHTML shouldBe "<div>otherMessage</div>"

      _ <- messages.onNextIO(Seq(div("message"))) *> IO.cede
      _  = node.children(0).innerHTML shouldBe "<div>message</div><div>otherMessage</div>"

      _ <- otherMessages.onNextIO(Seq(div("genus"))) *> IO.cede
      _  = node.children(0).innerHTML shouldBe "<div>message</div><div>genus</div>"
    } yield succeed
  }

  it should "update merged nodes separate children correctly" in {

    val messages      = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNode         = div(messages)(otherMessages)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ = node.children(0).innerHTML shouldBe ""

      _ <- otherMessages.onNextIO("otherMessage") *> IO.cede
      _  = node.children(0).innerHTML shouldBe "otherMessage"

      _ <- messages.onNextIO("message") *> IO.cede
      _  = node.children(0).innerHTML shouldBe "messageotherMessage"

      _ <- otherMessages.onNextIO("genus") *> IO.cede
      _  = node.children(0).innerHTML shouldBe "messagegenus"
    } yield succeed

  }

  it should "partially render component even if parts not present" in {

    val messagesColor   = Subject.publish[String]()
    val messagesBgColor = Subject.publish[String]()
    val childString     = Subject.publish[String]()

    val vNode = div(idAttr := "inner", color <-- messagesColor, backgroundColor <-- messagesBgColor, childString)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      inner = document.getElementById("inner").asInstanceOf[html.Div]

      _ = inner.innerHTML shouldBe ""
      _ = inner.style.color shouldBe ""
      _ = inner.style.backgroundColor shouldBe ""

      _ <- childString.onNextIO("fish") *> IO.cede
      _  = inner.innerHTML shouldBe "fish"
      _  = inner.style.color shouldBe ""
      _  = inner.style.backgroundColor shouldBe ""

      _ <- messagesColor.onNextIO("red") *> IO.cede
      _  = inner.innerHTML shouldBe "fish"
      _  = inner.style.color shouldBe "red"
      _  = inner.style.backgroundColor shouldBe ""

      _ <- messagesBgColor.onNextIO("blue") *> IO.cede
      _  = inner.innerHTML shouldBe "fish"
      _  = inner.style.color shouldBe "red"
      _  = inner.style.backgroundColor shouldBe "blue"

    } yield succeed

  }

  it should "partially render component even if parts not present2" in {

    val messagesColor = Subject.publish[String]()
    val childString   = Subject.publish[String]()

    val vNode = div(idAttr := "inner", color <-- messagesColor, childString)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      inner = document.getElementById("inner").asInstanceOf[html.Div]

      _ = inner.innerHTML shouldBe ""
      _ = inner.style.color shouldBe ""

      _ <- childString.onNextIO("fish") *> IO.cede
      _  = inner.innerHTML shouldBe "fish"
      _  = inner.style.color shouldBe ""

      _ <- messagesColor.onNextIO("red") *> IO.cede
      _  = inner.innerHTML shouldBe "fish"
      _  = inner.style.color shouldBe "red"
    } yield succeed

  }

  it should "update reused vnodes correctly" in {

    val messages  = Subject.publish[String]()
    val vNode     = div(data.ralf := true, messages)
    val container = div(vNode, vNode)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, container)

      _ <- messages.onNextIO("message") *> IO.cede
      _  = node.children(0).children(0).innerHTML shouldBe "message"
      _  = node.children(0).children(1).innerHTML shouldBe "message"

      _ <- messages.onNextIO("bumo") *> IO.cede
      _  = node.children(0).children(0).innerHTML shouldBe "bumo"
      _  = node.children(0).children(1).innerHTML shouldBe "bumo"
    } yield succeed

  }

  it should "update merged nodes correctly (render reuse)" in {

    val messages      = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNodeTemplate = div(messages)
    val vNode         = vNodeTemplate(otherMessages)

    val node = IO {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      node1 <- node
      _     <- Outwatch.renderInto[IO](node1, vNodeTemplate)

      node2 <- node
      _     <- Outwatch.renderInto[IO](node2, vNode)

      _ <- messages.onNextIO("gurkon") *> IO.cede
      _ <- otherMessages.onNextIO("otherMessage") *> IO.cede
      _  = node1.children(0).innerHTML shouldBe "gurkon"
      _  = node2.children(0).innerHTML shouldBe "gurkonotherMessage"

      _ <- messages.onNextIO("message") *> IO.cede
      _  = node1.children(0).innerHTML shouldBe "message"
      _  = node2.children(0).innerHTML shouldBe "messageotherMessage"

      _ <- otherMessages.onNextIO("genus") *> IO.cede
      _  = node1.children(0).innerHTML shouldBe "message"
      _  = node2.children(0).innerHTML shouldBe "messagegenus"
    } yield succeed
  }

  it should "update merged node attributes correctly" in {

    val messages      = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNode         = div(data.noise <-- messages)(data.noise <-- otherMessages)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ <- otherMessages.onNextIO("otherMessage") *> IO.cede
      _  = node.children(0).getAttribute("data-noise") shouldBe "otherMessage"

      _ <- messages.onNextIO("message") // should be ignored *> IO.cede
      _  = node.children(0).getAttribute("data-noise") shouldBe "otherMessage"

      _ <- otherMessages.onNextIO("genus") *> IO.cede
      _  = node.children(0).getAttribute("data-noise") shouldBe "genus"
    } yield succeed
  }

  it should "update merged node styles written with style() correctly" in {

    val messages      = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNode         = div(VMod.style("color") <-- messages)(VMod.style("color") <-- otherMessages)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ <- otherMessages.onNextIO("red") *> IO.cede
      _  = node.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

      _ <- messages.onNextIO("blue") // should be ignored *> IO.cede
      _  = node.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

      _ <- otherMessages.onNextIO("green") *> IO.cede
      _  = node.children(0).asInstanceOf[html.Element].style.color shouldBe "green"
    } yield succeed

  }

  it should "update merged node styles correctly" in {

    val messages      = Subject.publish[String]()
    val otherMessages = Subject.publish[String]()
    val vNode         = div(color <-- messages)(color <-- otherMessages)

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ <- otherMessages.onNextIO("red") *> IO.cede
      _  = node.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

      _ <- messages.onNextIO("blue") // should be ignored *> IO.cede
      _  = node.children(0).asInstanceOf[html.Element].style.color shouldBe "red"

      _ <- otherMessages.onNextIO("green") *> IO.cede
      _  = node.children(0).asInstanceOf[html.Element].style.color shouldBe "green"
    } yield succeed

  }

  it should "render composite VNodes properly" in {

    val items = Seq("one", "two", "three")
    val vNode = div(items.map(item => span(item)))

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ = node.innerHTML shouldBe "<div><span>one</span><span>two</span><span>three</span></div>"
    } yield succeed
  }

  it should "render nodes with only attribute receivers properly" in {

    val classes = Subject.publish[String]()
    val vNode   = button(className <-- classes, "Submit")

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ <- classes.onNextIO("active") *> IO.cede

      _ = node.innerHTML shouldBe """<button class="active">Submit</button>"""
    } yield succeed
  }

  it should "work with custom tags" in {

    val vNode = div(VNode.html("main")())

    val node = {
      val node = document.createElement("div")
      document.body.appendChild(node)
      node
    }

    for {
      _ <- Outwatch.renderInto[IO](node, vNode)

      _ = node.innerHTML shouldBe "<div><main></main></div>"
    } yield succeed
  }

  it should "work with un-assigned booleans attributes and props" in {

    val vNode = option(selected, disabled)

    val node = {
      val node = document.createElement("option").asInstanceOf[html.Option]
      document.body.appendChild(node)
      node
    }

    node.selected shouldBe false
    node.disabled shouldBe false

    Outwatch.renderReplace[IO](node, vNode).map { _ =>
      node.selected shouldBe true
      node.disabled shouldBe true
    }
  }

  it should "correctly work with Render conversions" in {

    for {
      n1 <- IO(document.createElement("div"))

      _ <- Outwatch.renderReplace[IO](n1, div("one"))
      _  = n1.innerHTML shouldBe "one"

      _ <- Outwatch.renderReplace[IO](n1, div(Some("one")))
      _  = n1.innerHTML shouldBe "one"

      n2 <- IO(document.createElement("div"))

      _ <- Outwatch.renderReplace[IO](n2, div(None: Option[Int]))
      _  = n2.innerHTML shouldBe ""

      _ <- Outwatch.renderReplace[IO](n1, div(1))
      _  = n1.innerHTML shouldBe "1"

      _ <- Outwatch.renderReplace[IO](n1, div(1.0))
      _  = n1.innerHTML shouldBe "1"

      _ <- Outwatch.renderReplace[IO](n1, div(Seq("one", "two")))
      _  = n1.innerHTML shouldBe "onetwo"

      _ <- Outwatch.renderReplace[IO](n1, div(Seq(1, 2)))
      _  = n1.innerHTML shouldBe "12"

      _ <- Outwatch.renderReplace[IO](n1, div(Seq(1.0, 2.0)))
      _  = n1.innerHTML shouldBe "12"

    } yield succeed
  }

  "Children stream" should "work for string sequences" in {

    val myStrings: Observable[Seq[String]] = Observable(Seq("a", "b"))
    val node                               = div(idAttr := "strings", myStrings)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "ab"
    } yield succeed
  }

  it should "work for string sequences non empty" in {

    val myStrings: Observable[Seq[String]] = nonEmptyObservable(Seq("a", "b"))
    val node                               = div(idAttr := "strings", myStrings)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "ab"
    } yield succeed
  }

  it should "work for double/boolean/long/int" in {

    val node = div(idAttr := "strings", 1.1, true, 133L, 7)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "1.1true1337"
    } yield succeed
  }

  it should "work for string options" in {

    IO(Subject.behavior(Option("a"))).flatMap { myOption =>
      val node = div(idAttr := "strings", myOption)

      for {
        _ <- Outwatch.renderInto[IO]("#app", node)

        element = document.getElementById("strings")
        _       = element.innerHTML shouldBe "a"

        _ <- myOption.onNextIO(None) *> IO.cede
        _  = element.innerHTML shouldBe ""
      } yield succeed
    }
  }

  it should "work for vnode options" in {

    IO(Subject.behavior(Option(div("a")))).flatMap { myOption =>
      val node = div(idAttr := "strings", myOption)

      for {
        _ <- Outwatch.renderInto[IO]("#app", node)

        element = document.getElementById("strings")
        _       = element.innerHTML shouldBe "<div>a</div>"

        _ <- myOption.onNextIO(None) *> IO.cede
        _  = element.innerHTML shouldBe ""

      } yield succeed
    }
  }

  "Modifier stream" should "work for modifier" in {

    val myHandler = Subject.behavior[VMod](Seq(cls := "hans", b("stark")))
    val node      = div(idAttr := "strings", div(VMod(myHandler)))

    for {
      _      <- Outwatch.renderInto[IO]("#app", node)
      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe """<div class="hans"><b>stark</b></div>"""
      _      <- myHandler.onNextIO(Option(idAttr := "fair")) *> IO.cede
      _       = element.innerHTML shouldBe """<div id="fair"></div>"""
    } yield succeed
  }

  it should "work for multiple mods" in {

    val myHandler = Subject.replayLatest[VMod]()
    val node      = div(idAttr := "strings", div(myHandler, "bla"))

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div>bla</div>"

      _ <- myHandler.onNextIO(cls := "hans") *> IO.cede
      _  = element.innerHTML shouldBe """<div class="hans">bla</div>"""

      innerHandler = Subject.replayLatest[VMod]()

      _ <- myHandler.onNextIO(
             div(
               innerHandler,
               cls := "no?",
               "yes?",
             ),
           ) *> IO.cede
      _ = element.innerHTML shouldBe """<div><div class="no?">yes?</div>bla</div>"""

      _ <- innerHandler.onNextIO(Seq(span("question:"), idAttr := "heidi")) *> IO.cede
      _  = element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question:</span>yes?</div>bla</div>"""

      _ <- myHandler.onNextIO(
             div(
               innerHandler,
               cls := "no?",
               "yes?",
               b("go!"),
             ),
           ) *> IO.cede
      _ =
        element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question:</span>yes?<b>go!</b></div>bla</div>"""

      _ <- innerHandler.onNextIO(Seq(span("question and answer:"), idAttr := "heidi")) *> IO.cede
      _ =
        element.innerHTML shouldBe """<div><div class="no?" id="heidi"><span>question and answer:</span>yes?<b>go!</b></div>bla</div>"""

      _ <- myHandler.onNextIO(Seq(span("nope"))) *> IO.cede
      _  = element.innerHTML shouldBe """<div><span>nope</span>bla</div>"""

      _ <- innerHandler.onNextIO(b("me?")) *> IO.cede
      _  = element.innerHTML shouldBe """<div><span>nope</span>bla</div>"""
    } yield succeed
  }

  it should "work for nested stream modifier" in {

    val myHandler = Subject.replayLatest[VMod]()
    val node      = div(idAttr := "strings", div(myHandler))

    for {
      _      <- Outwatch.renderInto[IO]("#app", node)
      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div></div>"

      innerHandler = Subject.replayLatest[VMod]()

      _ <- myHandler.onNextIO(innerHandler) *> IO.cede
      _  = element.innerHTML shouldBe """<div></div>"""

      _ <- innerHandler.onNextIO(VMod(cls := "hans", "1")) *> IO.cede
      _  = element.innerHTML shouldBe """<div class="hans">1</div>"""

      innerHandler2 = Subject.replayLatest[VMod]()

      _ <- myHandler.onNextIO(innerHandler2) *> IO.cede
      _  = element.innerHTML shouldBe """<div></div>"""

      _ <- myHandler.onNextIO(CompositeModifier(VMod(innerHandler2) :: Nil)) *> IO.cede
      _  = element.innerHTML shouldBe """<div></div>"""

      _ <- myHandler.onNextIO(CompositeModifier(VMod(innerHandler2) :: Nil)) *> IO.cede
      _  = element.innerHTML shouldBe """<div></div>"""

      _ <- myHandler.onNextIO(CompositeModifier(StringVNode("pete") :: VMod(innerHandler2) :: Nil)) *> IO.cede
      _  = element.innerHTML shouldBe """<div>pete</div>"""

      _ <- innerHandler2.onNextIO(VMod(idAttr := "dieter", "r")) *> IO.cede
      _  = element.innerHTML shouldBe """<div id="dieter">peter</div>"""

      _ <- innerHandler.onNextIO(b("me?")) *> IO.cede
      _  = element.innerHTML shouldBe """<div id="dieter">peter</div>"""

      _ <- myHandler.onNextIO(span("the end")) *> IO.cede
      _  = element.innerHTML shouldBe """<div><span>the end</span></div>"""
    } yield succeed
  }

  it should "work for nested stream modifier and default value" in {

    var numPatches = 0

    val myHandler = Subject.replayLatest[VMod]()
    val node: VNode = div(
      idAttr := "strings",
      div(
        onSnabbdomPrePatch doAction { numPatches += 1 },
        myHandler.prepend(VMod("initial")),
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div>initial</div>"
      _       = numPatches shouldBe 0

      innerHandler = Subject.replayLatest[VMod]()

      _ <- myHandler.onNextIO(innerHandler.prepend(BasicAttr("initial", "2"))) *> IO.cede
      _  = element.innerHTML shouldBe """<div initial="2"></div>"""
      _  = numPatches shouldBe 1

      _ <- innerHandler.onNextIO(BasicAttr("attr", "3")) *> IO.cede
      _  = element.innerHTML shouldBe """<div attr="3"></div>"""
      _  = numPatches shouldBe 2

      innerHandler2 = Subject.replayLatest[VMod]()
      _            <- myHandler.onNextIO(innerHandler2.prepend(VMod("initial3"))) *> IO.cede
      _             = element.innerHTML shouldBe """<div>initial3</div>"""
      _             = numPatches shouldBe 3

      _ <-
        myHandler.onNextIO(CompositeModifier(VMod(innerHandler2.prepend(VMod("initial4"))) :: Nil)) *> IO.cede
      _ = element.innerHTML shouldBe """<div>initial4</div>"""
      _ = numPatches shouldBe 4

      _ <- myHandler.onNextIO(CompositeModifier(StringVNode("pete") :: VMod(innerHandler2) :: Nil)) *> IO.cede
      _  = element.innerHTML shouldBe """<div>pete</div>"""
      _  = numPatches shouldBe 5

      _ <- innerHandler2.onNextIO(VMod(idAttr := "dieter", "r")) *> IO.cede
      _  = element.innerHTML shouldBe """<div id="dieter">peter</div>"""
      _  = numPatches shouldBe 6

      _ <- innerHandler.onNextIO("me?") *> IO.cede
      _  = element.innerHTML shouldBe """<div id="dieter">peter</div>"""
      _  = numPatches shouldBe 6

      _ <- myHandler.onNextIO(span("the end")) *> IO.cede
      _  = element.innerHTML shouldBe """<div><span>the end</span></div>"""
      _  = numPatches shouldBe 7
    } yield succeed
  }

  it should "work for deeply nested handlers" in {

    val a = Subject.behavior(0)
    val b = a.map(_.toString)
    val node =
      div(
        a.map(_ =>
          div(
            b.map(b => div(b)),
          ),
        ),
      )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("app")
      _       = element.innerHTML shouldBe "<div><div><div>0</div></div></div>"

      _ <- a.onNextIO(1) *> IO.cede
      _  = element.innerHTML shouldBe "<div><div><div>1</div></div></div>"
    } yield succeed
  }

  it should "work for nested stream modifier and empty default and start with" in {

    var numPatches = 0

    val myHandler = Subject.behavior[VMod]("initial")
    val node = div(
      idAttr := "strings",
      div(
        onSnabbdomPrePatch doAction { numPatches += 1 },
        myHandler,
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div>initial</div>"
      _       = numPatches shouldBe 0

      innerHandler = Subject.replayLatest[VMod]()
      _           <- myHandler.onNextIO(innerHandler.startWith(BasicAttr("initial", "2") :: Nil)) *> IO.cede
      _            = element.innerHTML shouldBe """<div initial="2"></div>"""
      _            = numPatches shouldBe 1

      _ <- innerHandler.onNextIO(BasicAttr("attr", "3")) *> IO.cede
      _  = element.innerHTML shouldBe """<div attr="3"></div>"""
      _  = numPatches shouldBe 2

      innerHandler2 = Subject.replayLatest[VMod]()
      _            <- myHandler.onNextIO(innerHandler2.startWith(VMod("initial3") :: Nil)) *> IO.cede
      _             = element.innerHTML shouldBe """<div>initial3</div>"""
      _             = numPatches shouldBe 3

      _ <-
        myHandler.onNextIO(CompositeModifier(VMod(innerHandler2.prepend(VMod("initial4"))) :: Nil)) *> IO.cede
      _ = element.innerHTML shouldBe """<div>initial4</div>"""
      _ = numPatches shouldBe 4

      _ <- myHandler.onNextIO(CompositeModifier(StringVNode("pete") :: VMod(innerHandler2) :: Nil)) *> IO.cede
      _  = element.innerHTML shouldBe """<div>pete</div>"""
      _  = numPatches shouldBe 5

      _ <- innerHandler2.onNextIO(VMod(idAttr := "dieter", "r")) *> IO.cede
      _  = element.innerHTML shouldBe """<div id="dieter">peter</div>"""
      _  = numPatches shouldBe 6

      _ <- innerHandler.onNextIO("me?") *> IO.cede
      _  = element.innerHTML shouldBe """<div id="dieter">peter</div>"""
      _  = numPatches shouldBe 6

      _ <- myHandler.onNextIO(span("the end")) *> IO.cede
      _  = element.innerHTML shouldBe """<div><span>the end</span></div>"""
      _  = numPatches shouldBe 7
    } yield succeed
  }

  it should "work for stream modifier and streaming default value (subscriptions are canceled properly)" in {

    val myHandler    = Subject.replayLatest[VMod]()
    val innerHandler = Subject.replayLatest[VMod]()

    val outerTriggers = new scala.collection.mutable.ArrayBuffer[VMod]
    val innerTriggers = new scala.collection.mutable.ArrayBuffer[VMod]

    val node = div(
      idAttr := "strings",
      div(
        myHandler.map { x => outerTriggers += x; x }
          .prepend(VMod(innerHandler.map { x => innerTriggers += x; x }.prepend(VMod("initial")))),
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div>initial</div>"
      _       = outerTriggers.size shouldBe 0
      _       = innerTriggers.size shouldBe 0

      _ <- innerHandler.onNextIO(VMod("hi!")) *> IO.cede
      _  = element.innerHTML shouldBe """<div>hi!</div>"""
      _  = outerTriggers.size shouldBe 0
      _  = innerTriggers.size shouldBe 1

      _ <- myHandler.onNextIO(VMod("test")) *> IO.cede
      _  = element.innerHTML shouldBe """<div>test</div>"""
      _  = outerTriggers.size shouldBe 1
      _  = innerTriggers.size shouldBe 1

      _ <- myHandler.onNextIO(nonEmptyObservable(BasicAttr("initial", "2"))) *> IO.cede
      _  = element.innerHTML shouldBe """<div initial="2"></div>"""
      _  = outerTriggers.size shouldBe 2
      _  = innerTriggers.size shouldBe 1

      _ <- innerHandler.onNextIO(VMod("me?")) *> IO.cede
      _  = element.innerHTML shouldBe """<div initial="2"></div>"""
      _  = outerTriggers.size shouldBe 2
      _  = innerTriggers.size shouldBe 1

      _ <- myHandler.onNextIO(innerHandler.map { x => innerTriggers += x; x }.prepend(VMod.empty)) *> IO.cede
      _  = element.innerHTML shouldBe """<div>me?</div>"""
      _  = outerTriggers.size shouldBe 3
      _  = innerTriggers.size shouldBe 2

      _ <- innerHandler.onNextIO(BasicAttr("attr", "3")) *> IO.cede
      _  = element.innerHTML shouldBe """<div attr="3"></div>"""
      _  = outerTriggers.size shouldBe 3
      _  = innerTriggers.size shouldBe 3

      innerTriggers2 = new scala.collection.mutable.ArrayBuffer[VMod]
      innerTriggers3 = new scala.collection.mutable.ArrayBuffer[VMod]

      innerHandler2 = Subject.replayLatest[VMod]()
      innerHandler3 = Subject.replayLatest[VMod]()

      _ <- innerHandler.onNextIO(innerHandler2.map { x => innerTriggers2 += x; x }.prepend(VMod(innerHandler3.map { x =>
             innerTriggers3 += x; x
           }))) *> IO.cede
      _ = element.innerHTML shouldBe """<div></div>"""
      _ = outerTriggers.size shouldBe 3
      _ = innerTriggers.size shouldBe 4
      _ = innerTriggers2.size shouldBe 0
      _ = innerTriggers3.size shouldBe 0

      _ <- innerHandler2.onNextIO(VMod("2")) *> IO.cede
      _  = element.innerHTML shouldBe """<div>2</div>"""
      _  = outerTriggers.size shouldBe 3
      _  = innerTriggers.size shouldBe 4
      _  = innerTriggers2.size shouldBe 1
      _  = innerTriggers3.size shouldBe 0

      _ <- innerHandler.onNextIO(EmptyModifier) *> IO.cede
      _  = element.innerHTML shouldBe """<div></div>"""
      _  = outerTriggers.size shouldBe 3
      _  = innerTriggers.size shouldBe 5
      _  = innerTriggers2.size shouldBe 1
      _  = innerTriggers3.size shouldBe 0

      _ <- innerHandler2.onNextIO(VMod("me?")) *> IO.cede
      _  = element.innerHTML shouldBe """<div></div>"""
      _  = outerTriggers.size shouldBe 3
      _  = innerTriggers.size shouldBe 5
      _  = innerTriggers2.size shouldBe 1
      _  = innerTriggers3.size shouldBe 0

      _ <- innerHandler3.onNextIO(VMod("me?")) *> IO.cede
      _  = element.innerHTML shouldBe """<div></div>"""
      _  = outerTriggers.size shouldBe 3
      _  = innerTriggers.size shouldBe 5
      _  = innerTriggers2.size shouldBe 1
      _  = innerTriggers3.size shouldBe 0

      _ <- myHandler.onNextIO(VMod("go away")) *> IO.cede
      _  = element.innerHTML shouldBe """<div>go away</div>"""
      _  = outerTriggers.size shouldBe 4
      _  = innerTriggers.size shouldBe 5
      _  = innerTriggers2.size shouldBe 1
      _  = innerTriggers3.size shouldBe 0

      _ <- innerHandler.onNextIO(VMod("me?")) *> IO.cede
      _  = element.innerHTML shouldBe """<div>go away</div>"""
      _  = outerTriggers.size shouldBe 4
      _  = innerTriggers.size shouldBe 5
      _  = innerTriggers2.size shouldBe 1
      _  = innerTriggers3.size shouldBe 0

    } yield succeed
  }

  it should "be able to render basic handler" in {
    val counter: VMod = button(
      idAttr := "click",
      IO(Subject.behavior(0)).map { handler =>
        VMod(onClick(handler.map(_ + 1)) --> handler, handler)
      },
    )

    val vtree = div(counter)

    for {
      _      <- Outwatch.renderInto[IO]("#app", vtree)
      element = document.getElementById("click")

      _ = element.innerHTML shouldBe "0"

      _ <- IO(sendEvent(element, "click")) *> IO.cede
      _  = element.innerHTML shouldBe "1"

      _ <- IO(sendEvent(element, "click")) *> IO.cede
      _  = element.innerHTML shouldBe "2"
    } yield succeed
  }

  it should "be able to render basic handler with scan" in {
    val counter: VMod = button(
      idAttr := "click",
      IO(Subject.replayLatest[Int]()).map { handler =>
        VMod(onClick.asScan0(0)(_ + 1) --> handler, handler)
      },
    )

    val vtree = div(counter)

    for {
      _      <- Outwatch.renderInto[IO]("#app", vtree)
      element = document.getElementById("click")

      _ = element.innerHTML shouldBe "0"

      _ <- IO(sendEvent(element, "click")) *> IO.cede
      _  = element.innerHTML shouldBe "1"

      _ <- IO(sendEvent(element, "click")) *> IO.cede
      _  = element.innerHTML shouldBe "2"
    } yield succeed
  }

  it should "to render basic handler with scan directly from EmitterBuilder" in {
    val counter: VMod = button(
      idAttr := "click",
      onClick.asScan0(0)(_ + 1).handled(VMod(_)),
    )

    val vtree = div(counter)

    for {
      _      <- Outwatch.renderInto[IO]("#app", vtree)
      element = document.getElementById("click")

      _ = element.innerHTML shouldBe "0"

      _ <- IO(sendEvent(element, "click")) *> IO.cede
      _  = element.innerHTML shouldBe "1"

      _ <- IO(sendEvent(element, "click")) *> IO.cede
      _  = element.innerHTML shouldBe "2"
    } yield succeed
  }

  it should "work for not overpatching keep proxy from previous patch" in {
    val handler  = Subject.replayLatest[Int]()
    val handler2 = Subject.replayLatest[Int]()

    var inserted        = 0
    var destroyed       = 0
    var patched         = 0
    var inserted2       = 0
    var uninserted2     = 0
    var patched2        = 0
    var insertedHeinz   = 0
    var uninsertedHeinz = 0
    var patchedHeinz    = 0
    var insertedKlara   = 0
    var uninsertedKlara = 0
    var patchedKlara    = 0

    val node = div(
      idAttr := "strings",
      handler.map(i =>
        if (i == 0) VMod.empty
        else
          div(
            i,
            onSnabbdomPostPatch.doAction { patched += 1 },
            onSnabbdomInsert.doAction { inserted += 1 },
            onSnabbdomDestroy.doAction { destroyed += 1 },
          ),
      ),
      div(
        "heinz",
        onSnabbdomPostPatch.doAction { patchedHeinz += 1 },
        onSnabbdomInsert.doAction { insertedHeinz += 1 },
        onSnabbdomDestroy.doAction { uninsertedHeinz += 1 },
      ),
      handler2.map(i =>
        if (i == 0) VMod.empty
        else
          div(
            i,
            onSnabbdomPostPatch.doAction { patched2 += 1 },
            onSnabbdomInsert.doAction { inserted2 += 1 },
            onSnabbdomDestroy.doAction { uninserted2 += 1 },
          ),
      ),
      div(
        "klara",
        onSnabbdomPostPatch.doAction { patchedKlara += 1 },
        onSnabbdomInsert.doAction { insertedKlara += 1 },
        onSnabbdomDestroy.doAction { uninsertedKlara += 1 },
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = element.innerHTML shouldBe """<div>heinz</div><div>klara</div>"""
      _ = inserted shouldBe 0
      _ = destroyed shouldBe 0
      _ = patched shouldBe 0
      _ = inserted2 shouldBe 0
      _ = uninserted2 shouldBe 0
      _ = patched2 shouldBe 0
      _ = insertedHeinz shouldBe 1
      _ = uninsertedHeinz shouldBe 0
      _ = patchedHeinz shouldBe 0
      _ = insertedKlara shouldBe 1
      _ = uninsertedKlara shouldBe 0
      _ = patchedKlara shouldBe 0

      _ <- handler.onNextIO(1) *> IO.cede
      _  = element.innerHTML shouldBe """<div>1</div><div>heinz</div><div>klara</div>"""
      _  = inserted shouldBe 1
      _  = destroyed shouldBe 0
      _  = patched shouldBe 0
      _  = inserted2 shouldBe 0
      _  = uninserted2 shouldBe 0
      _  = patched2 shouldBe 0
      _  = insertedHeinz shouldBe 1
      _  = uninsertedHeinz shouldBe 0
      _  = patchedHeinz shouldBe 0
      _  = insertedKlara shouldBe 1
      _  = uninsertedKlara shouldBe 0
      _  = patchedKlara shouldBe 0

      _ <- handler2.onNextIO(99) *> IO.cede
      _  = element.innerHTML shouldBe """<div>1</div><div>heinz</div><div>99</div><div>klara</div>"""
      _  = inserted shouldBe 1
      _  = destroyed shouldBe 0
      _  = patched shouldBe 0
      _  = inserted2 shouldBe 1
      _  = uninserted2 shouldBe 0
      _  = patched2 shouldBe 0
      _  = insertedHeinz shouldBe 1
      _  = uninsertedHeinz shouldBe 0
      _  = patchedHeinz shouldBe 0
      _  = insertedKlara shouldBe 1
      _  = uninsertedKlara shouldBe 0
      _  = patchedKlara shouldBe 0

      _ <- handler2.onNextIO(0) *> IO.cede
      _  = element.innerHTML shouldBe """<div>1</div><div>heinz</div><div>klara</div>"""
      _  = inserted shouldBe 1
      _  = destroyed shouldBe 0
      _  = patched shouldBe 0
      _  = inserted2 shouldBe 1
      _  = uninserted2 shouldBe 1
      _  = patched2 shouldBe 0
      _  = insertedHeinz shouldBe 1
      _  = uninsertedHeinz shouldBe 0
      _  = patchedHeinz shouldBe 0
      _  = insertedKlara shouldBe 1
      _  = uninsertedKlara shouldBe 0
      _  = patchedKlara shouldBe 0

      _ <- handler.onNextIO(0) *> IO.cede
      _  = element.innerHTML shouldBe """<div>heinz</div><div>klara</div>"""
      _  = inserted shouldBe 1
      _  = destroyed shouldBe 1
      _  = patched shouldBe 0
      _  = inserted2 shouldBe 1
      _  = uninserted2 shouldBe 1
      _  = patched2 shouldBe 0
      _  = insertedHeinz shouldBe 1
      _  = uninsertedHeinz shouldBe 0
      _  = patchedHeinz shouldBe 0
      _  = insertedKlara shouldBe 1
      _  = uninsertedKlara shouldBe 0
      _  = patchedKlara shouldBe 0

      _ <- handler2.onNextIO(3) *> IO.cede
      _  = element.innerHTML shouldBe """<div>heinz</div><div>3</div><div>klara</div>"""
      _  = inserted shouldBe 1
      _  = destroyed shouldBe 1
      _  = patched shouldBe 0
      _  = inserted2 shouldBe 2
      _  = uninserted2 shouldBe 1
      _  = patched2 shouldBe 0
      _  = insertedHeinz shouldBe 1
      _  = uninsertedHeinz shouldBe 0
      _  = patchedHeinz shouldBe 0
      _  = insertedKlara shouldBe 1
      _  = uninsertedKlara shouldBe 0
      _  = patchedKlara shouldBe 0

      _ <- handler.onNextIO(2) *> IO.cede
      _  = element.innerHTML shouldBe """<div>2</div><div>heinz</div><div>3</div><div>klara</div>"""
      _  = inserted shouldBe 2
      _  = destroyed shouldBe 1
      _  = patched shouldBe 0
      _  = inserted2 shouldBe 2
      _  = uninserted2 shouldBe 1
      _  = patched2 shouldBe 0
      _  = insertedHeinz shouldBe 1
      _  = uninsertedHeinz shouldBe 0
      _  = patchedHeinz shouldBe 0
      _  = insertedKlara shouldBe 1
      _  = uninsertedKlara shouldBe 0
      _  = patchedKlara shouldBe 0

      _ <- handler.onNextIO(18) *> IO.cede
      _  = element.innerHTML shouldBe """<div>18</div><div>heinz</div><div>3</div><div>klara</div>"""
      _  = inserted shouldBe 2
      _  = destroyed shouldBe 1
      _  = patched shouldBe 1
      _  = inserted2 shouldBe 2
      _  = uninserted2 shouldBe 1
      _  = patched2 shouldBe 0
      _  = insertedHeinz shouldBe 1
      _  = uninsertedHeinz shouldBe 0
      _  = patchedHeinz shouldBe 0
      _  = insertedKlara shouldBe 1
      _  = uninsertedKlara shouldBe 0
      _  = patchedKlara shouldBe 0

      _ <- handler2.onNextIO(19) *> IO.cede
      _  = element.innerHTML shouldBe """<div>18</div><div>heinz</div><div>19</div><div>klara</div>"""
      _  = inserted shouldBe 2
      _  = destroyed shouldBe 1
      _  = patched shouldBe 1
      _  = inserted2 shouldBe 2
      _  = uninserted2 shouldBe 1
      _  = patched2 shouldBe 1
      _  = insertedHeinz shouldBe 1
      _  = uninsertedHeinz shouldBe 0
      _  = patchedHeinz shouldBe 0
      _  = insertedKlara shouldBe 1
      _  = uninsertedKlara shouldBe 0
      _  = patchedKlara shouldBe 0
    } yield succeed
  }

  it should "work for nested observables with seq modifiers " in {

    val innerHandler = Subject.behavior("b")
    val outerHandler = Subject.behavior(Seq[VMod]("a", data.test := "v", innerHandler))

    val node = div(
      idAttr := "strings",
      outerHandler,
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.outerHTML shouldBe """<div id="strings" data-test="v">ab</div>"""

      _ <- innerHandler.onNextIO("c") *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings" data-test="v">ac</div>"""

      _ <- outerHandler.onNextIO(Seq[VMod]("meh")) *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings">meh</div>"""
    } yield succeed
  }

  it should "work for nested observables with seq modifiers and attribute stream" in {
    val innerHandler = Subject.replayLatest[String]()
    val outerHandler = Subject.behavior(Seq[VMod]("a", data.test := "v", href <-- innerHandler))

    val node = div(
      idAttr := "strings",
      outerHandler,
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.outerHTML shouldBe """<div id="strings" data-test="v">a</div>"""

      _ <- innerHandler.onNextIO("c") *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings" data-test="v" href="c">a</div>"""

      _ <- innerHandler.onNextIO("d") *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings" data-test="v" href="d">a</div>"""

      _ <- outerHandler.onNextIO(Seq[VMod]("meh")) *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings">meh</div>"""
    } yield succeed
  }

  it should "work for double nested stream modifier" in {

    val myHandler = Subject.replayLatest[VMod]()

    val node = div(idAttr := "strings", div(myHandler))

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div></div>"

      _ <- myHandler.onNextIO(nonEmptyObservable[VMod](nonEmptyObservable[VMod](cls := "hans"))) *> IO.cede
      _  = element.innerHTML shouldBe """<div class="hans"></div>"""
    } yield succeed
  }

  it should "work for triple nested stream modifier" in {

    val myHandler = Subject.replayLatest[VMod]()

    val node = div(idAttr := "strings", div(myHandler))

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div></div>"

      _ <- myHandler.onNextIO(
             nonEmptyObservable[VMod](nonEmptyObservable[VMod](nonEmptyObservable(cls := "hans"))),
           ) *> IO.cede
      _ = element.innerHTML shouldBe """<div class="hans"></div>"""
    } yield succeed
  }

  it should "work for multiple nested stream modifier" in {

    val myHandler = Subject.replayLatest[VMod]()

    val node = div(idAttr := "strings", div(myHandler))

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div></div>"

      _ <- myHandler.onNextIO(
             nonEmptyObservable[VMod](VMod(nonEmptyObservable[VMod]("a"), nonEmptyObservable(span("b")))),
           ) *> IO.cede
      _ = element.innerHTML shouldBe """<div>a<span>b</span></div>"""
    } yield succeed
  }

  it should "work for nested attribute stream receiver" in {

    val myHandler = Subject.replayLatest[VMod]()

    val node = div(idAttr := "strings", div(myHandler))

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div></div>"

      _ <- myHandler.onNextIO(cls <-- nonEmptyObservable("hans")) *> IO.cede
      _  = element.innerHTML shouldBe """<div class="hans"></div>"""
    } yield succeed
  }

  it should "work for nested emitter" in {

    val myHandler = Subject.replayLatest[VMod]()

    val node = div(idAttr := "strings", div(idAttr := "click", myHandler))

    var clickCounter = 0

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe """<div id="click"></div>"""

      _ <- myHandler.onNextIO(onClick doAction (clickCounter += 1)) *> IO.cede
      _  = element.innerHTML shouldBe """<div id="click"></div>"""

      _  = clickCounter shouldBe 0
      _ <- IO(sendEvent(document.getElementById("click"), "click")) *> IO.cede
      _  = clickCounter shouldBe 1

      _ <- IO(sendEvent(document.getElementById("click"), "click")) *> IO.cede
      _  = clickCounter shouldBe 2

      _ <- myHandler.onNextIO(VMod.empty) *> IO.cede
      _  = element.innerHTML shouldBe """<div id="click"></div>"""

      _ <- IO(sendEvent(document.getElementById("click"), "click")) *> IO.cede
      _  = clickCounter shouldBe 2
    } yield succeed
  }

  it should "work for streaming accum attributes" in {

    val myClasses  = Subject.behavior("second")
    val myClasses2 = Subject.replayLatest[String]()

    val node = div(
      idAttr := "strings",
      div(
        cls := "first",
        myClasses.map { cls := _ },
        Seq[VMod](
          cls <-- myClasses2,
        ),
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = element.innerHTML shouldBe """<div class="first second"></div>"""

      _ <- myClasses2.onNextIO("third") *> IO.cede
      _  = element.innerHTML shouldBe """<div class="first second third"></div>"""

      _ <- myClasses2.onNextIO("more") *> IO.cede
      _  = element.innerHTML shouldBe """<div class="first second more"></div>"""

      _ <- myClasses.onNextIO("yeah") *> IO.cede
      _  = element.innerHTML shouldBe """<div class="first yeah more"></div>"""
    } yield succeed
  }

  "Observer/Observable types" should "work for behavior Subject" in {

    var mounts   = 0
    var unmounts = 0
    var updates  = 0

    for {
      myHandler  <- IO(Subject.behavior(-1))
      clsHandler <- IO(Subject.behavior("one"))
      node = div(
               idAttr := "strings",
               myHandler,
               cls <-- clsHandler,
               onClick.asEval(0) --> myHandler,
               onDomMount doAction { mounts += 1 },
               onDomUnmount doAction { unmounts += 1 },
               onDomUpdate doAction { updates += 1 },
             )
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.outerHTML shouldBe """<div id="strings" class="one">-1</div>"""
      _       = mounts shouldBe 1
      _       = unmounts shouldBe 0
      _       = updates shouldBe 0

      _ <- myHandler.onNextIO(1) *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings" class="one">1</div>"""
      _  = mounts shouldBe 1
      _  = unmounts shouldBe 0
      _  = updates shouldBe 1

      _ <- IO(sendEvent(document.getElementById("strings"), "click")) *> IO.cede

      _ = element.outerHTML shouldBe """<div id="strings" class="one">0</div>"""
      _ = mounts shouldBe 1
      _ = unmounts shouldBe 0
      _ = updates shouldBe 2

      _ <- clsHandler.onNextIO("two") *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings" class="two">0</div>"""
      _  = mounts shouldBe 1
      _  = unmounts shouldBe 0
      _  = updates shouldBe 3
    } yield succeed
  }

  it should "work for empty publish Subject" in {

    var mounts   = 0
    var unmounts = 0
    var updates  = 0

    for {
      myHandler  <- IO(Subject.publish[Int]())
      clsHandler <- IO(Subject.publish[String]())
      node = div(
               idAttr := "strings",
               myHandler,
               cls <-- clsHandler,
               onClick.as(0) --> myHandler,
               onDomMount doAction { mounts += 1 },
               onDomUnmount doAction { unmounts += 1 },
               onDomUpdate doAction { updates += 1 },
             )
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.outerHTML shouldBe """<div id="strings"></div>"""
      _       = mounts shouldBe 1
      _       = unmounts shouldBe 0
      _       = updates shouldBe 0

      _ <- myHandler.onNextIO(-1) *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings">-1</div>"""
      _  = mounts shouldBe 1
      _  = unmounts shouldBe 0
      _  = updates shouldBe 1

      _ <- clsHandler.onNextIO("one") *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings" class="one">-1</div>"""
      _  = mounts shouldBe 1
      _  = unmounts shouldBe 0
      _  = updates shouldBe 2

      _ <- myHandler.onNextIO(1) *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings" class="one">1</div>"""
      _  = mounts shouldBe 1
      _  = unmounts shouldBe 0
      _  = updates shouldBe 3

      _ <- IO(sendEvent(document.getElementById("strings"), "click")) *> IO.cede

      _ = element.outerHTML shouldBe """<div id="strings" class="one">0</div>"""
      _ = mounts shouldBe 1
      _ = unmounts shouldBe 0
      _ = updates shouldBe 4

      _ <- clsHandler.onNextIO("two") *> IO.cede
      _  = element.outerHTML shouldBe """<div id="strings" class="two">0</div>"""
      _  = mounts shouldBe 1
      _  = unmounts shouldBe 0
      _  = updates shouldBe 5
    } yield succeed
  }

  "ChildCommand" should "work in handler" in {
    val cmds = Subject.replayLatest[ChildCommand]()

    val node = div(
      idAttr := "strings",
      cmds,
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe ""

      _ <- cmds.onNextIO(ChildCommand.ReplaceAll(b("Hello World") :: span("!") :: Nil)) *> IO.cede
      _  = element.innerHTML shouldBe """<b>Hello World</b><span>!</span>"""

      _ <- cmds.onNextIO(ChildCommand.Insert(1, p("and friends", dsl.key := 42))) *> IO.cede
      _  = element.innerHTML shouldBe """<b>Hello World</b><p>and friends</p><span>!</span>"""

      _ <- cmds.onNextIO(ChildCommand.Move(1, 2)) *> IO.cede
      _  = element.innerHTML shouldBe """<b>Hello World</b><span>!</span><p>and friends</p>"""

      _ <- cmds.onNextIO(ChildCommand.MoveId(ChildCommand.ChildId.Key(42), 1)) *> IO.cede
      _  = element.innerHTML shouldBe """<b>Hello World</b><p>and friends</p><span>!</span>"""

      _ <- cmds.onNextIO(ChildCommand.RemoveId(ChildCommand.ChildId.Key(42))) *> IO.cede
      _  = element.innerHTML shouldBe """<b>Hello World</b><span>!</span>"""
    } yield succeed
  }

  it should "work in handler with element reference" in {
    val initialChildren =
      p(
        "How much?",
        dsl.key := "question-1",
        idAttr  := "id-1",
      ) ::
        p(
          "Why so cheap?",
          dsl.key := "question-2",
          idAttr  := "id-2",
        ) ::
        Nil

    val cmds = Subject.behavior[ChildCommand](ChildCommand.ReplaceAll(initialChildren))

    val node = div(
      "Questions?",
      div(
        idAttr := "strings",
        onClick.map(ev => ChildCommand.RemoveId(ChildCommand.ChildId.Element(ev.target))) --> cmds,
        cmds,
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe """<p id="id-1">How much?</p><p id="id-2">Why so cheap?</p>"""

      _ <- IO(sendEvent(document.getElementById("id-1"), "click")) *> IO.cede

      _ = element.innerHTML shouldBe """<p id="id-2">Why so cheap?</p>"""

      _ <- cmds.onNextIO(ChildCommand.InsertBeforeId(ChildCommand.ChildId.Key("question-2"), div("spam"))) *> IO.cede
      _  = element.innerHTML shouldBe """<div>spam</div><p id="id-2">Why so cheap?</p>"""

      _ <- cmds.onNextIO(ChildCommand.MoveId(ChildCommand.ChildId.Key("question-2"), 0)) *> IO.cede
      _  = element.innerHTML shouldBe """<p id="id-2">Why so cheap?</p><div>spam</div>"""

      _ <- IO(sendEvent(document.getElementById("id-2"), "click")) *> IO.cede
      _  = element.innerHTML shouldBe """<div>spam</div>"""
    } yield succeed
  }

  it should "work in value observable" in {
    val cmds = Subject.replayLatest[ChildCommand]()

    val node = div(
      idAttr := "strings",
      cmds.prepend(ChildCommand.ReplaceAll(div("huch") :: Nil)),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe "<div>huch</div>"

      _ <- cmds.onNextIO(ChildCommand.Prepend(b("nope"))) *> IO.cede
      _  = element.innerHTML shouldBe """<b>nope</b><div>huch</div>"""

      _ <- cmds.onNextIO(ChildCommand.Move(0, 1)) *> IO.cede
      _  = element.innerHTML shouldBe """<div>huch</div><b>nope</b>"""

      _ <- cmds.onNextIO(ChildCommand.Replace(1, b("yes"))) *> IO.cede
      _  = element.innerHTML shouldBe """<div>huch</div><b>yes</b>"""
    } yield succeed
  }

  "Emitter subscription" should "be correctly subscribed" in {

    val clicks = Subject.behavior[Int](0)

    var incCounter = 0
    var mapCounter = 0
    val innerMod   = onClick.transform(_ => clicks.map { c => mapCounter += 1; c }) doAction { incCounter += 1 }
    val modHandler = Subject.behavior(innerMod)

    val innerNode   = div(modHandler)
    val nodeHandler = Subject.behavior(innerNode)

    val node = div(
      idAttr := "strings",
      nodeHandler,
    )

    incCounter shouldBe 0
    mapCounter shouldBe 0

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      _ = incCounter shouldBe 1
      _ = mapCounter shouldBe 1

      _ <- clicks.onNextIO(1) *> IO.cede
      _  = incCounter shouldBe 2
      _  = mapCounter shouldBe 2

      _ <- modHandler.onNextIO(VMod.empty) *> IO.cede
      _  = incCounter shouldBe 2
      _  = mapCounter shouldBe 2

      _ <- clicks.onNextIO(2) *> IO.cede
      _  = incCounter shouldBe 2
      _  = mapCounter shouldBe 2

      _ <- modHandler.onNextIO(innerMod) *> IO.cede
      _  = incCounter shouldBe 3
      _  = mapCounter shouldBe 3

      _ <- clicks.onNextIO(3) *> IO.cede
      _  = incCounter shouldBe 4
      _  = mapCounter shouldBe 4

      _ <- nodeHandler.onNextIO(span) *> IO.cede
      _  = incCounter shouldBe 4
      _  = mapCounter shouldBe 4

      _ <- clicks.onNextIO(4) *> IO.cede
      _  = incCounter shouldBe 4
      _  = mapCounter shouldBe 4
    } yield succeed
  }

  it should "be correctly subscribed for emitterbuilder.asLatestEmitter" in {

    var aCounter          = 0
    var bCounter          = 0
    var lastValue: String = null

    val aEvent = Subject.publish[String]()
    val bEvent = Subject.publish[String]()
    val innerNode =
      input(
        EmitterBuilder
          .fromSource(aEvent)
          .map { x => aCounter += 1; x }
          .asLatestEmitter(EmitterBuilder.fromSource(bEvent).map { x => bCounter += 1; x }) foreach { str =>
          lastValue = str
        },
      )

    val handler = Subject.behavior[VMod](innerNode)
    val node = div(
      idAttr := "strings",
      handler,
    )

    for {
      _ <- aEvent.onNextIO("nope?") *> IO.cede
      _ <- bEvent.onNextIO("nope?") *> IO.cede
      _  = aCounter shouldBe 0
      _  = bCounter shouldBe 0
      _  = lastValue shouldBe null

      _ <- Outwatch.renderInto[IO]("#app", node)

      _ = aCounter shouldBe 0
      _ = bCounter shouldBe 0
      _ = lastValue shouldBe null

      _ <- aEvent.onNextIO("a") *> IO.cede
      _  = aCounter shouldBe 1
      _  = bCounter shouldBe 0
      _  = lastValue shouldBe null

      _ <- bEvent.onNextIO("b") *> IO.cede
      _  = aCounter shouldBe 1
      _  = bCounter shouldBe 1
      _  = lastValue shouldBe null

      _ <- aEvent.onNextIO("a2") *> IO.cede
      _  = aCounter shouldBe 2
      _  = bCounter shouldBe 1
      _  = lastValue shouldBe "b"

      _ <- bEvent.onNextIO("ahja") *> IO.cede
      _  = aCounter shouldBe 2
      _  = bCounter shouldBe 2
      _  = lastValue shouldBe "b"

      _ <- aEvent.onNextIO("oh") *> IO.cede
      _  = aCounter shouldBe 3
      _  = bCounter shouldBe 2
      _  = lastValue shouldBe "ahja"

      _ <- handler.onNextIO(VMod.empty) *> IO.cede

      _ <- aEvent.onNextIO("hmm?") *> IO.cede
      _  = aCounter shouldBe 3
      _  = bCounter shouldBe 2
      _  = lastValue shouldBe "ahja"

      _ <- bEvent.onNextIO("no?") *> IO.cede
      _  = aCounter shouldBe 3
      _  = bCounter shouldBe 2
      _  = lastValue shouldBe "ahja"

    } yield succeed
  }

  "Thunk" should "work" in {
    val myString: Subject[String] = Subject.replayLatest[String]()

    var mountCount      = 0
    var preupdateCount  = 0
    var updateCount     = 0
    var unmountCount    = 0
    var renderFnCounter = 0
    val node = div(
      idAttr := "strings",
      myString.map { myString =>
        b(idAttr := "bla").thunk("component")(myString) {
          renderFnCounter += 1
          VMod(
            cls := "b",
            myString,
            onDomMount.doAction { mountCount += 1 },
            onDomPreUpdate.doAction { preupdateCount += 1 },
            onDomUpdate.doAction { updateCount += 1 },
            onDomUnmount.doAction { unmountCount += 1 },
          )
        }
      },
      b("something else"),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = renderFnCounter shouldBe 0
      _ = mountCount shouldBe 0
      _ = preupdateCount shouldBe 0
      _ = updateCount shouldBe 0
      _ = unmountCount shouldBe 0
      _ = element.innerHTML shouldBe "<b>something else</b>"

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mountCount shouldBe 1
      _  = preupdateCount shouldBe 0
      _  = updateCount shouldBe 0
      _  = unmountCount shouldBe 0
      _  = element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mountCount shouldBe 1
      _  = preupdateCount shouldBe 1
      _  = updateCount shouldBe 1
      _  = unmountCount shouldBe 0
      _  = element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("hai!") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mountCount shouldBe 2
      _  = preupdateCount shouldBe 1
      _  = updateCount shouldBe 1
      _  = unmountCount shouldBe 1
      _  = element.innerHTML shouldBe """<b id="bla" class="b">hai!</b><b>something else</b>"""

      _ <- myString.onNextIO("fuchs.") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mountCount shouldBe 3
      _  = preupdateCount shouldBe 1
      _  = updateCount shouldBe 1
      _  = unmountCount shouldBe 2
      _  = element.innerHTML shouldBe """<b id="bla" class="b">fuchs.</b><b>something else</b>"""
    } yield succeed
  }

  it should "work with equals" in {
    val myString: Subject[String] = Subject.replayLatest[String]()

    var equalsCounter = 0
    class Wrap(val s: String) {
      override def equals(other: Any) = {
        equalsCounter += 1
        other match {
          case w: Wrap => s == w.s
          case _       => false
        }
      }
    }

    var renderFnCounter = 0
    val node = div(
      idAttr := "strings",
      myString.map { myString =>
        b(idAttr := "bla").thunk("component")(new Wrap(myString)) {
          renderFnCounter += 1
          Seq[VMod](cls   := "b", myString)
        }
      },
      b("something else"),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = renderFnCounter shouldBe 0
      _ = equalsCounter shouldBe 0
      _ = element.innerHTML shouldBe "<b>something else</b>"

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = equalsCounter shouldBe 0
      _  = element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = equalsCounter shouldBe 1
      _  = element.innerHTML shouldBe """<b id="bla" class="b">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("hai!") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = equalsCounter shouldBe 2
      _  = element.innerHTML shouldBe """<b id="bla" class="b">hai!</b><b>something else</b>"""
    } yield succeed
  }

  it should "work with inner stream" in {
    val myString: Subject[String] = Subject.replayLatest[String]()
    val myThunk: Subject[Unit]    = Subject.replayLatest[Unit]()

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
      },
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = renderFnCounter shouldBe 0
      _ = element.innerHTML shouldBe ""

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b></b>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b></b>"

      _ <- myString.onNextIO("ok?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b><p>ok?</p></b>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b><p>ok?</p></b>"

      _ <- myString.onNextIO("hope so") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b><p>hope so</p></b>"

      _ <- myString.onNextIO("ohai") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b><p>ohai</p></b>"
    } yield succeed
  }

  it should "work with inner and adjacent stream" in {
    val myString: Subject[String] = Subject.replayLatest[String]()
    val myOther: Subject[String]  = Subject.replayLatest[String]()
    val myThunk: Subject[Int]     = Subject.replayLatest[Int]()

    var renderFnCounter = 0
    val node = div(
      idAttr := "strings",
      myThunk.map { i =>
        b.thunk("component")(i) {
          renderFnCounter += 1
          VMod(
            i,
            myString.map { str =>
              if (str.isEmpty) span("nope")
              else p(dsl.key := str, str)
            },
          )
        }
      },
      myOther,
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = renderFnCounter shouldBe 0
      _ = element.innerHTML shouldBe ""

      _ <- myThunk.onNextIO(0) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b>0</b>"

      _ <- myThunk.onNextIO(0) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b>0</b>"

      _ <- myString.onNextIO("ok?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b>0<p>ok?</p></b>"

      _ <- myString.onNextIO("got it?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b>0<p>got it?</p></b>"

      _ <- myOther.onNextIO("nope") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<b>0<p>got it?</p></b>nope"

      _ <- myThunk.onNextIO(1) *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = element.innerHTML shouldBe "<b>1<p>got it?</p></b>nope"

      _ <- myThunk.onNextIO(2) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = element.innerHTML shouldBe "<b>2<p>got it?</p></b>nope"

      _ <- myOther.onNextIO("yes") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = element.innerHTML shouldBe "<b>2<p>got it?</p></b>yes"

      _ <- myString.onNextIO("sad?") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = element.innerHTML shouldBe "<b>2<p>sad?</p></b>yes"

      _ <- myString.onNextIO("hungry?") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = element.innerHTML shouldBe "<b>2<p>hungry?</p></b>yes"

      _ <- myString.onNextIO("thursty?") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = element.innerHTML shouldBe "<b>2<p>thursty?</p></b>yes"

      _ <- myOther.onNextIO("maybe") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = element.innerHTML shouldBe "<b>2<p>thursty?</p></b>maybe"

      _ <- myThunk.onNextIO(3) *> IO.cede
      _  = renderFnCounter shouldBe 4
      _  = element.innerHTML shouldBe "<b>3<p>thursty?</p></b>maybe"

      _ <- myString.onNextIO("") *> IO.cede
      _  = renderFnCounter shouldBe 4
      _  = element.innerHTML shouldBe "<b>3<span>nope</span></b>maybe"

      _ <- myOther.onNextIO("come on") *> IO.cede
      _  = renderFnCounter shouldBe 4
      _  = element.innerHTML shouldBe "<b>3<span>nope</span></b>come on"

      _ <- myThunk.onNextIO(3) *> IO.cede
      _  = renderFnCounter shouldBe 4
      _  = element.innerHTML shouldBe "<b>3<span>nope</span></b>come on"

      _ <- myOther.onNextIO("hey?") *> IO.cede
      _  = renderFnCounter shouldBe 4
      _  = element.innerHTML shouldBe "<b>3<span>nope</span></b>hey?"

      _ <- myString.onNextIO("oh") *> IO.cede
      _  = renderFnCounter shouldBe 4
      _  = element.innerHTML shouldBe "<b>3<p>oh</p></b>hey?"

      _ <- myOther.onNextIO("ah") *> IO.cede
      _  = renderFnCounter shouldBe 4
      _  = element.innerHTML shouldBe "<b>3<p>oh</p></b>ah"

      _ <- myThunk.onNextIO(3) *> IO.cede
      _  = renderFnCounter shouldBe 4
      _  = element.innerHTML shouldBe "<b>3<p>oh</p></b>ah"

      _ <- myThunk.onNextIO(4) *> IO.cede
      _  = renderFnCounter shouldBe 5
      _  = element.innerHTML shouldBe "<b>4<p>oh</p></b>ah"
    } yield succeed
  }

  it should "work with nested inner stream" in {
    val myString: Subject[String]     = Subject.replayLatest[String]()
    val myInner: Subject[String]      = Subject.replayLatest[String]()
    val myInnerOther: Subject[String] = Subject.replayLatest[String]()
    val myOther: Subject[String]      = Subject.replayLatest[String]()
    val myThunk: Subject[Unit]        = Subject.replayLatest[Unit]()

    var renderFnCounter = 0
    val node = div(
      idAttr := "strings",
      myThunk.map { _ =>
        div(
          b.thunkStatic("component") {
            renderFnCounter += 1
            myString.map { str =>
              div(
                if (str.isEmpty) div("empty") else p.thunk("inner")(str)(VMod(str, myInner)),
                myInnerOther,
              )
            }
          },
          myOther,
        )
      },
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = renderFnCounter shouldBe 0
      _ = element.innerHTML shouldBe ""

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b></b></div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b></b></div>"

      _ <- myString.onNextIO("ok?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      _ <- myString.onNextIO("ok?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ok?</p></div></b></div>"

      _ <- myString.onNextIO("hope so") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>hope so</p></div></b></div>"

      _ <- myString.onNextIO("ohai") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ohai</p></div></b></div>"

      _ <- myString.onNextIO("") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><div>empty</div></div></b></div>"

      _ <- myInner.onNextIO("!") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><div>empty</div></div></b></div>"

      _ <- myString.onNextIO("torst") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>torst!</p></div></b></div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>torst!</p></div></b></div>"

      _ <- myInner.onNextIO("?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>torst?</p></div></b></div>"

      _ <- myString.onNextIO("gandalf") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b></div>"

      _ <- myString.onNextIO("gandalf") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b></div>"

      _ <- myOther.onNextIO("du") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b>du</div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gandalf?</p></div></b>du</div>"

      _ <- myInner.onNextIO("!") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gandalf!</p></div></b>du</div>"

      _ <- myString.onNextIO("fanta") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      _ <- myString.onNextIO("fanta") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>du</div>"

      _ <- myOther.onNextIO("nee") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>fanta!</p></div></b>nee</div>"

      _ <- myInnerOther.onNextIO("muh") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>fanta!</p>muh</div></b>nee</div>"

      _ <- myString.onNextIO("ghost") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      _ <- myString.onNextIO("ghost") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>nee</div>"

      _ <- myOther.onNextIO("life") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      _ <- myInnerOther.onNextIO("muh") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      _ <- myString.onNextIO("ghost") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>life</div>"

      _ <- myOther.onNextIO("seen") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>seen</div>"

      _ <- myInnerOther.onNextIO("muh") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>ghost!</p>muh</div></b>seen</div>"

      _ <- myString.onNextIO("") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><div>empty</div>muh</div></b>seen</div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><div>empty</div>muh</div></b>seen</div>"

      _ <- myString.onNextIO("gott") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gott!</p>muh</div></b>seen</div>"

      _ <- myOther.onNextIO("dorf") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gott!</p>muh</div></b>dorf</div>"

      _ <- myInnerOther.onNextIO("ach") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      _ <- myString.onNextIO("gott") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gott!</p>ach</div></b>dorf</div>"

      _ <- myInner.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gotthans</p>ach</div></b>dorf</div>"

      _ <- myOther.onNextIO("das") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gotthans</p>ach</div></b>das</div>"

      _ <- myInnerOther.onNextIO("tank") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gotthans</p>tank</div></b>das</div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gotthans</p>tank</div></b>das</div>"

      _ <- myInner.onNextIO("so") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gottso</p>tank</div></b>das</div>"

      _ <- myOther.onNextIO("datt") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gottso</p>tank</div></b>datt</div>"

      _ <- myInnerOther.onNextIO("ohje") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gottso</p>ohje</div></b>datt</div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>gottso</p>ohje</div></b>datt</div>"

      _ <- myString.onNextIO("ende") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>datt</div>"

      _ <- myString.onNextIO("ende") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>datt</div>"

      _ <- myOther.onNextIO("fin") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>fin</div>"

      _ <- myThunk.onNextIO(()) *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = element.innerHTML shouldBe "<div><b><div><p>endeso</p>ohje</div></b>fin</div>"
    } yield succeed
  }

  it should "work with streams (switchMap)" in {
    val myString: Subject[String]   = Subject.replayLatest[String]()
    val myId: Subject[String]       = Subject.replayLatest[String]()
    val myInner: Subject[String]    = Subject.replayLatest[String]()
    val myOther: Subject[VMod]      = Subject.replayLatest[VMod]()
    val thunkContent: Subject[VMod] = Subject.replayLatest[VMod]()

    var renderFnCounter = 0
    var mounts          = List.empty[Int]
    var preupdates      = List.empty[Int]
    var updates         = List.empty[Int]
    var unmounts        = List.empty[Int]
    var counter         = 0
    def mountHooks = {
      val c = counter
      counter += 1
      VMod(
        onDomMount.doAction { mounts :+= c },
        onDomPreUpdate.doAction { preupdates :+= c },
        onDomUpdate.doAction { updates :+= c },
        onDomUnmount.doAction { unmounts :+= c },
      )
    }
    val node = div(
      idAttr := "strings",
      myString.switchMap { myString =>
        if (myString == "empty") nonEmptyObservable(b.thunk("component")(myString)(VMod("empty", mountHooks)))
        else
          myInner
            .map[VNode](s => div(s, mountHooks))
            .prepend(b(idAttr <-- myId).thunk("component")(myString) {
              renderFnCounter += 1
              VMod(cls        := "b", myString, mountHooks, thunkContent)
            })
      },
      myOther,
      b("something else"),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = renderFnCounter shouldBe 0
      _ = mounts shouldBe Nil
      _ = preupdates shouldBe Nil
      _ = updates shouldBe Nil
      _ = unmounts shouldBe Nil
      _ = element.innerHTML shouldBe "<b>something else</b>"

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe Nil
      _  = updates shouldBe Nil
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b">wal?</b><b>something else</b>"""

      _ <- myId.onNextIO("tier") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe List(0)
      _  = updates shouldBe List(0)
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe List(0, 0)
      _  = updates shouldBe List(0, 0)
      _  = unmounts shouldBe List()
      _  = element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("hai!") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1)
      _  = preupdates shouldBe List(0, 0)
      _  = updates shouldBe List(0, 0)
      _  = unmounts shouldBe List(0)
      _  = element.innerHTML shouldBe """<b class="b" id="tier">hai!</b><b>something else</b>"""

      _ <- myId.onNextIO("nope") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1)
      _  = preupdates shouldBe List(0, 0, 1)
      _  = updates shouldBe List(0, 0, 1)
      _  = unmounts shouldBe List(0)
      _  = element.innerHTML shouldBe """<b class="b" id="nope">hai!</b><b>something else</b>"""

      _ <- myString.onNextIO("empty") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1, 2)
      _  = preupdates shouldBe List(0, 0, 1)
      _  = updates shouldBe List(0, 0, 1)
      _  = unmounts shouldBe List(0, 1)
      _  = element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      _ <- myId.onNextIO("nothing") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1, 2)
      _  = preupdates shouldBe List(0, 0, 1)
      _  = updates shouldBe List(0, 0, 1)
      _  = unmounts shouldBe List(0, 1)
      _  = element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      _ <- myString.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1)
      _  = updates shouldBe List(0, 0, 1)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="nothing" class="b">hans</b><b>something else</b>"""

      _ <- myId.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3)
      _  = updates shouldBe List(0, 0, 1, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      _ <- myString.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      _ <- thunkContent.onNextIO(p(dsl.key := "1", "el dieter")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter</p></b><b>something else</b>"""

      _ <- thunkContent.onNextIO(p(dsl.key := "2", "el dieter II")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><b>something else</b>"""

      _ <- myOther.onNextIO(div("baem!")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _ =
        element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><div>baem!</div><b>something else</b>"""

      _ <- myInner.onNextIO("meh") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3, 4)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2, 3)
      _  = element.innerHTML shouldBe """<div>meh</div><div>baem!</div><b>something else</b>"""

      _ <- myOther.onNextIO(div("fini")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3, 4)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2, 3)
      _  = element.innerHTML shouldBe """<div>meh</div><div>fini</div><b>something else</b>"""
    } yield succeed
  }

  it should "work with streams (switchMap 2)" in {
    val myString: Subject[String]   = Subject.replayLatest[String]()
    val myId: Subject[String]       = Subject.replayLatest[String]()
    val myInner: Subject[String]    = Subject.replayLatest[String]()
    val myOther: Subject[VMod]      = Subject.replayLatest[VMod]()
    val thunkContent: Subject[VMod] = Subject.replayLatest[VMod]()

    var renderFnCounter = 0
    var mounts          = List.empty[Int]
    var preupdates      = List.empty[Int]
    var updates         = List.empty[Int]
    var unmounts        = List.empty[Int]
    var counter         = 0
    def mountHooks = {
      val c = counter
      counter += 1
      VMod(
        onDomMount.doAction { mounts :+= c },
        onDomPreUpdate.doAction { preupdates :+= c },
        onDomUpdate.doAction { updates :+= c },
        onDomUnmount.doAction { unmounts :+= c },
      )
    }
    val node = div(
      idAttr := "strings",
      myString.switchMap { myString =>
        if (myString == "empty") nonEmptyObservable(b.thunk("component")(myString)(VMod("empty", mountHooks)))
        else
          myInner
            .map[VNode](s => div(s, mountHooks))
            .prepend(b(idAttr <-- myId).thunk("component")(myString) {
              renderFnCounter += 1
              VMod(cls        := "b", myString, mountHooks, thunkContent)
            })
      },
      myOther,
      b("something else"),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = renderFnCounter shouldBe 0
      _ = mounts shouldBe Nil
      _ = preupdates shouldBe Nil
      _ = updates shouldBe Nil
      _ = unmounts shouldBe Nil
      _ = element.innerHTML shouldBe "<b>something else</b>"

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe Nil
      _  = updates shouldBe Nil
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b">wal?</b><b>something else</b>"""

      _ <- myId.onNextIO("tier") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe List(0)
      _  = updates shouldBe List(0)
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe List(0, 0)
      _  = updates shouldBe List(0, 0)
      _  = unmounts shouldBe List()
      _  = element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("hai!") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1)
      _  = preupdates shouldBe List(0, 0)
      _  = updates shouldBe List(0, 0)
      _  = unmounts shouldBe List(0)
      _  = element.innerHTML shouldBe """<b class="b" id="tier">hai!</b><b>something else</b>"""

      _ <- myId.onNextIO("nope") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1)
      _  = preupdates shouldBe List(0, 0, 1)
      _  = updates shouldBe List(0, 0, 1)
      _  = unmounts shouldBe List(0)
      _  = element.innerHTML shouldBe """<b class="b" id="nope">hai!</b><b>something else</b>"""

      _ <- myString.onNextIO("empty") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1, 2)
      _  = preupdates shouldBe List(0, 0, 1)
      _  = updates shouldBe List(0, 0, 1)
      _  = unmounts shouldBe List(0, 1)
      _  = element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      _ <- myId.onNextIO("nothing") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1, 2)
      _  = preupdates shouldBe List(0, 0, 1)
      _  = updates shouldBe List(0, 0, 1)
      _  = unmounts shouldBe List(0, 1)
      _  = element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      _ <- myString.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1)
      _  = updates shouldBe List(0, 0, 1)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="nothing" class="b">hans</b><b>something else</b>"""

      _ <- myId.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3)
      _  = updates shouldBe List(0, 0, 1, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      _ <- myString.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      _ <- thunkContent.onNextIO(p(dsl.key := "1", "el dieter")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter</p></b><b>something else</b>"""

      _ <- thunkContent.onNextIO(p(dsl.key := "2", "el dieter II")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><b>something else</b>"""

      _ <- myOther.onNextIO(div("baem!")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _ =
        element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><div>baem!</div><b>something else</b>"""

      _ <- myInner.onNextIO("meh") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3, 4)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2, 3)
      _  = element.innerHTML shouldBe """<div>meh</div><div>baem!</div><b>something else</b>"""

      _ <- myOther.onNextIO(div("fini")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3, 4)
      _  = preupdates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2, 3)
      _  = element.innerHTML shouldBe """<div>meh</div><div>fini</div><b>something else</b>"""
    } yield succeed
  }

  it should "work with streams" in {
    val myString: Subject[String]   = Subject.replayLatest[String]()
    val myId: Subject[String]       = Subject.replayLatest[String]()
    val myInner: Subject[String]    = Subject.replayLatest[String]()
    val myOther: Subject[VMod]      = Subject.replayLatest[VMod]()
    val thunkContent: Subject[VMod] = Subject.replayLatest[VMod]()

    var renderFnCounter = 0
    var mounts          = List.empty[Int]
    var preupdates      = List.empty[Int]
    var updates         = List.empty[Int]
    var unmounts        = List.empty[Int]
    var counter         = 0
    def mountHooks = {
      val c = counter
      counter += 1
      VMod(
        onDomMount.doAction { mounts :+= c },
        onDomPreUpdate.doAction { preupdates :+= c },
        onDomUpdate.doAction { updates :+= c },
        onDomUnmount.doAction { unmounts :+= c },
      )
    }
    val node = div(
      idAttr := "strings",
      myString.map { myString =>
        if (myString == "empty") VMod(b.thunk("component")(myString)(VMod("empty", mountHooks)))
        else
          VMod(
            myInner
              .map[VNode](s => div(s, mountHooks))
              .prepend(b(idAttr <-- myId).thunk("component")(myString) {
                renderFnCounter += 1
                VMod(cls        := "b", myString, mountHooks, thunkContent)
              }),
          )
      },
      myOther,
      b("something else"),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")

      _ = renderFnCounter shouldBe 0
      _ = mounts shouldBe Nil
      _ = preupdates shouldBe Nil
      _ = updates shouldBe Nil
      _ = unmounts shouldBe Nil
      _ = element.innerHTML shouldBe "<b>something else</b>"

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe Nil
      _  = updates shouldBe Nil
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b">wal?</b><b>something else</b>"""

      _ <- myId.onNextIO("tier") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe List(0)
      _  = updates shouldBe List(0)
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      _ <- myId.onNextIO("tier") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe List(0, 0)
      _  = updates shouldBe List(0, 0)
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("wal?") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe List(0, 0, 0)
      _  = updates shouldBe List(0, 0, 0)
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      _ <- myId.onNextIO("tier") *> IO.cede
      _  = renderFnCounter shouldBe 1
      _  = mounts shouldBe List(0)
      _  = preupdates shouldBe List(0, 0, 0, 0)
      _  = updates shouldBe List(0, 0, 0, 0)
      _  = unmounts shouldBe Nil
      _  = element.innerHTML shouldBe """<b class="b" id="tier">wal?</b><b>something else</b>"""

      _ <- myString.onNextIO("hai!") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1)
      _  = preupdates shouldBe List(0, 0, 0, 0)
      _  = updates shouldBe List(0, 0, 0, 0)
      _  = unmounts shouldBe List(0)
      _  = element.innerHTML shouldBe """<b class="b" id="tier">hai!</b><b>something else</b>"""

      _ <- myId.onNextIO("nope") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1)
      _  = updates shouldBe List(0, 0, 0, 0, 1)
      _  = unmounts shouldBe List(0)
      _  = element.innerHTML shouldBe """<b class="b" id="nope">hai!</b><b>something else</b>"""

      _ <- myString.onNextIO("empty") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1, 2)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1)
      _  = updates shouldBe List(0, 0, 0, 0, 1)
      _  = unmounts shouldBe List(0, 1)
      _  = element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      _ <- myId.onNextIO("nothing") *> IO.cede
      _  = renderFnCounter shouldBe 2
      _  = mounts shouldBe List(0, 1, 2)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1)
      _  = updates shouldBe List(0, 0, 0, 0, 1)
      _  = unmounts shouldBe List(0, 1)
      _  = element.innerHTML shouldBe """<b>empty</b><b>something else</b>"""

      _ <- myString.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1)
      _  = updates shouldBe List(0, 0, 0, 0, 1)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="nothing" class="b">hans</b><b>something else</b>"""

      _ <- myId.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1, 3)
      _  = updates shouldBe List(0, 0, 0, 0, 1, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      _ <- myString.onNextIO("hans") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1, 3, 3)
      _  = updates shouldBe List(0, 0, 0, 0, 1, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans</b><b>something else</b>"""

      _ <- thunkContent.onNextIO(p(dsl.key := "1", "el dieter")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter</p></b><b>something else</b>"""

      _ <- thunkContent.onNextIO(p(dsl.key := "2", "el dieter II")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _  = element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><b>something else</b>"""

      _ <- myOther.onNextIO(div("baem!")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2)
      _ =
        element.innerHTML shouldBe """<b id="hans" class="b">hans<p>el dieter II</p></b><div>baem!</div><b>something else</b>"""

      _ <- myInner.onNextIO("meh") *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3, 4)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2, 3)
      _  = element.innerHTML shouldBe """<div>meh</div><div>baem!</div><b>something else</b>"""

      _ <- myOther.onNextIO(div("fini")) *> IO.cede
      _  = renderFnCounter shouldBe 3
      _  = mounts shouldBe List(0, 1, 2, 3, 4)
      _  = preupdates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3, 3)
      _  = updates shouldBe List(0, 0, 0, 0, 1, 3, 3, 3, 3)
      _  = unmounts shouldBe List(0, 1, 2, 3)
      _  = element.innerHTML shouldBe """<div>meh</div><div>fini</div><b>something else</b>"""
    } yield succeed
  }

  "Nested VNode" should "work without outdated patch" in {
    val myList: Subject[List[String]] = Subject.behavior[List[String]](List("hi"))
    val isSynced: Subject[Int]        = Subject.behavior[Int](-1)
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
              if (i <= isSynced) div(dsl.key := "something", "a", counter) else div("b", counter)
            }

            node.map { node =>
              div(
                div(node),
                syncedIcon,
                counter,
              )
            }
          }
        }),
      ),
      button(
        "Edit",
        idAttr := "edit-button",
        onClick.asLatest(myList).map(l => l.init ++ l.lastOption.map(_ + "!")) --> myList,
        onClick.asLatest(isSynced).map(_ + 1) --> isSynced,
      ),
    )

    val errors = mutable.ArrayBuffer[Throwable]()
    OutwatchTracing.error.unsafeForeach(errors += _)

    Outwatch.renderInto[IO]("#app", node).flatMap { _ =>
      val editButton = document.getElementById("edit-button")

      for {
        _ <- IO(sendEvent(editButton, "click")) *> IO.cede
        _ <- IO.sleep(1.seconds)
        _ <- IO(sendEvent(editButton, "click")) *> IO.cede
        _ <- IO.sleep(1.seconds)
        _  = errors.toList shouldBe List.empty
      } yield succeed
    }
  }

  // TODO: this test does not actually fail if it is wrong, but you just see an error message in the console output. Fix when merging error observable in OutwatchTracing.
  it should "work without outdated patch in thunk" in {
    val myList: Subject[List[String]] = Subject.behavior[List[String]](List("hi"))
    val isSynced: Subject[Int]        = Subject.behavior[Int](-1)
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
              if (i <= isSynced) div(dsl.key := "something", "a", counter) else div("b", counter)
            }

            node.map { node =>
              div(
                div(node),
                syncedIcon,
                counter,
              )
            }
          }
        }),
      ),
      button(
        "Edit",
        idAttr := "edit-button",
        onClick.asLatest(myList).map(l => l.init ++ l.lastOption.map(_ + "!")) --> myList,
        onClick.asLatest(isSynced).map(_ + 1) --> isSynced,
      ),
    )

    Outwatch.renderInto[IO]("#app", node).flatMap { _ =>
      val editButton = document.getElementById("edit-button")

      for {
        _ <- IO.unit
        _  = sendEvent(editButton, "click")
        _ <- IO.sleep(1.seconds)
        _  = sendEvent(editButton, "click")
        _ <- IO.sleep(1.seconds)
      } yield succeed
    }
  }

  "Custom Emitter builder" should "work with events" in {
    import scala.concurrent.duration._

    val clickableView: EmitterBuilder[Boolean, VMod] = EmitterBuilder[Boolean, VMod] { sink =>
      VMod(
        display.flex,
        minWidth := "0px",
        onMouseDown.as(true) --> sink,
        onMouseUp.as(false) --> sink,
      )
    }

    for {
      handler <- IO(Subject.replayLatest[String]())
      node = div(
               idAttr := "strings",
               clickableView.map {
                 case true  => "yes"
                 case false => "no"
               } --> handler,
               handler,
             )
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe ""

      _  = sendEvent(element, "mousedown")
      _ <- IO.sleep(0.1.seconds)
      _  = element.innerHTML shouldBe "yes"

      _  = sendEvent(element, "mouseup")
      _ <- IO.sleep(0.1.seconds)
      _  = element.innerHTML shouldBe "no"
    } yield succeed
  }

  it should "work with events as combined emitterbuidler" in {
    import scala.concurrent.duration._

    val clickableView: EmitterBuilder[Boolean, VMod] = EmitterBuilder[Boolean, VMod] { sink =>
      VMod(
        display.flex,
        minWidth := "0px",
        Monoid[EmitterBuilder[Boolean, VMod]].combine(onMouseDown.as(true), onMouseUp.as(false)) --> sink,
      )
    }

    for {
      handler <- IO(Subject.replayLatest[String]())
      node = div(
               idAttr := "strings",
               clickableView.map {
                 case true  => "yes"
                 case false => "no"
               } --> handler,
               handler,
             )
      _ <- Outwatch.renderInto[IO]("#app", node)

      element = document.getElementById("strings")
      _       = element.innerHTML shouldBe ""

      _  = sendEvent(element, "mousedown")
      _ <- IO.sleep(0.1.seconds)
      _  = element.innerHTML shouldBe "yes"

      _  = sendEvent(element, "mouseup")
      _ <- IO.sleep(0.1.seconds)
      _  = element.innerHTML shouldBe "no"
    } yield succeed
  }

  it should "report exception when rendering with RenderConfig.showError" in {

    case class MyException(value: String) extends Throwable {
      override def toString() = value
    }

    val ioException         = MyException("io")
    val observableException = MyException("observable")

    val node = div(
      idAttr := "strings",
      "hallo: ",
      IO.raiseError[String](ioException),
      Observable.raiseError[String](observableException),
    )

    var errors = List.empty[Throwable]
    val cancelable = OutwatchTracing.error.unsafeForeach { throwable =>
      errors = throwable :: errors
    }

    for {
      _      <- Outwatch.renderInto[IO]("#app", node, RenderConfig.showError)
      element = document.getElementById("strings")

      _ = errors shouldBe List(ioException, observableException).reverse

      _ =
        element.innerHTML shouldBe """hallo: <div style="background-color: rgb(253, 242, 245); color: rgb(215, 0, 34); padding: 5px; display: inline-block;">ERROR: io</div><div style="background-color: rgb(253, 242, 245); color: rgb(215, 0, 34); padding: 5px; display: inline-block;">ERROR: observable</div>"""

      _ = cancelable.unsafeCancel()
    } yield succeed
  }

  it should "not report exception when rendering with RenderConfig.ignoreError" in {

    case class MyException(value: String) extends Throwable {
      override def toString() = value
    }

    val ioException         = MyException("io")
    val observableException = MyException("observable")

    val node = div(
      idAttr := "strings",
      "hallo: ",
      IO.raiseError[String](ioException),
      Observable.raiseError[String](observableException),
    )

    var errors = List.empty[Throwable]
    val cancelable = OutwatchTracing.error.unsafeForeach { throwable =>
      errors = throwable :: errors
    }

    for {
      _      <- Outwatch.renderInto[IO]("#app", node, RenderConfig.ignoreError)
      element = document.getElementById("strings")

      _ = errors shouldBe List(ioException, observableException).reverse

      _ = element.innerHTML shouldBe """hallo: """

      _ = cancelable.unsafeCancel()
    } yield succeed
  }

  it should "configure modifier with different RenderConfig" in {

    val outerRenderConfig = RenderConfig(error => div(s"outer: $error"))

    val innerRenderConfig = RenderConfig(error => div(s"inner: $error"))

    case class MyException(value: String) extends Throwable {
      override def toString() = value
    }

    val innerException = MyException("inner")
    val outerException = MyException("outer")

    val node = div(
      idAttr := "strings",
      VMod.raiseError(outerException),
      VMod.configured(VMod.raiseError(innerException))(_ => innerRenderConfig),
    )

    var errors = List.empty[Throwable]
    val cancelable = OutwatchTracing.error.unsafeForeach { throwable =>
      errors = throwable :: errors
    }

    for {
      _      <- Outwatch.renderInto[IO]("#app", node, outerRenderConfig)
      element = document.getElementById("strings")

      _ = errors shouldBe List(outerException, innerException).reverse

      _ = element.innerHTML shouldBe """<div>outer: outer</div><div>inner: inner</div>"""

      _ = cancelable.unsafeCancel()
    } yield succeed
  }

  "Events while patching" should "fire for the correct dom node" in {

    val otherDiv       = Subject.replayLatest[VNode]()
    var insertedFirst  = 0
    var insertedSecond = 0
    var mountedFirst   = 0
    var mountedSecond  = 0

    val node = div(
      otherDiv.map(
        _(
          onSnabbdomInsert doAction { insertedFirst += 1 },
          onDomMount doAction { mountedFirst += 1 },
        ),
      ),
      div(
        onSnabbdomInsert doAction { insertedSecond += 1 },
        onDomMount doAction { mountedSecond += 1 },
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      _ = insertedFirst shouldBe 0
      _ = mountedFirst shouldBe 0
      _ = insertedSecond shouldBe 1
      _ = mountedSecond shouldBe 1

      _ <- otherDiv.onNextIO(div()) *> IO.cede

      _ = insertedFirst shouldBe 1
      _ = mountedFirst shouldBe 1
      _ = insertedSecond shouldBe 1
      _ = mountedSecond shouldBe 1

      _ <- otherDiv.onNextIO(div("hallo")) *> IO.cede

      _ = insertedFirst shouldBe 1
      _ = mountedFirst shouldBe 2
      _ = insertedSecond shouldBe 1
      _ = mountedSecond shouldBe 1
    } yield succeed
  }

  it should "fire for the correct dom node 2" in {
    // in this case, without keys, snabbdom patches the second node

    val otherDiv       = Subject.behavior[Option[VNode]](None)
    var insertedFirst  = 0
    var insertedSecond = 0
    var mountedFirst   = 0
    var mountedSecond  = 0

    val node = div(
      idAttr := "Foo",
      otherDiv.map(otherDiv =>
        div(
          otherDiv.map(
            _(
              onSnabbdomInsert doAction { insertedFirst += 1 },
              onDomMount doAction { mountedFirst += 1 },
            ),
          ),
          div(
            onSnabbdomInsert doAction { insertedSecond += 1 },
            onDomMount doAction { mountedSecond += 1 },
          ),
        ),
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      _ = insertedFirst shouldBe 0
      _ = mountedFirst shouldBe 0
      _ = insertedSecond shouldBe 1
      _ = mountedSecond shouldBe 1

      _ <- otherDiv.onNextIO(Some(div())) *> IO.cede

      _ = insertedFirst shouldBe 0
      _ = mountedFirst shouldBe 1
      _ = insertedSecond shouldBe 2
      _ = mountedSecond shouldBe 2

      _ <- otherDiv.onNextIO(Some(div("hallo"))) *> IO.cede

      _ = insertedFirst shouldBe 0
      _ = mountedFirst shouldBe 2
      _ = insertedSecond shouldBe 2
      _ = mountedSecond shouldBe 3
    } yield succeed
  }

  "EmitterBuilder" should "be referential transparent" in {
    val emitted1 = mutable.ArrayBuffer[Int]()
    val emitted2 = mutable.ArrayBuffer[Int]()

    val subject1 = Subject.replayLatest[Int]()
    val subject2 = Subject.replayLatest[Int]()

    def newEmitter() = onClick.as(0).transform(s => Observable.merge(s, subject1)).foreach(emitted1 += _)
    val emitter      = onClick.as(0).transform(s => Observable.merge(s, subject2)).foreach(emitted2 += _)

    val node = div(
      div(idAttr := "click1", newEmitter(), newEmitter()),
      div(idAttr := "click2", emitter, emitter),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      _ = emitted1.toList shouldBe List()
      _ = emitted2.toList shouldBe List()

      _ <- IO(sendEvent(document.getElementById("click1"), "click"))

      _ = emitted1.toList shouldBe List(0, 0)
      _ = emitted2.toList shouldBe List()

      _ <- IO(sendEvent(document.getElementById("click2"), "click"))

      _ = emitted1.toList shouldBe List(0, 0)
      _ = emitted2.toList shouldBe List(0, 0)

      _ <- subject1.onNextIO(1)

      _ = emitted1.toList shouldBe List(0, 0, 1, 1)
      _ = emitted2.toList shouldBe List(0, 0)

      _ <- subject2.onNextIO(1)

      _ = emitted1.toList shouldBe List(0, 0, 1, 1)
      _ = emitted2.toList shouldBe List(0, 0, 1, 1)
    } yield succeed
  }

  "Rx component" should "work" in Owned(SyncIO {

    var liveCounter = 0

    val variable1 = Var(1)
    val variable2 = Var("hallo")

    val modifier = Rx {
      liveCounter += 1
      VMod(s"${variable1()}. ${variable2()}")
    }

    val node = div(
      idAttr := "test",
      modifier,
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element <- IO(document.getElementById("test"))

      _ = element.innerHTML shouldBe "1. hallo"
      _ = liveCounter shouldBe 1

      _  = variable1.set(2)
      _ <- IO.cede

      _ = element.innerHTML shouldBe "2. hallo"
      _ = liveCounter shouldBe 2

      _  = variable2.set("du")
      _ <- IO.cede

      _ = element.innerHTML shouldBe "2. du"
      _ = liveCounter shouldBe 3
    } yield succeed
  }).unsafeRunSync()

  "Nested rx component" should "work" in {
    case class Modal(val content: String, val size: Option[Int])
    Owned(SyncIO {

      val modal: Var[Modal] = Var(Modal("hallo", None))

      val content = modal.map(_.content)

      val size = modal.map(_.size match {
        case Some(w) => VMod(w)
        case None    => VMod.empty
      })

      val node = div(
        idAttr := "test",
        content.map(str => div(str, size)),
      )

      for {
        _ <- Outwatch.renderInto[IO]("#app", node)

        element <- IO(document.getElementById("test"))

        _ = element.innerHTML shouldBe """<div>hallo</div>"""

        _  = modal.set(Modal("hallo", Some(1)))
        _ <- IO.cede

        _ = element.innerHTML shouldBe """<div>hallo1</div>"""

        _  = modal.set(Modal("huhu", None))
        _ <- IO.cede

        _ = element.innerHTML shouldBe """<div>huhu</div>"""
      } yield succeed
    }).unsafeRunSync()
  }

  it should "work complex" in {
    case class Modal(content: String, size: Option[Int])

    val currentModal: Var[Option[Modal]] = Var(None)

    val node = Owned(
      div(
        idAttr := "test",
        currentModal.sequence.map {
          _.map { modal =>
            val content = modal.map(_.content)

            val size = modal.map(_.size match {
              case Some(w) => VMod(width := s"${w}px")
              // case None    => VMod("was")
              case None => VMod.empty
            })

            content.map(str => div(str, size))
          }
        },
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element <- IO(document.getElementById("test"))

      _ = element.innerHTML shouldBe ""

      _  = currentModal.set(Some(Modal("hallo", None)))
      _ <- IO.cede

      _ = element.innerHTML shouldBe """<div>hallo</div>"""

      _  = currentModal.set(Some(Modal("hallo", Some(1))))
      _ <- IO.cede

      _ = element.innerHTML shouldBe """<div style="width: 1px;">hallo</div>"""

      _  = currentModal.set(Some(Modal("hallo2", None)))
      _ <- IO.cede

      _ = element.innerHTML shouldBe """<div style="">hallo2</div>"""
    } yield succeed
  }

  it should "render types that have a `Render` and a `Show` implementation using `Render`" in {
    case class Foo(bar: String)
    object Foo {
      implicit val renderFoo: Render[Foo] = (foo: Foo) => span(foo.bar)
      implicit val showFoo: Show[Foo]     = (foo: Foo) => foo.bar
    }

    val node = div(
      idAttr := "test",
      Foo("foo"),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      element <- IO(document.getElementById("test"))

      _ = element.innerHTML shouldBe """<span>foo</span>"""
    } yield succeed
  }

  "Component" should "render unit observables and effects" in {
    case class MyObservable[T](observable: Observable[T])
    object MyObservable {
      implicit object source extends Source[MyObservable] {
        def unsafeSubscribe[A](source: MyObservable[A])(sink: Observer[A]): Cancelable =
          source.observable.unsafeSubscribe(sink)
      }
    }

    var obsSubscribes   = 0
    var obsUnsubscribes = 0
    val obs = Observable.unit.tapSubscribe { () =>
      obsSubscribes += 1
      Cancelable { () =>
        obsUnsubscribes += 1
      }
    }

    val subject = Subject.behavior[Boolean](true)

    val node = div(
      idAttr := "test",
      subject.map {
        case true =>
          VMod(
            "hallo",
            obs,
            MyObservable(obs),
            obs.headIO,
          )
        case false =>
          VMod.empty
      },
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _ <- IO.cede

      element <- IO(document.getElementById("test"))

      _ = element.innerHTML shouldBe "hallo"

      _ = obsSubscribes shouldBe 3
      _ = obsUnsubscribes shouldBe 1

      _ <- subject.onNextIO(false)
      _ <- IO.cede

      _ = element.innerHTML shouldBe ""

      _ = obsSubscribes shouldBe 3
      _ = obsUnsubscribes shouldBe 3
    } yield succeed
  }
}
