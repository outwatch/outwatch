package outwatch

import org.scalatest.BeforeAndAfterEach
import rxscalajs.Subject
import org.scalajs.dom.raw.{HTMLInputElement, MouseEvent}
import org.scalajs.dom._
import outwatch.dom.helpers.DomUtils

class DomEventSpec extends UnitSpec with BeforeAndAfterEach {

  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  "EventStreams" should "emit and receive events correctly" in {
    import outwatch.dom._
    val observable = Subject[MouseEvent]
    val buttonDisabled = observable.mapTo(true).startWith(false)
    val vtree = div(id :="click", click --> observable,
      button(id := "btn", disabled <-- buttonDisabled)
    )

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, vtree)

    document.getElementById("btn").hasAttribute("disabled") shouldBe false

    val event = document.createEvent("Events")
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("btn").hasAttribute("disabled") shouldBe true
  }

  it should "be converted to a generic emitter correctly" in {
    import outwatch.dom._
    val observable = Subject[String]
    val message = "Hello!"
    val vtree = div(id :="click", click(message) --> observable,
      span(id := "child", child <-- observable)
    )

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, vtree)

    document.getElementById("child").innerHTML shouldBe ""

    val event = document.createEvent("Events")
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe message

    //dispatch another event
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe message
  }

  it should "be converted to a generic stream emitter correctly" in {
    import outwatch.dom._
    val stream = Subject[String]
    val messages = Subject[String]
    val vtree = div(id :="click", click(messages) --> stream,
      span(id := "child", child <-- stream)
    )

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, vtree)

    document.getElementById("child").innerHTML shouldBe ""

    val firstMessage = "First"
    messages.next(firstMessage)

    val event = document.createEvent("Events")
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe firstMessage

    //dispatch another event
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe firstMessage

    val secondMessage = "Second"
    messages.next(secondMessage)

    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe secondMessage
  }

  it should "be able to set the value of a text field" in {
    import outwatch.dom._

    val values = Subject[String]

    val vtree = input(id:= "input", outwatch.dom.value <-- values)
    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, vtree)
    val patched = document.getElementById("input").asInstanceOf[HTMLInputElement]

    patched.value shouldBe ""

    val value1 = "Hello"
    values.next(value1)

    patched.value shouldBe value1

    val value2 = "World"
    values.next(value2)

    patched.value shouldBe value2

    values.next("")

    patched.value shouldBe ""
  }

  it should "be bindable to a list of children" in {
    import outwatch.dom._


    val state = Subject[Seq[VNode]]


    val vtree = div(
      ul(id:= "list", children <-- state)
    )

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, vtree)
    val list = document.getElementById("list")

    list.childElementCount shouldBe 0

    val first = "Test"

    state.next(Seq(span(first)))

    list.childElementCount shouldBe 1
    list.innerHTML.contains(first) shouldBe true

    val second = "Hello"
    state.next(Seq(span(first), span(second)))

    list.childElementCount shouldBe 2
    list.innerHTML.contains(first) shouldBe true
    list.innerHTML.contains(second) shouldBe true

    val third = "World"

    state.next(Seq(span(first), span(second), span(third)))

    list.childElementCount shouldBe 3
    list.innerHTML.contains(first) shouldBe true
    list.innerHTML.contains(second) shouldBe true
    list.innerHTML.contains(third) shouldBe true

    state.next(Seq(span(first), span(third)))

    list.childElementCount shouldBe 2
    list.innerHTML.contains(first) shouldBe true
    list.innerHTML.contains(third) shouldBe true
  }

}
