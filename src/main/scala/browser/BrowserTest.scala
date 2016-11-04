package browser

import outwatch.http.Http
import rxscalajs.{Observable, Subject}
import outwatch.dom.helpers.{InputEvent, DomUtils}

import scala.scalajs.js.{JSApp, JSON}
import org.scalajs.dom._
import outwatch.dom._



object BrowserTest extends JSApp {


  def main(): Unit = {
    todo()
  }

  def realWorld() = {

    val searchQueries = Subject[String]
    val orderByClicks = Subject[Boolean]

    val orderByCommands = orderByClicks.map(desc => if (desc) "desc" else "asc")

    val requests = searchQueries
      .combineLatest(orderByCommands)
      .map((query, order) => s"api.github.com/search/repositories?q=$query&order=$order&sort=stars")
      .debounceTime(300)
      .filter(_.length > 3)
      .distinctUntilChanged

    val responses = Http.get(requests)
      .retry(5)
      .map(o => List(li(o.toString))) //TODO
      .catchError(_ => Observable.of(List(strong("An error occured!"))))



    val component = div(
      label("Search"),
      input(inputType := "text", inputString --> searchQueries),
      input(inputType := "checkbox", inputBool --> orderByClicks),
      hr(),
      ul(children <-- responses)
    )
  }

  def errors() = {
    val obs = Observable.create[VNode](observer=> {
      observer.next("0")
      observer.next("1")
      observer.error("Shiiit")
      observer.next("2")
    })

    val fallback = Observable.of(div("safe!"))

    obs.catchError(a => fallback).subscribe(vnode => println(vnode), e => println("Wtf" + e))


    val tree = div(
      child <-- obs.catchError(any => obs.startWith("-1"))
    )

    OutWatch.render(document.getElementById("app"), tree)
  }

  def routing() = {
    val interval = 4000
    val state = Observable.interval(interval)
    val firstChild = h2(child <-- state)

    val obs = Subject[VNode]()
    val b = Subject[Boolean]

    val router = div(child <-- obs, h2("asdas"), inputBool --> b)

    val conditional = b.map(b => if (b) div("Yeah bro!") else span("God damnit!"))


    OutWatch.render(document.getElementById("app"),router)
    obs.next(firstChild)


    val cond = div(
      ul(
        li(if (true) span("Yeah") else input(placeholder := "No luck!"))
      )
    )

    sealed trait Shape
    case class Rectangle(width: Double, height: Double) extends Shape
    case class Circle(radius: Double) extends Shape


    val shape: Shape = Circle(3)
    val patterns = div(
      shape match {
        case Circle(Math.PI) => strong("The area of this circle is PI * PI * PI!!! :D")
        case Circle(radius) => span(s"Radius: $radius")
        case Rectangle(width, height) => span(s"Width: $width, Height: $height")
      }
    )




    val inputs = Subject[String]

    val childLists = inputs.map(s => li(s))
      .scan(Seq[VNode]())((list, vnode) => list :+  vnode)

    val secondChild = h3(child <-- state)
    val timeout = 9000
    window.setTimeout(() => obs.next(secondChild), timeout)

  }

  def todo() ={

    def TodoComponent(title: String, deleteStream: Subject[String]) =
      li(
        span(title),
        button(id:= title, click(title) --> deleteStream, "Delete")
      )

    val inputHandler = Subject[String]
    val deleteHandler = Subject[String]

    val adds = inputHandler
      .map(n => (list: Vector[String]) => list :+ n)

    val deletes = deleteHandler
      .map(n => (list: Vector[String]) => list.filter(_ != n))

    val state = adds.merge(deletes)
      .scan(Vector[String]())((state, modify) => modify(state))
      .map(_.map(n => TodoComponent(n, deleteHandler)))


    val vtree = div(
      TextFieldComponent("Todo: ", inputHandler),
      ul(id:= "list", children <-- state)
    )


    OutWatch.render(document.getElementById("app"),vtree)
  }

  def TextFieldComponent(labelText: String, outputStream: Subject[String]) = {

    val textFieldStream = Subject[String]
    val clickStream = Subject[MouseEvent]
    val keyStream = Subject[KeyboardEvent]

    val buttonDisabled = textFieldStream
      .map(_.length < 2)
      .startWith(true)

    val enterPressed = keyStream
      .filter(_.key == "Enter")

    val confirm$ = enterPressed.merge(clickStream)
      .withLatestFromWith(textFieldStream)((_, input) => input)

    val clear$ = confirm$.mapTo("")
    outputStream <-- confirm$

    div(
      label(labelText),
      input(id:= "input", inputType := "text", inputString --> textFieldStream, keyup --> keyStream, value <-- clear$),
      button(id := "submit", click --> clickStream, disabled <-- buttonDisabled, "Submit")
    )
  }

}


