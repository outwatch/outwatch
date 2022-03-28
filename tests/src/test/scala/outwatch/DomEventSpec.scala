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

    val vtree = IO(Subject.replayLast[MouseEvent]()).map { handler =>

      val buttonDisabled = handler.map(_ => true).startWith(Seq(false))

      div(idAttr := "click", onClick --> handler,
        button(idAttr := "btn", disabled <-- buttonDisabled)
      )
    }

    for {
      vtree <- vtree
          _ <- OutWatch.renderInto[IO]("#app", vtree)
       hasD <- IO(document.getElementById("btn").hasAttribute("disabled"))
          _ <- IO(hasD shouldBe false)
      event <- IO {
                 new Event("click", new EventInit {
                  bubbles = true
                  cancelable = false
                })
              }
          _ <- IO(document.getElementById("click").dispatchEvent(event))
          d <- IO(document.getElementById("btn").getAttribute("disabled"))
          _ <- IO(d shouldBe "")
    } yield succeed
  }

  it should "be converted to a generic emitter correctly" in {

    val message = "ad"

    val vtree = IO(Subject.replayLast[String]()).map { handler =>
      div(idAttr := "click", onClick.as(message) --> handler,
        span(idAttr := "child", handler)
      )
    }

    for {
      vtree <- vtree
      _ <- OutWatch.renderInto[IO]("#app", vtree)
    } yield {
      document.getElementById("child").innerHTML shouldBe ""

      val event = new Event("click", new EventInit {
        bubbles = true
        cancelable = false
      })
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("child").innerHTML shouldBe message

      //dispatch another event
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("child").innerHTML shouldBe message

    }
  }

  it should "be converted to a generic stream emitter correctly" in {

    IO(Subject.replayLast[String]()).flatMap { messages =>

      val vtree = IO(Subject.replayLast[String]()).map { stream =>
        div(idAttr := "click", onClick.asLatest(messages) --> stream,
          span(idAttr := "child", stream)
        )
      }

      for {
        vtree <- vtree
        _ <- OutWatch.renderInto[IO]("#app", vtree)
      } yield {

        document.getElementById("child").innerHTML shouldBe ""

        val firstMessage = "First"
        messages.unsafeOnNext(firstMessage)

        val event = new Event("click", new EventInit {
          bubbles = true
          cancelable = false
        })
        document.getElementById("click").dispatchEvent(event)

        document.getElementById("child").innerHTML shouldBe firstMessage

        //dispatch another event
        document.getElementById("click").dispatchEvent(event)

        document.getElementById("child").innerHTML shouldBe firstMessage

        val secondMessage = "Second"
        messages.unsafeOnNext(secondMessage)

        document.getElementById("click").dispatchEvent(event)

        document.getElementById("child").innerHTML shouldBe secondMessage
      }

    }
  }

  it should "be able to set the value of a text field" in {

    val values = Subject.publish[String]()

    val vtree = input(idAttr := "input", attributes.value <-- values)

    OutWatch.renderInto[IO]("#app", vtree).map {_ =>

      val patched = document.getElementById("input").asInstanceOf[html.Input]

      patched.value shouldBe ""

      val value1 = "Hello"
      values.unsafeOnNext(value1)

      patched.value shouldBe value1

      val value2 = "World"
      values.unsafeOnNext(value2)

      patched.value shouldBe value2

      values.unsafeOnNext("")

      patched.value shouldBe ""

    }
  }

  it should "preserve user input after setting defaultValue" in {
    val defaultValues = Subject.publish[String]()

    val vtree = input(idAttr := "input", attributes.defaultValue <-- defaultValues)
    OutWatch.renderInto[IO]("#app", vtree).map { _ =>

      val patched = document.getElementById("input").asInstanceOf[html.Input]
      patched.value shouldBe ""

      val value1 = "Hello"
      defaultValues.unsafeOnNext(value1)
      patched.value shouldBe value1

      val userInput = "user input"
      patched.value = userInput

      defaultValues.unsafeOnNext("GoodByte")
      patched.value shouldBe userInput

    }
  }

  it should "set input value to the same value after user change" in {
    val values = Subject.publish[String]()

    val vtree = input(idAttr := "input", attributes.value <-- values)
    OutWatch.renderInto[IO]("#app", vtree).map { _ =>

      val patched = document.getElementById("input").asInstanceOf[html.Input]
      patched.value shouldBe ""

      val value1 = "Hello"
      values.unsafeOnNext(value1)
      patched.value shouldBe value1

      patched.value = "user input"

      values.unsafeOnNext("Hello")
      patched.value shouldBe value1

    }
  }

  it should "be bindable to a list of children" in {

    val state = Subject.publish[Seq[VNode]]()


    val vtree = div(
      ul(idAttr := "list", state)
    )

    OutWatch.renderInto[IO]("#app", vtree).map { _ =>

      val list = document.getElementById("list")

      list.childElementCount shouldBe 0

      val first = "Test"

      state.unsafeOnNext(Seq(span(first)))

      list.childElementCount shouldBe 1
      list.innerHTML.contains(first) shouldBe true

      val second = "Hello"
      state.unsafeOnNext(Seq(span(first), span(second)))

      list.childElementCount shouldBe 2
      list.innerHTML.contains(first) shouldBe true
      list.innerHTML.contains(second) shouldBe true

      val third = "World"

      state.unsafeOnNext(Seq(span(first), span(second), span(third)))

      list.childElementCount shouldBe 3
      list.innerHTML.contains(first) shouldBe true
      list.innerHTML.contains(second) shouldBe true
      list.innerHTML.contains(third) shouldBe true

      state.unsafeOnNext(Seq(span(first), span(third)))

      list.childElementCount shouldBe 2
      list.innerHTML.contains(first) shouldBe true
      list.innerHTML.contains(third) shouldBe true
    }
  }

  it should "be able to handle two events of the same type" in {

    val messages = ("Hello", "World")

    val node = IO(Subject.replayLast[String]()).flatMap { first =>
      IO(Subject.replayLast[String]()).map { second =>
        div(
          button(idAttr := "click", onClick.as(messages._1) --> first, onClick.as(messages._2) --> second),
          span(idAttr := "first", first),
          span(idAttr := "second", second)
        )
      }
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {
      val event = new Event("click", new EventInit {
        bubbles = true
        cancelable = false
      })
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("first").innerHTML shouldBe messages._1
      document.getElementById("second").innerHTML shouldBe messages._2
    }
  }

  it should "be able to be transformed by a function in place" in {

    val number = 42

    val toTuple = (e: MouseEvent) => (e, number)

    val node = IO(Subject.replayLast[(MouseEvent, Int)]()).map { stream =>
      div(
        button(idAttr := "click", onClick.map(toTuple) --> stream),
        span(idAttr := "num", stream.map(_._2))
      )
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {

      val event = new Event("click", new EventInit {
        bubbles = true
        cancelable = false
      })
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("num").innerHTML shouldBe number.toString

    }
  }

  it should ".transform should work as expected" in {

    val numbers = Observable.fromIterable(Seq(1, 2))

    val transformer = (e: Observable[MouseEvent]) => e.mergeMap(_ => numbers)

    val node = IO(Subject.replayLast[Int]()).map { stream =>

      val state = stream.scan(List.empty[Int])((l, s) => l :+ s)

      div(
        button(idAttr := "click", onClick.transform(transformer) --> stream),
        span(idAttr := "num", state.map(nums => nums.map(num => span(num.toString))))
      )
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {

      val event = new Event("click", new EventInit {
        bubbles = true
        cancelable = false
      })
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("num").innerHTML shouldBe "<span>1</span><span>2</span>"

    }
  }

  it should "be able to be transformed from strings" in {

    val number = 42
    val onInputValue = onInput.value
    val node = IO(Subject.replayLast[Int]()).map { stream =>
      div(
        input(idAttr := "input", onInputValue.as(number) --> stream),
        span(idAttr := "num", stream)
      )
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {

      val inputEvt = new Event("input", new EventInit {
        bubbles = false
        cancelable = true
      })
      document.getElementById("input").dispatchEvent(inputEvt)

      document.getElementById("num").innerHTML shouldBe number.toString

    }
  }

  it should "handler can trigger side-effecting functions" in {
    var triggeredEventFunction = 0
    var triggeredIntFunction = 0
    var triggeredFunction = 0
    var triggeredFunction2 = 0

    val stream = Subject.publish[String]()
    val node = {
      div(
        button(idAttr := "button",
          onClick foreach (_ => triggeredEventFunction += 1),
          onClick.as(1) foreach (triggeredIntFunction += _),
          onClick doAction  { triggeredFunction += 1 },
          onSnabbdomUpdate doAction { triggeredFunction2 += 1 },
          stream
        )
      )
    }

    OutWatch.renderInto[IO]("#app", node).map {_ =>

      val inputEvt = new Event("click", new EventInit {
        bubbles = false
        cancelable = true
      })
      document.getElementById("button").dispatchEvent(inputEvt)
      stream.unsafeOnNext("woop")
      triggeredEventFunction shouldBe 1
      triggeredIntFunction shouldBe 1
      triggeredFunction shouldBe 1
      triggeredFunction2 shouldBe 1

      document.getElementById("button").dispatchEvent(inputEvt)
      stream.unsafeOnNext("waap")
      triggeredEventFunction shouldBe 2
      triggeredIntFunction shouldBe 2
      triggeredFunction shouldBe 2
      triggeredFunction2 shouldBe 2

    }
  }

  it should "correctly be transformed from latest in observable" in {

    val node = IO(Subject.replayLast[String]()).flatMap { submit =>

      val state = submit.scan(List.empty[String])((l, s) => l :+ s)

      IO(Subject.replayLast[String]()).map { stream =>
        div(
          input(idAttr := "input", tpe := "text", onInput.value --> stream),
          button(idAttr := "submit", onClick.asLatest(stream) --> submit),
          ul(idAttr := "items",
            state.map(items => items.map(it => li(it)))
          )
        )
      }
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {

      val inputElement = document.getElementById("input").asInstanceOf[html.Input]
      val submitButton = document.getElementById("submit")

      val inputEvt = new Event("input", new EventInit {
        bubbles = false
        cancelable = true
      })
      val clickEvt = new Event("click", new EventInit {
        bubbles = true
        cancelable = true
      })
      inputElement.value = "item 1"
      inputElement.dispatchEvent(inputEvt)

      inputElement.value = "item 2"
      inputElement.dispatchEvent(inputEvt)

      inputElement.value = "item 3"
      inputElement.dispatchEvent(inputEvt)

      submitButton.dispatchEvent(clickEvt)

      document.getElementById("items").childNodes.length shouldBe 1

    }
  }

  "Boolean Props" should "be handled corectly" in {

    val node = IO(Subject.replayLast[Boolean]()).map { checkValue =>
      div(
        input(idAttr := "checkbox", `type` := "Checkbox", checked <-- checkValue),
        button(idAttr := "on_button", onClick.as(true) --> checkValue, "On"),
        button(idAttr := "off_button", onClick.as(false) --> checkValue, "Off")
      )
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {

      val checkbox = document.getElementById("checkbox").asInstanceOf[html.Input]
      val onButton = document.getElementById("on_button")
      val offButton = document.getElementById("off_button")

      checkbox.checked shouldBe false

      val clickEvt = new Event("click", new EventInit {
        bubbles = true
        cancelable = true
      })
      onButton.dispatchEvent(clickEvt)

      checkbox.checked shouldBe true

      offButton.dispatchEvent(clickEvt)

      checkbox.checked shouldBe false

    }
  }

  "DomWindowEvents and DomDocumentEvents" should "trigger correctly" in {

    var docClicked = false
    var winClicked = false
    events.window.onClick.unsafeForeach(_ => winClicked = true)
    events.document.onClick.unsafeForeach(_ => docClicked = true)

    val node = div(button(idAttr := "input", tpe := "checkbox"))

    OutWatch.renderInto[IO]("#app", node).map { _ =>

      val inputEvt = new Event("click", new EventInit {
        bubbles = true
        cancelable = false
      })
      document.getElementById("input").dispatchEvent(inputEvt)

      winClicked shouldBe true
      docClicked shouldBe true

    }

  }

  "EmitterOps" should "correctly work on events" in {

    val node = IO(Subject.replayLast[String]()).flatMap { _ =>

      for {
        stringStream <- IO(Subject.replayLast[String]())
        doubleStream <- IO(Subject.replayLast[Double]())
        boolStream <- IO(Subject.replayLast[Boolean]())
        htmlElementStream <- IO(Subject.replayLast[html.Element]())
        svgElementTupleStream <- IO(Subject.replayLast[(org.scalajs.dom.svg.Element, org.scalajs.dom.svg.Element)]())
        elem = div(
          input(
            idAttr := "input", tpe := "text",

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
            onSnabbdomUpdate.asSvg --> svgElementTupleStream
          ),
          ul(idAttr := "items")
        )
      } yield elem
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {
      document.getElementById("input") should not be null
    }
  }

  it should "correctly be compiled with currentTarget" in {

    IO(Subject.replayLast[String]()).flatMap { stringHandler =>
      def modifier: VDomModifier = onDrag.value --> stringHandler

      IO(Subject.replayLast[String]()).flatMap { _ =>

      for {
        stream <- IO(Subject.replayLast[String]())
        eventStream <- IO(Subject.replayLast[MouseEvent]())
        elem = div(
          input(
            idAttr := "input", tpe := "text",
            onSearch.target.value --> stream,
            onClick.value --> stream,

            modifier
          ),
          ul(idAttr := "items"))
        _ <- OutWatch.renderInto[IO]("#app", elem)
        } yield {
          document.getElementById("input") should not be null
        }
      }
    }
  }

  "Children stream" should "work for string sequences" in {
    val myStrings: Observable[Seq[String]] = Observable(Seq("a", "b"))
    val node = div(idAttr := "strings",
      myStrings
    )

    OutWatch.renderInto[IO]("#app", node).map( _ =>
      document.getElementById("strings").innerHTML shouldBe "ab"
    )
  }

  "LocalStorage" should "have handler with proper events" in {
    var option: Option[Option[String]] = None

    val handler = LocalStorage.subjectWithEvents("hans")

    handler.unsafeForeach { o => option = Some(o) }

    option shouldBe Some(None)

    handler.unsafeOnNext(Some("gisela"))
    option shouldBe Some(Some("gisela"))

    handler.unsafeOnNext(None)
    option shouldBe Some(None)
  }

  it should "have handlerWithoutEvents with proper events" in {
    var option: Option[Option[String]] = None

    val handler = LocalStorage.subject("hans")

    handler.unsafeForeach { o => option = Some(o) }

    option shouldBe Some(None)

    handler.unsafeOnNext(Some("gisela"))
    option shouldBe Some(Some("gisela"))

    handler.unsafeOnNext(None)
    option shouldBe Some(None)
  }

  "Emitterbuilder" should "preventDefault (compile only)" in {

    val node = div(
      idAttr := "click",
      onClick.filter(_ => true).preventDefault.map(_ => 4).discard,
      onClick.preventDefault.map(_ => 3).discard
    )

    val test = for {
      _ <- OutWatch.renderInto[IO]("#app", node)
      _ <- IO {
            val event = new Event("click", new EventInit {
              bubbles = true
              cancelable = false
            })
            document.getElementById("click").dispatchEvent(event)
          }
    } yield {
      succeed
    }

    test

  }

  it should "stopPropagation" in {
    var triggeredFirst = false
    var triggeredSecond = false

    val node = div(
      onClick doAction {triggeredSecond = true},
      div(
        idAttr := "click",
        onClick.map(x => x).stopPropagation doAction {triggeredFirst = true}
      )
    )

    OutWatch.renderInto[IO]("#app", node).map { _ =>

      val event = new Event("click", new EventInit {
        bubbles = true
        cancelable = false
      })
      document.getElementById("click").dispatchEvent(event)

      triggeredFirst shouldBe true
      triggeredSecond shouldBe false
    }
  }

  "Global dom events" should "return an observable" in {
    var clicked = 0
    val sub = events.window.onClick.unsafeForeach { _ =>
      clicked += 1
    }


    def newEvent() = {
      new Event("click", new EventInit {
        bubbles = true
        cancelable = false
      })
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
      val event = new Event("click", new EventInit {
        bubbles = true
        cancelable = false
      })
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
