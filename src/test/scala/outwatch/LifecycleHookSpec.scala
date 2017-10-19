package outwatch

import cats.effect.IO
import org.scalajs.dom._
import org.scalatest.BeforeAndAfterEach
import outwatch.dom._
import outwatch.dom.helpers.{DomUtils, StyleBuilder}
import org.scalajs.dom.raw.HTMLElement
import rxscalajs.{Observable, Subject}

class LifecycleHookSpec extends UnitSpec with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
    ()
  }

  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  "Insertion hooks" should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO { switch = true })

    val node = div(insert --> sink)

    switch shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  "Insertion hooks" should "be called correctly on merged nodes" in {
    var switch = false
    val sink = Sink.create((_: Element) => IO(switch = true))
    var switch2 = false
    val sink2 = Sink.create((_: Element) => IO(switch2 = true))

    val node = div(insert --> sink)(insert --> sink2)

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true

  }

  "Destruction hooks" should "be called correctly on merged nodes" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO(switch = true))
    var switch2 = false
    val sink2 = Sink.create((_: Element) => IO(switch2 = true))

    val node = div(child <-- Observable.of(span(destroy --> sink)(destroy --> sink2), "Hasdasd"))

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true

  }

  "Update hooks" should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create((_: (Element, Element)) => IO(switch1 = true))
    var switch2 = false
    val sink2 = Sink.create((_: (Element, Element)) => IO(switch2 = true))

    val message = Subject[String]()
    val node = div(child <-- message, update --> sink1)(update --> sink2)

    OutWatch.render("#app", node).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.next("wursi")
    switch1 shouldBe true
    switch2 shouldBe true
  }

  "Destruction hooks" should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO { switch = true })

    val node = div(child <-- Observable.of(span(destroy --> sink), "Hasdasd"))

    switch shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  "Update hooks" should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: (Element, Element)) => IO { switch = true })

    val node = div(child <-- Observable.of(span(update --> sink, "Hello"), span(update --> sink, "Hey")))

    switch shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true

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
    val vNode = div(new StyleBuilder("color") <-- messages)(new StyleBuilder("color") <-- otherMessages)

    val node = document.createElement("div")
    document.body.appendChild(node)
    DomUtils.render(node, vNode).unsafeRunSync()

    otherMessages.next("red")
    node.children(0).asInstanceOf[HTMLElement].style.color shouldBe "red"

    messages.next("blue") // should be ignored
    node.children(0).asInstanceOf[HTMLElement].style.color shouldBe "red"

    otherMessages.next("green")
    node.children(0).asInstanceOf[HTMLElement].style.color shouldBe "green"
  }

}
