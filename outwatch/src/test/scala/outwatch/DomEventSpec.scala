package outwatch

import cats.effect.IO
import monix.reactive.subjects.PublishSubject
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom.{html, _}
import outwatch.Deprecated.IgnoreWarnings.initEvent
import outwatch.dom._
import outwatch.dom.dsl._
import outwatch.util.LocalStorage

import scala.scalajs.js

class DomEventSpec extends JSDomAsyncSpec {

  "EventStreams" should "emit and receive events correctly" in {

    val vtree = Handler.create[IO, MouseEvent].map { handler =>

      val buttonDisabled = handler.map(_ => true).startWith(Seq(false))

      div(id := "click", onClick --> handler,
        button(id := "btn", disabled <-- buttonDisabled)
      )
    }

    for {
      vtree <- vtree
          _ <- OutWatch.renderInto[IO]("#app", vtree)
       hasD <- IO(document.getElementById("btn").hasAttribute("disabled"))
          _ <- IO(hasD shouldBe false)
      event <- IO {
                 val event = document.createEvent("Events")
                 initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
                 event
              }
          _ <- IO(document.getElementById("click").dispatchEvent(event))
          d <- IO(document.getElementById("btn").getAttribute("disabled"))
          _ <- IO(d shouldBe "")
    } yield succeed
  }

  it should "be converted to a generic emitter correctly" in {

    val message = "ad"

    val vtree = Handler.create[IO, String].map { handler =>
      div(id := "click", onClick(message) --> handler,
        span(id := "child", handler)
      )
    }

    for {
      vtree <- vtree
      _ <- OutWatch.renderInto[IO]("#app", vtree)
    } yield {
      document.getElementById("child").innerHTML shouldBe ""

      val event = document.createEvent("Events")
      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("child").innerHTML shouldBe message

      //dispatch another event
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("child").innerHTML shouldBe message

    }
  }

  it should "be converted to a generic stream emitter correctly" in {

    Handler.create[IO, String].flatMap { messages =>

      val vtree = Handler.create[IO, String].map { stream =>
        div(id := "click", onClick(messages) --> stream,
          span(id := "child", stream)
        )
      }

      for {
        vtree <- vtree
        _ <- OutWatch.renderInto[IO]("#app", vtree)
      } yield {

        document.getElementById("child").innerHTML shouldBe ""

        val firstMessage = "First"
        messages.onNext(firstMessage)

        val event = document.createEvent("Events")
        initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
        document.getElementById("click").dispatchEvent(event)

        document.getElementById("child").innerHTML shouldBe firstMessage

        //dispatch another event
        document.getElementById("click").dispatchEvent(event)

        document.getElementById("child").innerHTML shouldBe firstMessage

        val secondMessage = "Second"
        messages.onNext(secondMessage)

        document.getElementById("click").dispatchEvent(event)

        document.getElementById("child").innerHTML shouldBe secondMessage
      }

    }
  }

  it should "be able to set the value of a text field" in {

    val values = PublishSubject[String]

    val vtree = input(id := "input", attributes.value <-- values)

    OutWatch.renderInto[IO]("#app", vtree).map {_ =>

      val patched = document.getElementById("input").asInstanceOf[html.Input]

      patched.value shouldBe ""

      val value1 = "Hello"
      values.onNext(value1)

      patched.value shouldBe value1

      val value2 = "World"
      values.onNext(value2)

      patched.value shouldBe value2

      values.onNext("")

      patched.value shouldBe ""

    }
  }

  it should "preserve user input after setting defaultValue" in {
    val defaultValues = PublishSubject[String]

    val vtree = input(id := "input", attributes.defaultValue <-- defaultValues)
    OutWatch.renderInto[IO]("#app", vtree).map { _ =>

      val patched = document.getElementById("input").asInstanceOf[html.Input]
      patched.value shouldBe ""

      val value1 = "Hello"
      defaultValues.onNext(value1)
      patched.value shouldBe value1

      val userInput = "user input"
      patched.value = userInput

      defaultValues.onNext("GoodByte")
      patched.value shouldBe userInput

    }
  }

  it should "set input value to the same value after user change" in {
    val values = PublishSubject[String]

    val vtree = input(id := "input", attributes.value <-- values)
    OutWatch.renderInto[IO]("#app", vtree).map { _ =>

      val patched = document.getElementById("input").asInstanceOf[html.Input]
      patched.value shouldBe ""

      val value1 = "Hello"
      values.onNext(value1)
      patched.value shouldBe value1

      patched.value = "user input"

      values.onNext("Hello")
      patched.value shouldBe value1

    }
  }

