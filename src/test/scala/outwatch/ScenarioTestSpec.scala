package outwatch

import org.scalajs.dom.{html, _}
import monix.reactive.{Observable, Observer}
import outwatch.Deprecated.IgnoreWarnings.initEvent
import outwatch.dom._
import outwatch.dom.dsl._
import outwatch.util.Store

class ScenarioTestSpec extends JSDomSpec {

  "A simple counter application" should "work as intended" in {

    val node = for {
      handlePlus <- Handler.create[MouseEvent]
      plusOne = handlePlus.map(_ => 1)

      handleMinus <- Handler.create[MouseEvent]
      minusOne = handleMinus.map(_ => -1)

      count = Observable.merge(plusOne, minusOne).scan(0)(_ + _).startWith(Seq(0))

    } yield div(
        div(
          button(id := "plus", "+", onClick --> handlePlus),
          button(id := "minus", "-", onClick --> handleMinus),
          span(id:="counter", count)
        )
      )

    val root = document.createElement("div")
    document.body.appendChild(root)

    node.flatMap(OutWatch.renderInto(root, _)).unsafeRunSync()

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)

    document.getElementById("counter").innerHTML shouldBe 0.toString

    document.getElementById("minus").dispatchEvent(event)

    document.getElementById("counter").innerHTML shouldBe (-1).toString

    for (i <- 0 to 10) {
      document.getElementById("plus").dispatchEvent(event)
      document.getElementById("counter").innerHTML shouldBe i.toString
    }
  }


  "A simple counter application that uses Store" should "work as intended" in {

    sealed trait Action
    case object Plus extends Action
    case object Minus extends Action

    type State = Int

    def reduce(state: State, action: Action): State = action match {
      case Plus => state + 1
      case Minus => state - 1
    }

    val node = for {
      store <- Store.create[State, Action](0, reduce _)
    } yield div(
        div(
          button(id := "plus", "+", onClick(Plus) --> store),
          button(id := "minus", "-", onClick(Minus) --> store),
          span(id:="counter", store)
        )
      )

    val root = document.createElement("div")
    document.body.appendChild(root)

    node.flatMap(OutWatch.renderInto(root, _)).unsafeRunSync()

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)

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

    val node = Handler.create[String].map { nameHandler =>
      div(
        label("Name:"),
        input(id := "input", tpe := "text", onInput.value --> nameHandler),
        hr(),
        h1(id := "greeting", greetStart, nameHandler)
      )
    }

    val root = document.createElement("div")
    document.body.appendChild(root)

    node.flatMap(OutWatch.renderInto(root, _)).unsafeRunSync()


    val evt = document.createEvent("HTMLEvents")
    initEvent(evt)("input", false, true)
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
      Handler.create[String].map { handler =>
        div(
          button(onClick("clicked") --> handler),
          div(cls := "label", handler)
        )
      }
    }

    val clickEvt = document.createEvent("Events")
    initEvent(clickEvt)("click", true, true)

    val comp = component()

    val component1 = div(component(), component())
    val component2 = div(comp, comp)

    val element1 = document.createElement("div")
    OutWatch.renderInto(element1, component1).unsafeRunSync()

    val element2 = document.createElement("div")
    OutWatch.renderInto(element2, component2).unsafeRunSync()

    element1.innerHTML shouldBe element2.innerHTML

    element1.getElementsByTagName("button").item(0).dispatchEvent(clickEvt)

    element1.innerHTML should not equal element2.innerHTML

    element2.getElementsByTagName("button").item(0).dispatchEvent(clickEvt)

    element1.innerHTML shouldBe element2.innerHTML
  }

  "A todo application" should "work with components" in {

    def TodoComponent(title: String, deleteStream: Observer[String]) =
      li(
        span(title),
        button(id:= title, onClick(title) --> deleteStream, "Delete")
      )

    def TextFieldComponent(labelText: String, outputStream: Observer[String]) = for {

      textFieldStream <- Handler.create[String]
      clickStream <- Handler.create[MouseEvent]
      keyStream <- Handler.create[KeyboardEvent]

      buttonDisabled = textFieldStream
        .map(_.length < 2)
        .startWith(Seq(true))

      enterPressed = keyStream
        .filter(_.key == "Enter")

      confirm = Observable.merge(enterPressed, clickStream)
        .withLatestFrom(textFieldStream)((_, input) => input)

    } yield div(
        managed(outputStream <-- confirm),
        label(labelText),
        input(id:= "input", tpe := "text", onInput.value --> textFieldStream, onKeyUp --> keyStream),
        button(id := "submit", onClick --> clickStream, disabled <-- buttonDisabled, "Submit")
      )



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

      state = Observable.merge(adds, deletes)
        .scan(Vector[String]())((state, modify) => modify(state))
        .map(_.map(n => TodoComponent(n, deleteHandler)))
      textFieldComponent = TextFieldComponent("Todo: ", inputHandler)

    } yield div(
        textFieldComponent,
        ul(id:= "list", state)
      )

    val root = document.createElement("div")
    document.body.appendChild(root)

    vtree.flatMap(OutWatch.renderInto(root, _)).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    initEvent(inputEvt)("input", false, true)

    val clickEvt = document.createEvent("Events")
    initEvent(clickEvt)("click", true, true)

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
