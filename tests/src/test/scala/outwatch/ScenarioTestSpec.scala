package outwatch

import cats.effect.IO
import org.scalajs.dom.{html, _}
import org.scalatest.Assertion
import outwatch.dsl._
import org.scalajs.dom.EventInit
import colibri._
import cats.implicits._

class ScenarioTestSpec extends JSDomAsyncSpec {

  def getMinus: Element   = document.getElementById("minus")
  def getPlus: Element    = document.getElementById("plus")
  def getCounter: Element = document.getElementById("counter")

  "A simple counter application" should "work as intended" in {
    for {
      handlePlus  <- IO(Subject.replayLatest[MouseEvent]())
      handleMinus <- IO(Subject.replayLatest[MouseEvent]())
      plusOne      = handlePlus.map(_ => 1)
      minusOne     = handleMinus.map(_ => -1)
      count        = Observable.merge(plusOne, minusOne).scan(0)(_ + _).startWith(Seq(0))

      node = div(
               div(
                 button(idAttr := "plus", "+", onClick --> handlePlus),
                 button(idAttr := "minus", "-", onClick --> handleMinus),
                 span(idAttr   := "counter", count),
               ),
             )
      r <- IO {
             val root = document.createElement("div")
             document.body.appendChild(root)
             root
           }
      _ <- Outwatch.renderInto[IO](r, node)
      event <- IO {
                 new Event(
                   "click",
                   new EventInit {
                     bubbles = true
                     cancelable = false
                   },
                 )
               }
      _ <- IO {
             getCounter.innerHTML shouldBe 0.toString
             getMinus.dispatchEvent(event)
           }
      _ <- IO.cede
      _ <- IO {
             getCounter.innerHTML shouldBe -1.toString
           }

      as <- (0 to 10).toList.traverse { _ =>
              IO(getPlus.dispatchEvent(event)) *> IO.cede *> IO(getCounter.innerHTML)
            }
    } yield {
      as should contain theSameElementsInOrderAs (0 to 10).map(_.toString)
    }
  }

  "A simple name application" should "work as intended" in {

    val greetStart = "Hello ,"
    val name1      = "Luka"
    val name2      = "Peter"

    def getGreeting(evt: Event, name: String): IO[Unit] = IO {
      document.getElementById("input").asInstanceOf[html.Input].value = name
      document.getElementById("input").dispatchEvent(evt)
      ()
    }

    def assertGreeting(greeting: String, name: String): Assertion =
      assert(greeting == greetStart + name)

    val node = IO(Subject.replayLatest[String]()).map { nameHandler =>
      div(
        label("Name:"),
        input(idAttr := "input", tpe := "text", onInput.value --> nameHandler),
        hr(),
        h1(idAttr := "greeting", greetStart, nameHandler),
      )
    }

    for {
      r <- IO {
             val root = document.createElement("div")
             document.body.appendChild(root)
             root
           }
      node <- node
      _    <- Outwatch.renderInto[IO](r, node)
      event <- IO {
                 new Event(
                   "input",
                   new EventInit {
                     bubbles = false
                     cancelable = true
                   },
                 )
               }
      _  <- getGreeting(event, name1) *> IO.cede
      g1 <- IO(document.getElementById("greeting").innerHTML)
      _  <- getGreeting(event, name2) *> IO.cede
      g2 <- IO(document.getElementById("greeting").innerHTML)
      _   = assertGreeting(g1, name1)
      _   = assertGreeting(g2, name2)

    } yield succeed
  }

  "A component" should "be referential transparent" in {

    val createDiv = IO(document.createElement("div"))
    def getButton(element: Element) =
      element.getElementsByTagName("button").item(0)

    def component(): IO[VNode] = {
      IO(Subject.replayLatest[String]()).map { handler =>
        div(
          button(onClick.as("clicked") --> handler),
          div(cls := "label", handler),
        )
      }
    }

    val comp       = component()
    val component1 = div(component(), component())
    val component2 = div(comp, comp)

    for {
      evt <- IO {
               new Event(
                 "click",
                 new EventInit {
                   bubbles = true
                   cancelable = true
                 },
               )
             }
      e1 <- createDiv
      _  <- Outwatch.renderInto[IO](e1, component1)
      e2 <- createDiv
      _  <- Outwatch.renderInto[IO](e2, component2)
      _ <- IO {
             getButton(e1).dispatchEvent(evt)
             getButton(e2).dispatchEvent(evt)
           }
      _ <- IO.cede
      _  = e1.innerHTML shouldBe e1.innerHTML

    } yield succeed
  }