  it should "be bindable to a list of children" in {

    val state = PublishSubject[Seq[VNode]]


    val vtree = div(
      ul(id := "list", state)
    )

    OutWatch.renderInto[IO]("#app", vtree).map { _ =>

      val list = document.getElementById("list")

      list.childElementCount shouldBe 0

      val first = "Test"

      state.onNext(Seq(span(first)))

      list.childElementCount shouldBe 1
      list.innerHTML.contains(first) shouldBe true

      val second = "Hello"
      state.onNext(Seq(span(first), span(second)))

      list.childElementCount shouldBe 2
      list.innerHTML.contains(first) shouldBe true
      list.innerHTML.contains(second) shouldBe true

      val third = "World"

      state.onNext(Seq(span(first), span(second), span(third)))

      list.childElementCount shouldBe 3
      list.innerHTML.contains(first) shouldBe true
      list.innerHTML.contains(second) shouldBe true
      list.innerHTML.contains(third) shouldBe true

      state.onNext(Seq(span(first), span(third)))

      list.childElementCount shouldBe 2
      list.innerHTML.contains(first) shouldBe true
      list.innerHTML.contains(third) shouldBe true
    }
  }

  it should "be able to handle two events of the same type" in {

    val messages = ("Hello", "World")

    val node = Handler.create[IO, String].flatMap { first =>
      Handler.create[IO, String].map { second =>
        div(
          button(id := "click", onClick(messages._1) --> first, onClick(messages._2) --> second),
          span(id := "first", first),
          span(id := "second", second)
        )
      }
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {
      val event = document.createEvent("Events")
      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("first").innerHTML shouldBe messages._1
      document.getElementById("second").innerHTML shouldBe messages._2
    }
  }

  it should "be able to be transformed by a function in place" in {

    val number = 42

    val toTuple = (e: MouseEvent) => (e, number)

    val node = Handler.create[IO, (MouseEvent, Int)].map { stream =>
      div(
        button(id := "click", onClick.map(toTuple) --> stream),
        span(id := "num", stream.map(_._2))
      )
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {

      val event = document.createEvent("Events")
      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("num").innerHTML shouldBe number.toString

    }
  }

  it should ".transform should work as expected" in {

    val numbers = Observable(1, 2)

    val transformer = (e: Observable[MouseEvent]) => e.concatMap(_ => numbers)

    val node = Handler.create[IO, Int].map { stream =>

      val state = stream.scan(List.empty[Int])((l, s) => l :+ s)

      div(
        button(id := "click", onClick.transform(transformer) --> stream),
        span(id := "num", state.map(nums => nums.map(num => span(num.toString))))
      )
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {

      val event = document.createEvent("Events")
      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
      document.getElementById("click").dispatchEvent(event)

      document.getElementById("num").innerHTML shouldBe "<span>1</span><span>2</span>"

    }
  }

  it should "be able to be transformed from strings" in {

    val number = 42
    val onInputValue = onInput.value
    val node = Handler.create[IO, Int].map { stream =>
      div(
        input(id := "input", onInputValue(number) --> stream),
        span(id := "num", stream)
      )
    }

    for {
      node <- node
      _ <- OutWatch.renderInto[IO]("#app", node)
    } yield {

      val inputEvt = document.createEvent("HTMLEvents")
      initEvent(inputEvt)("input", canBubbleArg = false, cancelableArg = true)
      document.getElementById("input").dispatchEvent(inputEvt)

      document.getElementById("num").innerHTML shouldBe number.toString

    }
  }

  it should "handler can trigger side-effecting functions" in {
    var triggeredEventFunction = 0
    var triggeredIntFunction = 0
    var triggeredFunction = 0
    var triggeredFunction2 = 0

    val stream = PublishSubject[String]
    val node = {
      div(
        button(id := "button",
          onClick foreach (_ => triggeredEventFunction += 1),
          onClick(1) foreach (triggeredIntFunction += _),
          onClick foreach  { triggeredFunction += 1 },
          onSnabbdomUpdate foreach { triggeredFunction2 += 1 },
          stream
        )
      )
    }

    OutWatch.renderInto[IO]("#app", node).map {_ =>

      val inputEvt = document.createEvent("HTMLEvents")
      initEvent(inputEvt)("click", canBubbleArg = false, cancelableArg = true)

      document.getElementById("button").dispatchEvent(inputEvt)
      stream.onNext("woop")
      triggeredEventFunction shouldBe 1
      triggeredIntFunction shouldBe 1
      triggeredFunction shouldBe 1
      triggeredFunction2 shouldBe 1

      document.getElementById("button").dispatchEvent(inputEvt)
      stream.onNext("waap")
      triggeredEventFunction shouldBe 2
      triggeredIntFunction shouldBe 2
      triggeredFunction shouldBe 2
      triggeredFunction2 shouldBe 2

    }
  }

  it should "correctly be transformed from latest in observable" in {

    val node = Handler.create[IO, String].flatMap { submit =>

      val state = submit.scan(List.empty[String])((l, s) => l :+ s)

      Handler.create[IO, String].map { stream =>
        div(
          input(id := "input", tpe := "text", onInput.value --> stream),
          button(id := "submit", onClick(stream) --> submit),
          ul(id := "items",
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

      val inputEvt = document.createEvent("HTMLEvents")
      initEvent(inputEvt)("input", canBubbleArg = false, cancelableArg = true)

      val clickEvt = document.createEvent("Events")
      initEvent(clickEvt)("click", canBubbleArg = true, cancelableArg = true)

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

    val node = Handler.create[IO, Boolean].map { checkValue =>
      div(
        input(id := "checkbox", `type` := "Checkbox", checked <-- checkValue),
        button(id := "on_button", onClick(true) --> checkValue, "On"),
        button(id := "off_button", onClick(false) --> checkValue, "Off")
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

      val clickEvt = document.createEvent("Events")
      initEvent(clickEvt)("click", canBubbleArg = true, cancelableArg = true)

      onButton.dispatchEvent(clickEvt)

      checkbox.checked shouldBe true

      offButton.dispatchEvent(clickEvt)

      checkbox.checked shouldBe false

    }
  }

  "DomWindowEvents and DomDocumentEvents" should "trigger correctly" in {

    var docClicked = false
    var winClicked = false
    events.window.onClick(_ => winClicked = true)
    events.document.onClick(_ => docClicked = true)

    val node = div(button(id := "input", tpe := "checkbox"))

    OutWatch.renderInto[IO]("#app", node).map { _ =>

      val inputEvt = document.createEvent("HTMLEvents")
      initEvent(inputEvt)("click", canBubbleArg = true, cancelableArg = false)

      document.getElementById("input").dispatchEvent(inputEvt)

      winClicked shouldBe true
      docClicked shouldBe true

    }

  }

  "EmitterOps" should "correctly work on events" in {

    val node = Handler.create[IO, String].flatMap { _ =>

      for {
        stringStream <- Handler.create[IO, String]
        doubleStream <- Handler.create[IO, Double]
        boolStream <- Handler.create[IO, Boolean]
        htmlElementStream <- Handler.create[IO, html.Element]
        svgElementTupleStream <- Handler.create[IO, (org.scalajs.dom.svg.Element, org.scalajs.dom.svg.Element)]
        elem = div(
          input(
            id := "input", tpe := "text",

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
          ul(id := "items")
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

    Handler.create[IO, String].flatMap { stringHandler =>
      def modifier: VDomModifier = onDrag.value --> stringHandler

      Handler.create[IO, String].flatMap { _ =>

      for {
        stream <- Handler.create[IO, String]
        eventStream <- Handler.create[IO, MouseEvent]
        elem = div(
          input(
            id := "input", tpe := "text",
            onSearch.target.value --> stream,
            onClick.value --> stream,

            modifier
          ),
          ul(id := "items"))
        _ <- OutWatch.renderInto[IO]("#app", elem)
        } yield {
          document.getElementById("input") should not be null
        }
      }
    }
  }

  "Children stream" should "work for string sequences" in {
    val myStrings: Observable[Seq[String]] = Observable(Seq("a", "b"))
    val node = div(id := "strings",
      myStrings
    )

    OutWatch.renderInto[IO]("#app", node).map( _ =>
      document.getElementById("strings").innerHTML shouldBe "ab"
    )
  }

  "LocalStorage" should "have handler with proper events" in {
    var option: Option[Option[String]] = None

    LocalStorage.handler[IO]("hans").map { handler =>

      handler.foreach { o => option = Some(o) }

      option shouldBe Some(None)

      handler.onNext(Some("gisela"))
      option shouldBe Some(Some("gisela"))

      handler.onNext(None)
      option shouldBe Some(None)

    }
  }

  it should "have handlerWithEventsOnly with proper events" in {
    var option: Option[Option[String]] = None

    LocalStorage.handlerWithEventsOnly[IO]("hans").map {handler =>
      handler.foreach { o => option = Some(o) }

      option shouldBe Some(None)

      handler.onNext(Some("gisela"))
      option shouldBe Some(None)

      handler.onNext(None)
      option shouldBe Some(None)

    }
  }

  it should "have handlerWithEventsOnly with initial value" in {
    import org.scalajs.dom.window.localStorage
    localStorage.setItem("hans", "wurst")

    var option: Option[Option[String]] = None

    LocalStorage.handlerWithEventsOnly[IO]("hans").map { handler =>

      handler.foreach { o => option = Some(o) }
      option shouldBe Some(Some("wurst"))
    }
  }

  it should "have handlerWithoutEvents with proper events" in {
    var option: Option[Option[String]] = None

    LocalStorage.handlerWithoutEvents[IO]("hans").map { handler =>

      handler.foreach { o => option = Some(o) }

      option shouldBe Some(None)

      handler.onNext(Some("gisela"))
      option shouldBe Some(Some("gisela"))

      handler.onNext(None)
      option shouldBe Some(None)

    }
  }

  "Emitterbuilder" should "preventDefault (compile only)" in {

    val node = div(
      id := "click",
      onClick.filter(_ => true).preventDefault.map(_ => 4) foreach {()},
      onClick.preventDefault.map(_ => 3) foreach {()}
    )

    val test = for {
      _ <- OutWatch.renderInto[IO]("#app", node)
      _ <- IO {
            val event = document.createEvent("Events")
            initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
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
      onClick foreach {triggeredSecond = true},
      div(
        id := "click",
        onClick.stopPropagation foreach {triggeredFirst = true}
      )
    )

    OutWatch.renderInto[IO]("#app", node).map { _ =>

      val event = document.createEvent("Events")
      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
      document.getElementById("click").dispatchEvent(event)

      triggeredFirst shouldBe true
      triggeredSecond shouldBe false
    }
  }

  it should "stopImmediatePropagation" in {
    // stopImmediatePropagation is supported in jsdom since version 9.12
    // https://github.com/jsdom/jsdom/blob/master/Changelog.md#9120
    pending

    var triggeredFirst = false
    var triggeredSecond = false
    val node = div(
      id := "click",
      onClick.stopImmediatePropagation foreach {triggeredFirst = true},
      onClick foreach {triggeredSecond = true}
    )

    OutWatch.renderInto[IO]("#app", node).map { _ =>

      val event = document.createEvent("Events")
      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
      document.getElementById("click").dispatchEvent(event)

      triggeredFirst shouldBe true
      triggeredSecond shouldBe false

    }
  }

  "Global dom events" should "return an observable" in {
    var clicked = 0
    val sub = events.window.onClick.foreach { _ =>
      clicked += 1
    }


    def newEvent() = {
      val event = document.createEvent("Events")
      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
      event
    }

    clicked shouldBe 0

    window.dispatchEvent(newEvent())

    clicked shouldBe 1

    window.dispatchEvent(newEvent())

    clicked shouldBe 2

    sub.cancel()
    window.dispatchEvent(newEvent())

    clicked shouldBe 2
  }

  it should "have sync operations" in {
    var clicked = List.empty[String]
    val sub = events.window.onClick.stopPropagation.foreach { ev =>
      clicked ++= ev.asInstanceOf[js.Dynamic].testtoken.asInstanceOf[String] :: Nil
    }

    def newEvent(token: String) = {
      val event = document.createEvent("Events")
      event.asInstanceOf[js.Dynamic].stopPropagation = { () =>
        event.asInstanceOf[js.Dynamic].testtoken = token
        ()
      }: js.Function0[Unit]
      initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
      event
    }

    clicked shouldBe Nil

    window.dispatchEvent(newEvent("a"))

    clicked shouldBe List("a")

    window.dispatchEvent(newEvent("b"))

    clicked shouldBe List("a", "b")

    sub.cancel()
    window.dispatchEvent(newEvent("c"))

    clicked shouldBe List("a", "b")
  }
}
