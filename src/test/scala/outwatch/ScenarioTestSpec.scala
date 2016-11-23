package outwatch

import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement
import org.scalatest.BeforeAndAfterEach
import rxscalajs.{Observable, Subject}
import outwatch.dom.helpers.DomUtils

class ScenarioTestSpec extends UnitSpec with BeforeAndAfterEach {
  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  val fixture = new {
    val event = document.createEvent("Events")
  }

  "A simple counter application" should "work as intended" in {
    import outwatch.dom._

    val handlePlus = createMouseHandler
    val plusOne$ = handlePlus.mapTo(1)

    val handleMinus = createMouseHandler
    val minusOne$ = handleMinus.mapTo(-1)

    val count$ = plusOne$.merge(minusOne$).scan(0)(_ + _).startWith(0)

    val node = div(
      div(
        button(id := "plus", "+", click --> handlePlus),
        button(id := "minus", "-", click --> handleMinus),
        span(id:="counter",child <-- count$)
      )
    )

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, node)

    val event = fixture.event
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)

    document.getElementById("counter").innerHTML shouldBe 0.toString

    document.getElementById("minus").dispatchEvent(event)

    document.getElementById("counter").innerHTML shouldBe (-1).toString

    for (i <- 0 to 10) {
      document.getElementById("plus").dispatchEvent(event)
      document.getElementById("counter").innerHTML shouldBe i.toString
    }

  }

  "A simple name application" should "work as intended" in {
    import outwatch.dom._

    val nameHandler = createStringHandler

    val greetStart = "Hello ,"

    val node = div(
      label("Name:"),
      input(id := "input", inputType := "text", inputString --> nameHandler),
      hr(),
      h1(id :="greeting", greetStart, child <-- nameHandler)
    )
    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, node)


    val evt = document.createEvent("HTMLEvents")
    evt.initEvent("input", false, true)
    val name = "Luka"

    document.getElementById("input").asInstanceOf[HTMLInputElement].value = name
    document.getElementById("input").dispatchEvent(evt)

    document.getElementById("greeting").innerHTML shouldBe greetStart + name

    val name2 = "Peter"

    document.getElementById("input").asInstanceOf[HTMLInputElement].value = name2
    document.getElementById("input").dispatchEvent(evt)

    document.getElementById("greeting").innerHTML shouldBe greetStart + name2
  }

  "A todo application" should "work with components" in {
    import outwatch.dom._

    def TodoComponent(title: String, deleteStream: Sink[String]) =
      li(
        span(title),
        button(id:= title, click(title) --> deleteStream, "Delete")
      )

    def TextFieldComponent(labelText: String, outputStream: Sink[String]) = {

      val textFieldStream = createStringHandler
      val clickStream = createMouseHandler
      val keyStream = createKeyboardHandler

      val buttonDisabled = textFieldStream
        .map(_.length < 2)
        .startWith(true)

      val enterPressed = keyStream
        .filter(_.key == "Enter")

      val confirm$ = enterPressed.merge(clickStream)
        .withLatestFromWith(textFieldStream)((_, input) => input)

      outputStream <-- confirm$

      div(
        label(labelText),
        input(id:= "input", inputType := "text", inputString --> textFieldStream, keyup --> keyStream),
        button(id := "submit", click --> clickStream, disabled <-- buttonDisabled, "Submit")
      )
    }

    val inputHandler = createStringHandler
    val deleteHandler = createStringHandler

    def addToList(todo: String) = {
      (list: Vector[String]) => list :+ todo
    }

    def removeFromList(todo: String) = {
      (list: Vector[String]) => list.filterNot(_ == todo)
    }

    val adds = inputHandler
      .map(addToList _)

    val deletes = deleteHandler
      .map(n => (list: Vector[String]) => list.filter(_ != n))

    val state = adds.merge(deletes)
      .scan(Vector[String]())((state, modify) => modify(state))
      .map(_.map(n => TodoComponent(n, deleteHandler)))


    val vtree = div(
      TextFieldComponent("Todo: ", inputHandler),
      ul(id:= "list", children <-- state)
    )

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, vtree)

    val inputEvt = document.createEvent("HTMLEvents")
    inputEvt.initEvent("input", false, true)

    val clickEvt = document.createEvent("Events")
    clickEvt.initEvent("click", true, true)

    val inputElement = document.getElementById("input").asInstanceOf[HTMLInputElement]
    val submitButton = document.getElementById("submit")
    val list = document.getElementById("list")

    list.childElementCount shouldBe 0

    val todo = "fold laundry"
    inputElement.value = todo
    inputElement.dispatchEvent(inputEvt)
    submitButton.dispatchEvent(clickEvt)

    list.childElementCount shouldBe 1

    val todo2 = "wash dishes"
    inputElement.value = todo2
    inputElement.dispatchEvent(inputEvt)
    submitButton.dispatchEvent(clickEvt)

    list.childElementCount shouldBe 2

    val todo3 = "clean windows"
    inputElement.value = todo3
    inputElement.dispatchEvent(inputEvt)
    submitButton.dispatchEvent(clickEvt)

    list.childElementCount shouldBe 3

    document.getElementById(todo2).dispatchEvent(clickEvt)

    list.childElementCount shouldBe 2

    document.getElementById(todo3).dispatchEvent(clickEvt)

    list.childElementCount shouldBe 1

    document.getElementById(todo).dispatchEvent(clickEvt)

    list.childElementCount shouldBe 0

  }
}
