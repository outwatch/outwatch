package outwatch


import monix.reactive.subjects.PublishSubject
import org.scalajs.dom.{html, _}
import outwatch.Deprecated.IgnoreWarnings.initEvent
import outwatch.dom._
import outwatch.dom.dsl._

class DomEventSpec extends JSDomSpec {

  "EventStreams" should "emit and receive events correctly" in {

    val vtree = Handler.create[MouseEvent].flatMap { handler =>

      val buttonDisabled = handler.map(_ => true).startWith(Seq(false))

      div(id := "click", onClick --> handler,
        button(id := "btn", disabled <-- buttonDisabled)
      )
    }

    OutWatch.renderInto("#app", vtree).unsafeRunSync()
    document.getElementById("btn").hasAttribute("disabled") shouldBe false

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("btn").getAttribute("disabled") shouldBe ""
  }

  it should "be converted to a generic emitter correctly" in {

    val message = "ad"

    val vtree = Handler.create[String].flatMap { handler =>
      div(id := "click", onClick(message) --> handler,
        span(id := "child", handler)
      )
    }

    OutWatch.renderInto("#app", vtree).unsafeRunSync()

    document.getElementById("child").innerHTML shouldBe ""

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe message

    //dispatch another event
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe message
  }

  it should "be converted to a generic stream emitter correctly" in {

    val messages = Handler.create[String].unsafeRunSync()

    val vtree = Handler.create[String].flatMap { stream =>
      div(id := "click", onClick(messages) --> stream,
        span(id := "child", stream)
      )
    }

    OutWatch.renderInto("#app", vtree).unsafeRunSync()

    document.getElementById("child").innerHTML shouldBe ""

    val firstMessage = "First"
    messages.observer.onNext(firstMessage)

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe firstMessage

    //dispatch another event
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe firstMessage

    val secondMessage = "Second"
    messages.observer.onNext(secondMessage)

    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe secondMessage
  }

