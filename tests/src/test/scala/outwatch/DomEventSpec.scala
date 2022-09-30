package outwatch

import cats.effect.IO
import org.scalajs.dom.{html, _}
import outwatch.dsl._
import org.scalajs.dom.EventInit
import colibri._
import outwatch.util.LocalStorage

import scala.scalajs.js

class DomEventSpec extends JSDomAsyncSpec {

  "EventStreams" should "emit and receive events correctly" in {

    val vtree = IO(Subject.replayLatest[MouseEvent]()).map { handler =>
      val buttonDisabled = handler.map(_ => true).startWith(Seq(false))

      div(idAttr := "click", onClick --> handler, button(idAttr := "btn", disabled <-- buttonDisabled))
    }

    for {
      vtree <- vtree
      _     <- Outwatch.renderInto[IO]("#app", vtree)
      hasD  <- IO(document.getElementById("btn").hasAttribute("disabled"))
      _     <- IO(hasD shouldBe false)
      event <- IO {
                 new Event(
                   "click",
                   new EventInit {
                     bubbles = true
                     cancelable = false
                   },
                 )
               }
      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede
      d <- IO(document.getElementById("btn").getAttribute("disabled"))
      _ <- IO(d shouldBe "")
    } yield succeed
  }

  it should "be converted to a generic emitter correctly" in {

    val message = "ad"

    val vtree = IO(Subject.replayLatest[String]()).map { handler =>
      div(idAttr := "click", onClick.as(message) --> handler, span(idAttr := "child", handler))
    }

    for {
      vtree <- vtree
      _     <- Outwatch.renderInto[IO]("#app", vtree)
      _      = document.getElementById("child").innerHTML shouldBe ""

      event = new Event(
                "click",
                new EventInit {
                  bubbles = true
                  cancelable = false
                },
              )
      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = document.getElementById("child").innerHTML shouldBe message

      // dispatch another event
      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = document.getElementById("child").innerHTML shouldBe message

    } yield succeed
  }

  it should "be converted to a generic stream emitter correctly" in {

    val messages = Subject.replayLatest[String]()

    val vtree = IO(Subject.replayLatest[String]()).map { stream =>
      div(idAttr := "click", onClick.asLatest(messages) --> stream, span(idAttr := "child", stream))
    }

    for {
      vtree <- vtree
      _     <- Outwatch.renderInto[IO]("#app", vtree)

      _ = document.getElementById("child").innerHTML shouldBe ""

      firstMessage = "First"
      _           <- messages.onNextIO(firstMessage) *> IO.cede

      event = new Event(
                "click",
                new EventInit {
                  bubbles = true
                  cancelable = false
                },
              )
      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = document.getElementById("child").innerHTML shouldBe firstMessage

      // dispatch another event
      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = document.getElementById("child").innerHTML shouldBe firstMessage

      secondMessage = "Second"
      _            <- messages.onNextIO(secondMessage) *> IO.cede

      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = document.getElementById("child").innerHTML shouldBe secondMessage
    } yield succeed
  }

  it should "be able to set the value of a text field" in {

    val values = Subject.publish[String]()

    val vtree = input(idAttr := "input", attributes.value <-- values)

    for {
      _ <- Outwatch.renderInto[IO]("#app", vtree)

      patched = document.getElementById("input").asInstanceOf[html.Input]

      _ = patched.value shouldBe ""

      value1 = "Hello"
      _     <- values.onNextIO(value1) *> IO.cede

      _ = patched.value shouldBe value1

      value2 = "World"
      _     <- values.onNextIO(value2) *> IO.cede

      _ = patched.value shouldBe value2

      _ <- values.onNextIO("") *> IO.cede

      _ = patched.value shouldBe ""

    } yield succeed
  }

  it should "preserve user input after setting defaultValue" in {
    val defaultValues = Subject.publish[String]()

    val vtree = input(idAttr := "input", attributes.defaultValue <-- defaultValues)

    for {
      _ <- Outwatch.renderInto[IO]("#app", vtree)

      patched = document.getElementById("input").asInstanceOf[html.Input]
      _       = patched.value shouldBe ""

      value1 = "Hello"
      _     <- defaultValues.onNextIO(value1) *> IO.cede
      _      = patched.value shouldBe value1

      userInput = "user input"
      _         = patched.value = userInput

      _ <- defaultValues.onNextIO("GoodByte") *> IO.cede
      _  = patched.value shouldBe userInput

    } yield succeed
  }

