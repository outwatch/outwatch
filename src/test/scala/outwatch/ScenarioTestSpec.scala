package outwatch

import org.scalajs.dom._
import org.scalajs.dom.html
import org.scalatest.BeforeAndAfterEach
import outwatch.dom._
import outwatch.dom.helpers.DomUtils

class ScenarioTestSpec extends UnitSpec with BeforeAndAfterEach {
  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  override def beforeEach(): Unit = {
    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
    ()
  }

  "A simple counter application" should "work as intended" in {
    val node = for {
      handlePlus <- Handler.mouseEvents
      plusOne = handlePlus.mapTo(1)

      handleMinus <- Handler.mouseEvents
      minusOne = handleMinus.mapTo(-1)

      count = plusOne.merge(minusOne).scan(0)(_ + _).startWith(0)

      div <- div(
        div(
          button(id := "plus", "+", onClick --> handlePlus),
          button(id := "minus", "-", onClick --> handleMinus),
          span(id:="counter",child <-- count)
        )
      )
    } yield div

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, node).unsafeRunSync()

    val event = document.createEvent("Events")
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
    val greetStart = "Hello ,"

    val node = Handler.create[String].flatMap { nameHandler =>
      div(
        label("Name:"),
        input(id := "input", tpe := "text", onInputString --> nameHandler),
        hr(),
        h1(id := "greeting", greetStart, child <-- nameHandler)
      )
    }

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, node).unsafeRunSync()


    val evt = document.createEvent("HTMLEvents")
    evt.initEvent("input", false, true)
    val name = "Luka"

    document.getElementById("input").asInstanceOf[html.Input].value = name
    document.getElementById("input").dispatchEvent(evt)

    document.getElementById("greeting").innerHTML shouldBe greetStart + name

    val name2 = "Peter"

    document.getElementById("input").asInstanceOf[html.Input].value = name2
    document.getElementById("input").dispatchEvent(evt)

    document.getElementById("greeting").innerHTML shouldBe greetStart + name2
  }

  "A component" should "be referential transparent" in {

    def component() = {
      Handler.create[String].flatMap { handler =>
        div(
          button(onClick("clicked") --> handler),
          div(cls := "label", child <-- handler)
        )
      }
    }

    val clickEvt = document.createEvent("Events")
    clickEvt.initEvent("click", true, true)

    val comp = component()

    val component1 = div(component(), component())
    val component2 = div(comp, comp)

    val element1 = document.createElement("div")
    DomUtils.render(element1, component1).unsafeRunSync()

    val element2 = document.createElement("div")
    DomUtils.render(element2, component2).unsafeRunSync()

    element1.getElementsByTagName("button").item(0).dispatchEvent(clickEvt)

    element2.getElementsByTagName("button").item(0).dispatchEvent(clickEvt)

    element1.innerHTML shouldBe element2.innerHTML
  }

  "A todo application" should "work with components" in {

    def TodoComponent(title: String, deleteStream: Sink[String]) =
      li(
        span(title),
        button(id:= title, onClick(title) --> deleteStream, "Delete")
      )

    def TextFieldComponent(labelText: String, outputStream: Sink[String]) = for {

      textFieldStream <- Handler.create[String]
      clickStream <- Handler.mouseEvents
      keyStream <- Handler.keyboardEvents

      buttonDisabled = textFieldStream
        .map(_.length < 2)
        .startWith(true)

      enterPressed = keyStream
        .filter(_.key == "Enter")

      confirm = enterPressed.merge(clickStream)
        .withLatestFromWith(textFieldStream)((_, input) => input)

      _ <- (outputStream <-- confirm)

      div <- div(
        label(labelText),
        input(id:= "input", tpe := "text", onInputString --> textFieldStream, onKeyUp --> keyStream),
        button(id := "submit", onClick --> clickStream, disabled <-- buttonDisabled, "Submit")
      )
    } yield div



    def addToList(todo: String) = {
      (list: Vector[String]) => list :+ todo
    }

    def removeFromList(todo: String) = {
      (list: Vector[String]) => list.filterNot(_ == todo)
    }

    val vtree = for {
      inputHandler <- Handler.create[String]
      deleteHandler <- Handler.create[String]

      adds = inputHandler
        .map(addToList)

      deletes = deleteHandler
        .map(removeFromList)

      state = adds.merge(deletes)
        .scan(Vector[String]())((state, modify) => modify(state))
        .map(_.map(n => TodoComponent(n, deleteHandler)))
      textFieldComponent = TextFieldComponent("Todo: ", inputHandler)

      div <- div(
        textFieldComponent,
        ul(id:= "list", children <-- state)
      )
    } yield div

    val root = document.createElement("div")
    document.body.appendChild(root)

    DomUtils.render(root, vtree).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    inputEvt.initEvent("input", false, true)

    val clickEvt = document.createEvent("Events")
    clickEvt.initEvent("click", true, true)

    val inputElement = document.getElementById("input").asInstanceOf[html.Input]
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