  it should "be able to set the value of a text field" in {

    val values = PublishSubject[String]

    val vtree = input(id := "input", attributes.value <-- values)

    OutWatch.renderInto("#app", vtree).unsafeRunSync()

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
  it should "preserve user input after setting defaultValue" in {
    val defaultValues = PublishSubject[String]

    val vtree = input(id := "input", attributes.defaultValue <-- defaultValues)
    OutWatch.renderInto("#app", vtree).unsafeRunSync()

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

  it should "set input value to the same value after user change" in {
    val values = PublishSubject[String]

    val vtree = input(id := "input", attributes.value <-- values)
    OutWatch.renderInto("#app", vtree).unsafeRunSync()

    val patched = document.getElementById("input").asInstanceOf[html.Input]
    patched.value shouldBe ""

    val value1 = "Hello"
    values.onNext(value1)
    patched.value shouldBe value1

    patched.value = "user input"

    values.onNext("Hello")
    patched.value shouldBe value1
  }

  it should "be bindable to a list of children" in {

    val state = PublishSubject[Seq[VNode]]


    val vtree = div(
      ul(id := "list", state)
    )

    OutWatch.renderInto("#app", vtree).unsafeRunSync()

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

  it should "be able to handle two events of the same type" in {

    val first = Handler.create[String].unsafeRunSync()

    val second = Handler.create[String].unsafeRunSync()

    val messages = ("Hello", "World")

    val node = div(
      button(id := "click", onClick(messages._1) --> first, onClick(messages._2) --> second),
      span(id := "first", first),
      span(id := "second", second)
    )

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)

    document.getElementById("click").dispatchEvent(event)

    document.getElementById("first").innerHTML shouldBe messages._1
    document.getElementById("second").innerHTML shouldBe messages._2
  }

  it should "be able to be transformed by a function in place" in {

    val number = 42

    val toTuple = (e: MouseEvent) => (e, number)

    val node = Handler.create[(MouseEvent, Int)].flatMap { stream =>
      div(
        button(id := "click", onClick.map(toTuple) --> stream),
        span(id := "num", stream.map(_._2))
      )
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)

    document.getElementById("click").dispatchEvent(event)

    document.getElementById("num").innerHTML shouldBe number.toString
  }


  it should ".transform should work as expected" in {

    val numbers = Observable(1, 2)

    val transformer = (e: Observable[MouseEvent]) => e.concatMap(_ => numbers)

    val node = Handler.create[Int].flatMap { stream =>

      val state = stream.scan(List.empty[Int])((l, s) => l :+ s)

      div(
        button(id := "click", onClick.transform(transformer) --> stream),
        span(id := "num", state.map(nums => nums.map(num => span(num.toString))))
      )
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)

    document.getElementById("click").dispatchEvent(event)

    document.getElementById("num").innerHTML shouldBe "<span>1</span><span>2</span>"
  }

  it should "be able to be transformed from strings" in {

    val number = 42
    val onInputValue = onInput.value
    val node = Handler.create[Int].flatMap { stream =>
      div(
        input(id := "input", onInputValue(number) --> stream),
        span(id := "num", stream)
      )
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    initEvent(inputEvt)("input", false, true)

    document.getElementById("input").dispatchEvent(inputEvt)

    document.getElementById("num").innerHTML shouldBe number.toString
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
          onClick --> sideEffect(_ => triggeredEventFunction += 1),
          onClick(1) --> sideEffect(triggeredIntFunction += _),
          onClick --> sideEffect { triggeredFunction += 1 },
          onUpdate --> sideEffect((old, current) => triggeredFunction2 += 1),
          stream
        )
      )
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    initEvent(inputEvt)("click", false, true)

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

  it should "be able to toggle attributes with a boolean observer" in {
    import outwatch.util.SyntaxSugar._

    val someClass = "some-class"
    val node = Handler.create[Boolean].flatMap { stream =>
      div(
        button(id := "input", tpe := "checkbox", onClick(true) --> stream),
        span(id := "toggled", stream ?= (className := someClass))
      )
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    initEvent(inputEvt)("click", true, false)


    document.getElementById("input").dispatchEvent(inputEvt)

    document.getElementById("toggled").classList.contains(someClass) shouldBe true
  }


  it should "correctly be transformed from latest in observable" in {

    val node = Handler.create[String].flatMap { submit =>

      val state = submit.scan(List.empty[String])((l, s) => l :+ s)

      Handler.create[String].flatMap { stream =>
        div(
          input(id := "input", tpe := "text", onInput.value --> stream),
          button(id := "submit", onClick(stream) --> submit),
          ul(id := "items",
            state.map(items => items.map(it => li(it)))
          )
        )
      }
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val inputElement = document.getElementById("input").asInstanceOf[html.Input]
    val submitButton = document.getElementById("submit")

    val inputEvt = document.createEvent("HTMLEvents")
    initEvent(inputEvt)("input", false, true)

    val clickEvt = document.createEvent("Events")
    initEvent(clickEvt)("click", true, true)

    inputElement.value = "item 1"
    inputElement.dispatchEvent(inputEvt)

    inputElement.value = "item 2"
    inputElement.dispatchEvent(inputEvt)

    inputElement.value = "item 3"
    inputElement.dispatchEvent(inputEvt)

    submitButton.dispatchEvent(clickEvt)

    document.getElementById("items").childNodes.length shouldBe 1
  }


  "Boolean Props" should "be handled corectly" in {

    val node = Handler.create[Boolean].flatMap { checkValue =>
      div(
        input(id := "checkbox", `type` := "Checkbox", checked <-- checkValue),
        button(id := "on_button", onClick(true) --> checkValue, "On"),
        button(id := "off_button", onClick(false) --> checkValue, "Off")
      )
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val checkbox = document.getElementById("checkbox").asInstanceOf[html.Input]
    val onButton = document.getElementById("on_button")
    val offButton = document.getElementById("off_button")

    checkbox.checked shouldBe false

    val clickEvt = document.createEvent("Events")
    initEvent(clickEvt)("click", true, true)

    onButton.dispatchEvent(clickEvt)

    checkbox.checked shouldBe true

    offButton.dispatchEvent(clickEvt)

    checkbox.checked shouldBe false
  }

  "DomWindowEvents and DomDocumentEvents" should "trigger correctly" in {
    import outwatch.dom._

    var docClicked = false
    var winClicked = false
    events.window.onClick(ev => winClicked = true)
    events.document.onClick(ev => docClicked = true)

    val node =
      div(
        button(id := "input", tpe := "checkbox")
      )

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    initEvent(inputEvt)("click", true, false)

    document.getElementById("input").dispatchEvent(inputEvt)

    winClicked shouldBe true
    docClicked shouldBe true
  }

  "EmitterOps" should "correctly work on events" in {

    val node = Handler.create[String].flatMap { submit =>

      for {
        stringStream <- Handler.create[String]
        doubleStream <- Handler.create[Double]
        boolStream <- Handler.create[Boolean]
        htmlElementStream <- Handler.create[html.Element]
        svgElementTupleStream <- Handler.create[(org.scalajs.dom.svg.Element, org.scalajs.dom.svg.Element)]
        elem <- div(
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

            onInsert.asHtml --> htmlElementStream,
            onUpdate.asSvg --> svgElementTupleStream
          ),
          ul(id := "items")
        )
      } yield elem
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val element = document.getElementById("input")
    element should not be null
  }

  it should "correctly be compiled with currentTarget" in {

    val stringHandler = Handler.create[String].unsafeRunSync()

    def modifier: VDomModifier = onDrag.value --> stringHandler

    val node = Handler.create[String].flatMap { submit =>

      for {
        stream <- Handler.create[String]
        eventStream <- Handler.create[MouseEvent]
        elem <- div(
          input(
            id := "input", tpe := "text",

            onSearch.target.value --> stream,
            onClick.value --> stream,

            modifier
          ),
          ul(id := "items")
        )
      } yield elem
    }

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val element = document.getElementById("input")
    element should not be null
  }


  "Children stream" should "work for string sequences" in {
    val myStrings: Observable[Seq[String]] = Observable(Seq("a", "b"))
    val node = div(id := "strings",
      myStrings
    )

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val element = document.getElementById("strings")
    element.innerHTML shouldBe "ab"
  }

  "LocalStorage.handler" should "should have proper events" in {
    var option: Option[Option[String]] = None
    val handler = util.LocalStorage.handler("hans").unsafeRunSync()
    handler.foreach { o => option = Some(o) }

    option shouldBe Some(None)

    handler.unsafeOnNext(Some("gisela"))
    option shouldBe Some(Some("gisela"))

    handler.unsafeOnNext(None)
    option shouldBe Some(None)
  }

  "LocalStorage.handlerWithEventsOnly" should "should have proper events" in {
    var option: Option[Option[String]] = None
    val handler = util.LocalStorage.handlerWithEventsOnly("hans").unsafeRunSync()
    handler.foreach { o => option = Some(o) }

    option shouldBe Some(None)

    handler.unsafeOnNext(Some("gisela"))
    option shouldBe Some(None)

    handler.unsafeOnNext(None)
    option shouldBe Some(None)
  }

  "LocalStorage.handlerWithoutEvents" should "should have proper events" in {
    var option: Option[Option[String]] = None
    val handler = util.LocalStorage.handlerWithoutEvents("hans").unsafeRunSync()
    handler.foreach { o => option = Some(o) }

    option shouldBe Some(None)

    handler.unsafeOnNext(Some("gisela"))
    option shouldBe Some(Some("gisela"))

    handler.unsafeOnNext(None)
    option shouldBe Some(None)
  }

  "Emitterbuilder" should "preventDefault (compile only)" in {
    val node = div(
      id := "click",
      onClick.filter(_ => true).preventDefault.map(_ => 4) --> sideEffect{()},
      onClick.preventDefault.map(_ => 3) --> sideEffect{()}
    )

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)
  }

  "Emitterbuilder" should "stopPropagation" in {
    var triggeredFirst = false
    var triggeredSecond = false
    val node = div(
      onClick --> sideEffect{triggeredSecond = true},
      div(
        id := "click",
        onClick.stopPropagation --> sideEffect{triggeredFirst = true}
      )
    )

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    triggeredFirst shouldBe true
    triggeredSecond shouldBe false
  }

  "Emitterbuilder" should "stopImmediatePropagation" in {
    // stopImmediatePropagation is supported in jsdom since version 9.12
    // https://github.com/jsdom/jsdom/blob/master/Changelog.md#9120
    pending

    var triggeredFirst = false
    var triggeredSecond = false
    val node = div(
      id := "click",
      onClick.stopImmediatePropagation --> sideEffect{triggeredFirst = true},
      onClick --> sideEffect{triggeredSecond = true}
    )

    OutWatch.renderInto("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    initEvent(event)("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    triggeredFirst shouldBe true
    triggeredSecond shouldBe false
  }
}