  it should "set input value to the same value after user change" in {
    val values = Subject.publish[String]()

    val vtree = input(idAttr := "input", attributes.value <-- values)

    for {
      _ <- Outwatch.renderInto[IO]("#app", vtree)

      patched = document.getElementById("input").asInstanceOf[html.Input]
      _       = patched.value shouldBe ""

      value1 = "Hello"
      _     <- values.onNextIO(value1) *> IO.cede
      _      = patched.value shouldBe value1

      _ = patched.value = "user input"

      _ <- values.onNextIO("Hello") *> IO.cede
      _  = patched.value shouldBe value1

    } yield succeed
  }

  it should "be bindable to a list of children" in {

    val state = Subject.publish[Seq[VNode]]()

    val vtree = div(
      ul(idAttr := "list", state),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", vtree)

      list = document.getElementById("list")

      _ = list.childElementCount shouldBe 0

      first = "Test"

      _ <- state.onNextIO(Seq(span(first))) *> IO.cede

      _ = list.childElementCount shouldBe 1
      _ = list.innerHTML.contains(first) shouldBe true

      second = "Hello"
      _     <- state.onNextIO(Seq(span(first), span(second))) *> IO.cede

      _ = list.childElementCount shouldBe 2
      _ = list.innerHTML.contains(first) shouldBe true
      _ = list.innerHTML.contains(second) shouldBe true

      third = "World"

      _ <- state.onNextIO(Seq(span(first), span(second), span(third))) *> IO.cede

      _ = list.childElementCount shouldBe 3
      _ = list.innerHTML.contains(first) shouldBe true
      _ = list.innerHTML.contains(second) shouldBe true
      _ = list.innerHTML.contains(third) shouldBe true

      _ <- state.onNextIO(Seq(span(first), span(third))) *> IO.cede

      _ = list.childElementCount shouldBe 2
      _ = list.innerHTML.contains(first) shouldBe true
      _ = list.innerHTML.contains(third) shouldBe true
    } yield succeed
  }

  it should "be able to handle two events of the same type" in {

    val messages = ("Hello", "World")

    val node = IO(Subject.replayLatest[String]()).flatMap { first =>
      IO(Subject.replayLatest[String]()).map { second =>
        div(
          button(idAttr := "click", onClick.as(messages._1) --> first, onClick.as(messages._2) --> second),
          span(idAttr   := "first", first),
          span(idAttr   := "second", second),
        )
      }
    }

    for {
      node <- node
      _    <- Outwatch.renderInto[IO]("#app", node)

      event = new Event(
                "click",
                new EventInit {
                  bubbles = true
                  cancelable = false
                },
              )

      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = document.getElementById("first").innerHTML shouldBe messages._1
      _ = document.getElementById("second").innerHTML shouldBe messages._2
    } yield succeed
  }

  it should "be able to be transformed by a function in place" in {

    val number = 42

    val toTuple = (e: MouseEvent) => (e, number)

    val node = IO(Subject.replayLatest[(MouseEvent, Int)]()).map { stream =>
      div(
        button(idAttr := "click", onClick.map(toTuple) --> stream),
        span(idAttr   := "num", stream.map(_._2)),
      )
    }

    for {
      node <- node
      _    <- Outwatch.renderInto[IO]("#app", node)

      event = new Event(
                "click",
                new EventInit {
                  bubbles = true
                  cancelable = false
                },
              )

      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = document.getElementById("num").innerHTML shouldBe number.toString

    } yield succeed
  }

  it should ".transform should work as expected" in {

    val numbers = Observable.fromIterable(Seq(1, 2))

    val transformer = (e: Observable[MouseEvent]) => e.mergeMap(_ => numbers)

    val node = IO(Subject.replayLatest[Int]()).map { stream =>
      val state = stream.scan(List.empty[Int])((l, s) => l :+ s)

      div(
        button(idAttr := "click", onClick.transform(transformer) --> stream),
        span(idAttr   := "num", state.map(nums => nums.map(num => span(num.toString)))),
      )
    }

    for {
      node <- node
      _    <- Outwatch.renderInto[IO]("#app", node)

      event = new Event(
                "click",
                new EventInit {
                  bubbles = true
                  cancelable = false
                },
              )

      _ <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = document.getElementById("num").innerHTML shouldBe "<span>1</span><span>2</span>"

    } yield succeed
  }