  "A todo application" should "work with components" in {
    def TodoComponent(title: String, deleteStream: Observer[String]) =
      li(
        span(title),
        button(idAttr := title, onClick.as(title) --> deleteStream, "Delete"),
      )

    def TextFieldComponent(labelText: String, outputStream: Observer[String]) = for {

      textFieldStream <- IO(Subject.replayLatest[String]())
      clickStream     <- IO(Subject.replayLatest[MouseEvent]())
      keyStream       <- IO(Subject.replayLatest[KeyboardEvent]())

    } yield {
      val buttonDisabled = textFieldStream
        .map(_.length < 2)
        .startWith(Seq(true))

      val enterPressed = keyStream
        .filter(_.key == "Enter")

      val confirm = Observable
        .merge(enterPressed, clickStream)
        .withLatestMap(textFieldStream)((_, input) => input)

      div(
        EmitterBuilder.fromSource(confirm) --> outputStream,
        label(labelText),
        input(idAttr  := "input", tpe := "text", onInput.value --> textFieldStream, onKeyUp --> keyStream),
        button(idAttr := "submit", onClick --> clickStream, disabled <-- buttonDisabled, "Submit"),
      )
    }

    def addToList(todo: String) = { (list: Vector[String]) =>
      list :+ todo
    }

    def removeFromList(todo: String) = { (list: Vector[String]) =>
      list.filterNot(_ == todo)
    }

    val vtree = for {
      inputHandler  <- IO(Subject.replayLatest[String]())
      deleteHandler <- IO(Subject.replayLatest[String]())

      adds = inputHandler
               .map(addToList)

      deletes = deleteHandler
                  .map(removeFromList)

      state = Observable
                .merge(adds, deletes)
                .scan(Vector[String]())((state, modify) => modify(state))
                .map(_.map(n => TodoComponent(n, deleteHandler)))
      textFieldComponent = TextFieldComponent("Todo: ", inputHandler)

    } yield div(
      textFieldComponent,
      ul(idAttr := "list", state),
    )

    for {

      root <- IO {
                val root = document.createElement("div")
                document.body.appendChild(root)
                root
              }

      vtree <- vtree
      _     <- Outwatch.renderInto[IO](root, vtree)

      inputEvt <- IO {
                    new Event(
                      "input",
                      new EventInit {
                        bubbles = false
                        cancelable = true
                      },
                    )
                  }

      clickEvt <- IO {
                    new Event(
                      "click",
                      new EventInit {
                        bubbles = true
                        cancelable = true
                      },
                    )
                  }

      inputE    <- IO(document.getElementById("input").asInstanceOf[html.Input])
      submitBtn <- IO(document.getElementById("submit"))
      list      <- IO(document.getElementById("list"))
      _          = list.childElementCount shouldBe 0

      t <- IO {
             val todo = "fold laundry"
             inputE.value = todo
             inputE.dispatchEvent(inputEvt)
             submitBtn.dispatchEvent(clickEvt)
             todo
           }

      _ <- IO.cede

      _ = list.childElementCount shouldBe 1

      t2 <- IO {
              val todo2 = "wash dishes"
              inputE.value = todo2
              inputE.dispatchEvent(inputEvt)
              submitBtn.dispatchEvent(clickEvt)
              todo2
            }

      _ <- IO.cede

      _ = list.childElementCount shouldBe 2

      t3 <- IO {
              val todo3 = "clean windows"
              inputE.value = todo3
              inputE.dispatchEvent(inputEvt)
              submitBtn.dispatchEvent(clickEvt)
              todo3
            }

      _ <- IO.cede

      _ = list.childElementCount shouldBe 3

      _ <- IO(document.getElementById(t2).dispatchEvent(clickEvt))
      _ <- IO.cede
      _  = list.childElementCount shouldBe 2

      _ <- IO(document.getElementById(t3).dispatchEvent(clickEvt))
      _ <- IO.cede
      _  = list.childElementCount shouldBe 1

      _ <- IO(document.getElementById(t).dispatchEvent(clickEvt))
      _ <- IO.cede
      _  = list.childElementCount shouldBe 0

    } yield succeed
  }
}
