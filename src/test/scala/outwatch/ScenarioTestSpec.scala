package outwatch

import cats.effect.IO
import org.scalajs.dom.{html, _}
import monix.reactive.{Observable, Observer}
import org.scalatest.Assertion
import outwatch.Deprecated.IgnoreWarnings.initEvent
import outwatch.dom._
import outwatch.dom.dsl._
import outwatch.util.Store

class ScenarioTestSpec extends JSDomAsyncSpec {

  def getMinus: Element   = document.getElementById("minus")
  def getPlus: Element    = document.getElementById("plus")
  def getCounter: Element = document.getElementById("counter")

  "A simple counter application" should "work as intended" in {

    val test: IO[Assertion] = for {
       handlePlus <- Handler.create[MouseEvent]
      handleMinus <- Handler.create[MouseEvent]
          plusOne = handlePlus.map(_ => 1)
         minusOne = handleMinus.map(_ => -1)
            count = Observable.merge(plusOne, minusOne).scan(0)(_ + _).startWith(Seq(0))

             node = div(div(
                        button(id := "plus", "+", onClick --> handlePlus),
                        button(id := "minus", "-", onClick --> handleMinus),
                        span(id:="counter", count)
                    ))
                r <- IO {
                      val root = document.createElement("div")
                      document.body.appendChild(root)
                      root
                    }
                _ <- OutWatch.renderInto(r, node)
            event <- IO {
                      val event = document.createEvent("Events")
                      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
                      event
                    }
                _ <- IO {
                      getCounter.innerHTML shouldBe 0.toString
                      getMinus.dispatchEvent(event)
                      getCounter.innerHTML shouldBe (-1).toString
                    }
                as <- IO {
                      (0 to 10).map { _ =>
                        getPlus.dispatchEvent(event)
                        getCounter.innerHTML
                     }}
    } yield {
      as should contain theSameElementsInOrderAs (0 to 10).map(_.toString)
    }

    test

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

    val node: IO[VNode] = for {
      store <- Store.create[State, Action](0, reduce _)
    } yield div(
        div(
          button(id := "plus", "+", onClick(Plus) --> store),
          button(id := "minus", "-", onClick(Minus) --> store),
          span(id:="counter", store)
        )
      )

    val test: IO[Assertion] = for {
      r <- IO {
            val root = document.createElement("div")
            document.body.appendChild(root)
            root
          }
      node <- node
      _ <- OutWatch.renderInto(r, node)
      e <- IO {
            val event = document.createEvent("Events")
            initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
            event
          }
      _ <- IO {
            getCounter.innerHTML shouldBe 0.toString
            getMinus.dispatchEvent(e)
            getCounter.innerHTML shouldBe (-1).toString
          }
      as <- IO {
            (0 to 10).map { _ =>
              getPlus.dispatchEvent(e)
              getCounter.innerHTML
           }}

    } yield {
      as should contain theSameElementsInOrderAs (0 to 10).map(_.toString)
    }

    test

  }

  "A simple name application" should "work as intended" in {

    val greetStart = "Hello ,"
    val name1 = "Luka"
    val name2 = "Peter"

    def getGreeting(evt: Event, name: String): IO[String] = IO {
      document.getElementById("input").asInstanceOf[html.Input].value = name
      document.getElementById("input").dispatchEvent(evt)
      document.getElementById("greeting").innerHTML
    }

    def assertGreeting(greeting: String, name: String): Assertion =
      assert(greeting == greetStart + name)

    val node = Handler.create[String].map { nameHandler =>
      div(
        label("Name:"),
        input(id := "input", tpe := "text", onInput.value --> nameHandler),
        hr(),
        h1(id := "greeting", greetStart, nameHandler)
      )
    }

    val test: IO[Assertion] = for {
          r <- IO {
                val root = document.createElement("div")
                document.body.appendChild(root)
                root
              }
      node  <- node
          _ <- OutWatch.renderInto(r, node)
      event <- IO {
                val evt = document.createEvent("HTMLEvents")
                initEvent(evt)("input", canBubbleArg = false, cancelableArg = true)
                evt
              }
          g1 <- getGreeting(event, name1)
          g2 <- getGreeting(event, name2)
           _ = assertGreeting(g1, name1)
           _ = assertGreeting(g2, name2)

    } yield succeed

    test
  }