  it should "be able to be transformed from strings" in {

    val number       = 42
    val onInputValue = onInput.value
    val node = IO(Subject.replayLatest[Int]()).map { stream =>
      div(
        input(idAttr := "input", onInputValue.as(number) --> stream),
        span(idAttr  := "num", stream),
      )
    }

    for {
      node <- node
      _    <- Outwatch.renderInto[IO]("#app", node)

      inputEvt = new Event(
                   "input",
                   new EventInit {
                     bubbles = false
                     cancelable = true
                   },
                 )
      _ <- IO(document.getElementById("input").dispatchEvent(inputEvt)) *> IO.cede

      _ = document.getElementById("num").innerHTML shouldBe number.toString

    } yield succeed
  }

  it should "handler can trigger side-effecting functions" in {
    var triggeredEventFunction = 0
    var triggeredIntFunction   = 0
    var triggeredFunction      = 0
    var triggeredFunction2     = 0

    val stream = Subject.publish[String]()
    val node = {
      div(
        button(
          idAttr := "button",
          onClick foreach (_ => triggeredEventFunction += 1),
          onClick.as(1) foreach (triggeredIntFunction += _),
          onClick doAction { triggeredFunction += 1 },
          onSnabbdomUpdate doAction { triggeredFunction2 += 1 },
          stream,
        ),
      )
    }

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      inputEvt = new Event(
                   "click",
                   new EventInit {
                     bubbles = false
                     cancelable = true
                   },
                 )
      _ <- IO(document.getElementById("button").dispatchEvent(inputEvt)) *> IO.cede
      _ <- stream.onNextIO("woop") *> IO.cede
      _  = triggeredEventFunction shouldBe 1
      _  = triggeredIntFunction shouldBe 1
      _  = triggeredFunction shouldBe 1
      _  = triggeredFunction2 shouldBe 1

      _ <- IO(document.getElementById("button").dispatchEvent(inputEvt)) *> IO.cede
      _ <- stream.onNextIO("waap") *> IO.cede
      _  = triggeredEventFunction shouldBe 2
      _  = triggeredIntFunction shouldBe 2
      _  = triggeredFunction shouldBe 2
      _  = triggeredFunction2 shouldBe 2

    } yield succeed
  }

  it should "correctly be transformed from latest in observable" in {

    val node = IO(Subject.replayLatest[String]()).flatMap { submit =>
      val state = submit.scan(List.empty[String])((l, s) => l :+ s)

      IO(Subject.replayLatest[String]()).map { stream =>
        div(
          input(idAttr  := "input", tpe := "text", onInput.value --> stream),
          button(idAttr := "submit", onClick.asLatest(stream) --> submit),
          ul(idAttr     := "items", state.map(items => items.map(it => li(it)))),
        )
      }
    }

    for {
      node <- node
      _    <- Outwatch.renderInto[IO]("#app", node)

      inputElement = document.getElementById("input").asInstanceOf[html.Input]
      submitButton = document.getElementById("submit")

      inputEvt = new Event(
                   "input",
                   new EventInit {
                     bubbles = false
                     cancelable = true
                   },
                 )
      clickEvt = new Event(
                   "click",
                   new EventInit {
                     bubbles = true
                     cancelable = true
                   },
                 )
      _  = inputElement.value = "item 1"
      _ <- IO(inputElement.dispatchEvent(inputEvt)) *> IO.cede

      _  = inputElement.value = "item 2"
      _ <- IO(inputElement.dispatchEvent(inputEvt)) *> IO.cede

      _  = inputElement.value = "item 3"
      _ <- IO(inputElement.dispatchEvent(inputEvt)) *> IO.cede

      _ <- IO(submitButton.dispatchEvent(clickEvt)) *> IO.cede

      _ = document.getElementById("items").childNodes.length shouldBe 1

    } yield succeed
  }

  "Boolean Props" should "be handled corectly" in {

    val node = IO(Subject.replayLatest[Boolean]()).map { checkValue =>
      div(
        input(idAttr  := "checkbox", `type` := "Checkbox", checked <-- checkValue),
        button(idAttr := "on_button", onClick.as(true) --> checkValue, "On"),
        button(idAttr := "off_button", onClick.as(false) --> checkValue, "Off"),
      )
    }

    for {
      node <- node
      _    <- Outwatch.renderInto[IO]("#app", node)

      checkbox  = document.getElementById("checkbox").asInstanceOf[html.Input]
      onButton  = document.getElementById("on_button")
      offButton = document.getElementById("off_button")

      _ = checkbox.checked shouldBe false

      clickEvt = new Event(
                   "click",
                   new EventInit {
                     bubbles = true
                     cancelable = true
                   },
                 )
      _ <- IO(onButton.dispatchEvent(clickEvt)) *> IO.cede

      _ = checkbox.checked shouldBe true

      _ <- IO(offButton.dispatchEvent(clickEvt)) *> IO.cede

      _ = checkbox.checked shouldBe false

    } yield succeed
  }

  "DomWindowEvents and DomDocumentEvents" should "trigger correctly" in {

    var docClicked = false
    var winClicked = false
    events.window.onClick.unsafeForeach(_ => winClicked = true)
    events.document.onClick.unsafeForeach(_ => docClicked = true)

    val node = div(button(idAttr := "input", tpe := "checkbox"))

    Outwatch.renderInto[IO]("#app", node).map { _ =>
      val inputEvt = new Event(
        "click",
        new EventInit {
          bubbles = true
          cancelable = false
        },
      )
      document.getElementById("input").dispatchEvent(inputEvt)

      winClicked shouldBe true
      docClicked shouldBe true

    }

  }

  "EmitterOps" should "correctly work on events" in {

    val node = for {
      stringStream          <- IO(Subject.replayLatest[String]())
      doubleStream          <- IO(Subject.replayLatest[Double]())
      boolStream            <- IO(Subject.replayLatest[Boolean]())
      htmlElementStream     <- IO(Subject.replayLatest[html.Element]())
      svgElementTupleStream <- IO(Subject.replayLatest[(org.scalajs.dom.svg.Element, org.scalajs.dom.svg.Element)]())
      elem = div(
               input(
                 idAttr := "input",
                 tpe    := "text",
                 onSearch.target.value --> stringStream,
                 onSearch.target.valueAsNumber --> doubleStream,
                 onSearch.target.checked --> boolStream,
                 onClick.target.value --> stringStream,

                 // uses currentTarget and assumes html.Input type by default
                 onClick.value --> stringStream,
                 onClick.valueAsNumber --> doubleStream,
                 onChange.checked --> boolStream,
                 onClick.filter(_ => true).value --> stringStream,
                 onSnabbdomInsert.asHtml --> htmlElementStream,
                 onSnabbdomUpdate.asSvg --> svgElementTupleStream,
               ),
               ul(idAttr := "items"),
             )
    } yield elem

    for {
      node <- node
      _    <- Outwatch.renderInto[IO]("#app", node)
    } yield {
      document.getElementById("input") should not be null
    }
  }

  it should "correctly be compiled with currentTarget" in {

    IO(Subject.replayLatest[String]()).flatMap { stringHandler =>
      def modifier: VModifier = onDrag.value --> stringHandler

      IO(Subject.replayLatest[String]()).flatMap { _ =>
        for {
          stream      <- IO(Subject.replayLatest[String]())
          eventStream <- IO(Subject.replayLatest[MouseEvent]())
          elem = div(
                   input(
                     idAttr := "input",
                     tpe    := "text",
                     onSearch.target.value --> stream,
                     onClick.value --> stream,
                     modifier,
                   ),
                   ul(idAttr := "items"),
                 )
          _ <- Outwatch.renderInto[IO]("#app", elem)
        } yield {
          document.getElementById("input") should not be null
        }
      }
    }
  }

  "Children stream" should "work for string sequences" in {
    val myStrings: Observable[Seq[String]] = Observable(Seq("a", "b"))
    val node                               = div(idAttr := "strings", myStrings)

    Outwatch.renderInto[IO]("#app", node).map(_ => document.getElementById("strings").innerHTML shouldBe "ab")
  }

  "LocalStorage" should "have handler with proper events" in {
    var option: Option[Option[String]] = None

    LocalStorage.handler[IO]("hans").map { handler =>
      handler.unsafeForeach { o => option = Some(o) }

      option shouldBe Some(None)

      handler.unsafeOnNext(Some("gisela"))
      option shouldBe Some(Some("gisela"))

      handler.unsafeOnNext(None)
      option shouldBe Some(None)

    }
  }

  it should "have handlerWithEventsOnly with proper events" in {
    var option: Option[Option[String]] = None

    LocalStorage.handlerWithEventsOnly[IO]("hans").map { handler =>
      handler.unsafeForeach { o => option = Some(o) }

      option shouldBe Some(None)

      handler.unsafeOnNext(Some("gisela"))
      option shouldBe Some(None)

      handler.unsafeOnNext(None)
      option shouldBe Some(None)

    }
  }

  it should "have handlerWithEventsOnly with initial value" in {
    import org.scalajs.dom.window.localStorage
    localStorage.setItem("hans", "wurst")

    var option: Option[Option[String]] = None

    LocalStorage.handlerWithEventsOnly[IO]("hans").map { handler =>
      handler.unsafeForeach { o => option = Some(o) }
      option shouldBe Some(Some("wurst"))
    }
  }

  it should "have handlerWithoutEvents with proper events" in {
    var option: Option[Option[String]] = None

    LocalStorage.handlerWithoutEvents[IO]("hans").map { handler =>
      handler.unsafeForeach { o => option = Some(o) }

      option shouldBe Some(None)

      handler.unsafeOnNext(Some("gisela"))
      option shouldBe Some(Some("gisela"))

      handler.unsafeOnNext(None)
      option shouldBe Some(None)

    }
  }

  "Emitterbuilder" should "preventDefault (compile only)" in {

    val node = div(
      idAttr := "click",
      onClick.filter(_ => true).preventDefault.map(_ => 4).discard,
      onClick.preventDefault.map(_ => 3).discard,
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _ <- IO {
             val event = new Event(
               "click",
               new EventInit {
                 bubbles = true
                 cancelable = false
               },
             )
             document.getElementById("click").dispatchEvent(event)
           }
    } yield {
      succeed
    }
  }

  it should "stopPropagation" in {
    var triggeredFirst  = false
    var triggeredSecond = false

    val node = div(
      onClick doAction { triggeredSecond = true },
      div(
        idAttr := "click",
        onClick.map(x => x).stopPropagation doAction { triggeredFirst = true },
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      event = new Event(
                "click",
                new EventInit {
                  bubbles = true
                  cancelable = false
                },
              )
      send <- IO(document.getElementById("click").dispatchEvent(event)) *> IO.cede

      _ = triggeredFirst shouldBe true
      _ = triggeredSecond shouldBe false
    } yield succeed
  }

  "Global dom events" should "return an observable" in {
    var clicked = 0
    val sub = events.window.onClick.unsafeForeach { _ =>
      clicked += 1
    }

    def newEvent() = {
      new Event(
        "click",
        new EventInit {
          bubbles = true
          cancelable = false
        },
      )
    }

    clicked shouldBe 0

    window.dispatchEvent(newEvent())

    clicked shouldBe 1

    window.dispatchEvent(newEvent())

    clicked shouldBe 2

    sub.unsafeCancel()
    window.dispatchEvent(newEvent())

    clicked shouldBe 2
  }

  it should "have sync operations" in {
    var clicked = List.empty[String]
    val sub = events.window.onClick.stopPropagation.unsafeForeach { ev =>
      clicked ++= ev.asInstanceOf[js.Dynamic].testtoken.asInstanceOf[String] :: Nil
    }

    def newEvent(token: String) = {
      val event = new Event(
        "click",
        new EventInit {
          bubbles = true
          cancelable = false
        },
      )
      event.asInstanceOf[js.Dynamic].stopPropagation = { () =>
        event.asInstanceOf[js.Dynamic].testtoken = token
        ()
      }: js.Function0[Unit]
      event
    }

    clicked shouldBe Nil

    window.dispatchEvent(newEvent("a"))

    clicked shouldBe List("a")

    window.dispatchEvent(newEvent("b"))

    clicked shouldBe List("a", "b")

    sub.unsafeCancel()
    window.dispatchEvent(newEvent("c"))

    clicked shouldBe List("a", "b")
  }
}
