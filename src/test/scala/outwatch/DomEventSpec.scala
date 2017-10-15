package outwatch

import org.scalajs.dom._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.PropertyChecks
import rxscalajs.Subject
import outwatch.dom._

class DomEventSpec extends UnitSpec with BeforeAndAfterEach with PropertyChecks {

  override def beforeEach(): Unit = {
    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
    ()
  }

  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  "EventStreams" should "emit and receive events correctly" in {

    val vtree = Handler.mouseEvents.flatMap { observable =>

      val buttonDisabled = observable.mapTo(true).startWith(false)
      
      div(id := "click", onClick --> observable,
        button(id := "btn", disabled <-- buttonDisabled)
      )
    }

    OutWatch.render("#app", vtree).unsafeRunSync()
    document.getElementById("btn").hasAttribute("disabled") shouldBe false

    val event = document.createEvent("Events")
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("btn").getAttribute("disabled") shouldBe ""
  }

  it should "be converted to a generic emitter correctly" in {

    val message = "ad"

    val vtree = Handler.create[String].flatMap { observable =>
      div(id := "click", onClick(message) --> observable,
        span(id := "child", child <-- observable)
      )
    }

    OutWatch.render("#app", vtree).unsafeRunSync()

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

    val messages = Handler.create[String].unsafeRunSync()

    val vtree = Handler.create[String].flatMap { stream =>
      div(id := "click", onClick(messages) --> stream,
        span(id := "child", child <-- stream)
      )
    }

    OutWatch.render("#app", vtree).unsafeRunSync()

    document.getElementById("child").innerHTML shouldBe ""

    val firstMessage = "First"
    messages.observer.next(firstMessage)

    val event = document.createEvent("Events")
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe firstMessage

    //dispatch another event
    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe firstMessage

    val secondMessage = "Second"
    messages.observer.next(secondMessage)

    document.getElementById("click").dispatchEvent(event)

    document.getElementById("child").innerHTML shouldBe secondMessage
  }

  it should "be able to set the value of a text field" in {

    val values = Subject[String]

    val vtree = input(id:= "input", outwatch.dom.value <-- values)

    OutWatch.render("#app", vtree).unsafeRunSync()

    val patched = document.getElementById("input").asInstanceOf[html.Input]

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

    val state = Subject[Seq[VNode]]


    val vtree = div(
      ul(id:= "list", children <-- state)
    )

    OutWatch.render("#app", vtree).unsafeRunSync()

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

  it should "be able to handle two events of the same type" in {

    val first = Handler.create[String].unsafeRunSync()

    val second = Handler.create[String].unsafeRunSync()

    val messages = ("Hello", "World")

    val node = div(
      button(id := "click", onClick(messages._1) --> first, onClick(messages._2) --> second),
      span(id:="first",child <-- first),
      span(id:="second",child <-- second)
    )

    OutWatch.render("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)


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
        span(id := "num", child <-- stream.map(_._2))
      )
    }

    OutWatch.render("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)


    document.getElementById("click").dispatchEvent(event)

    document.getElementById("num").innerHTML shouldBe number.toString
  }



  it should ".transform should work as expected" in {

    val numbers = Observable.of(1, 2)

    val transformer = (e: Observable[MouseEvent]) => e.concatMap(_ => numbers)

    val node = Handler.create[Int].flatMap { stream =>

      val state = stream.scan(List.empty[Int])((l, s) => l :+ s)

      div(
        button(id := "click", onClick.transform(transformer) --> stream),
        span(id := "num", children <-- state.map(nums => nums.map(num => span(num.toString))))
      )
    }

    OutWatch.render("#app", node).unsafeRunSync()

    val event = document.createEvent("Events")
    event.initEvent("click", canBubbleArg = true, cancelableArg = false)


    document.getElementById("click").dispatchEvent(event)

    document.getElementById("num").innerHTML shouldBe "<span>1</span><span>2</span>"
  }

  it should "be able to be transformed from strings" in {

    val number = 42
    val node = Handler.create[Int].flatMap { stream =>
      div(
        button(id := "input", onInputString(number) --> stream),
        span(id:="num",child <-- stream)
      )
    }

    OutWatch.render("#app", node).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    inputEvt.initEvent("input", false, true)


    document.getElementById("input").dispatchEvent(inputEvt)

    document.getElementById("num").innerHTML shouldBe number.toString
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

    OutWatch.render("#app", node).unsafeRunSync()

    val inputEvt = document.createEvent("HTMLEvents")
    inputEvt.initEvent("click", true, false)


    document.getElementById("input").dispatchEvent(inputEvt)

    document.getElementById("toggled").classList.contains(someClass) shouldBe true
  }


  it should "currectly be transformed from latest in observable" in {

    val node = Handler.create[String].flatMap { submit =>

      val state = submit.scan(List.empty[String])((l, s) => l :+ s)

      Handler.create[String].flatMap { stream =>
        div(
          input(id := "input", tpe := "text", onInputString --> stream),
          button(id := "submit", onClick(stream) --> submit),
          ul( id := "items",
            children <-- state.map(items => items.map(it => li(it)))
          )
        )
      }
    }

    OutWatch.render("#app", node).unsafeRunSync()

    val inputElement = document.getElementById("input").asInstanceOf[html.Input]
    val submitButton = document.getElementById("submit")

    val inputEvt = document.createEvent("HTMLEvents")
    inputEvt.initEvent("input", false, true)

    val clickEvt = document.createEvent("Events")
    clickEvt.initEvent("click", true, true)

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