  "A component" should "be referential transparent" in {

    val createDiv = IO(document.createElement("div"))
    def getButton(element: Element) =
      element.getElementsByTagName("button").item(0)

    def component(): IO[VNode] = {
      Handler.create[String].map { handler =>
        div(
          button(onClick("clicked") --> handler),
          div(cls := "label", handler)
        )
      }
    }

    val comp = component()
    val component1 = div(component(), component())
    val component2 = div(comp, comp)

    val test: IO[Assertion] = for {
      evt <- IO {
        val clickEvt = document.createEvent("Events")
        initEvent(clickEvt)("click", canBubbleArg = true, cancelableArg = true)
        clickEvt
      }
      e1 <- createDiv
       _ <- OutWatch.renderInto(e1, component1)
      e2 <- createDiv
       _ <- OutWatch.renderInto(e2, component2)
       _ <- IO {
             getButton(e1).dispatchEvent(evt)
             getButton(e2).dispatchEvent(evt)
           }
       _ = e1.innerHTML shouldBe e1.innerHTML

    } yield succeed

    test
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

      confirm = Observable(enterPressed, clickStream).merge
        .withLatestFrom(textFieldStream)((_, input) => input)

    } yield div(
        emitter(confirm) --> outputStream,
        label(labelText),
        input(id:= "input", tpe := "text", onInput.value --> textFieldStream, onKeyUp --> keyStream),
        button(id := "submit", onClick --> clickStream, disabled <-- buttonDisabled, "Submit")
      )



    def addToList(todo: String) = {
      list: Vector[String] => list :+ todo
    }

    def removeFromList(todo: String) = {
      list: Vector[String] => list.filterNot(_ == todo)
    }

    val vtree = for {
      inputHandler <- Handler.create[String]
      deleteHandler <- Handler.create[String]

      adds = inputHandler
        .map(addToList)

      deletes = deleteHandler
        .map(removeFromList)

      state = Observable(adds, deletes).merge
        .scan(Vector[String]())((state, modify) => modify(state))
        .map(_.map(n => TodoComponent(n, deleteHandler)))
      textFieldComponent = TextFieldComponent("Todo: ", inputHandler)

    } yield div(
        textFieldComponent,
        ul(id:= "list", state)
      )

    val test: IO[Assertion] = for {

      root <- IO {
        val root = document.createElement("div")
        document.body.appendChild(root)
        root
      }

      vtree <- vtree
      _ <- OutWatch.renderInto(root, vtree)

      inputEvt <- IO {
        val inputEvt = document.createEvent("HTMLEvents")
        initEvent(inputEvt)("input", canBubbleArg = false, cancelableArg = true)
        inputEvt
      }

      clickEvt <- IO {
        val clickEvt = document.createEvent("Events")
        initEvent(clickEvt)("click", canBubbleArg = true, cancelableArg = true)
        clickEvt
      }

         inputE <- IO(document.getElementById("input").asInstanceOf[html.Input])
      submitBtn <- IO(document.getElementById("submit"))
           list <- IO(document.getElementById("list"))
              _ = list.childElementCount shouldBe 0

      t <- IO {
        val todo = "fold laundry"
        inputE.value = todo
        inputE.dispatchEvent(inputEvt)
        submitBtn.dispatchEvent(clickEvt)
        todo
      }
      _ = list.childElementCount shouldBe 1

      t2 <- IO {
        val todo2 = "wash dishes"
        inputE.value = todo2
        inputE.dispatchEvent(inputEvt)
        submitBtn.dispatchEvent(clickEvt)
        todo2
      }
      _ = list.childElementCount shouldBe 2

      t3 <- IO {
        val todo3 = "clean windows"
        inputE.value = todo3
        inputE.dispatchEvent(inputEvt)
        submitBtn.dispatchEvent(clickEvt)
        todo3
      }
      _ = list.childElementCount shouldBe 3

      _ <- IO(document.getElementById(t2).dispatchEvent(clickEvt))
      _ = list.childElementCount shouldBe 2

      _ <- IO(document.getElementById(t3).dispatchEvent(clickEvt))
      _ = list.childElementCount shouldBe 1

      _ <- IO(document.getElementById(t).dispatchEvent(clickEvt))
      _ = list.childElementCount shouldBe 0

    } yield succeed

    test

  }
}
