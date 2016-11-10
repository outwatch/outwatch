package outwatch

import org.scalajs.dom.raw.KeyboardEvent
import org.scalajs.dom.{MouseEvent, Event}
import rxscalajs.facade.SubjectFacade
import rxscalajs.{Observable, Subject}
import outwatch.dom.helpers._

/**
  *
  */
package object dom {
  def div(args: VDomModifier*) = DomUtils.hyperscriptHelper("div")(args: _*)
  def span(args: VDomModifier*) = DomUtils.hyperscriptHelper("span")(args: _*)
  def h1(args: VDomModifier*) = DomUtils.hyperscriptHelper("h1")(args: _*)
  def button(args: VDomModifier*) = DomUtils.hyperscriptHelper("button")(args: _*)
  def a(args: VDomModifier*) = DomUtils.hyperscriptHelper("a")(args: _*)
  def label(args: VDomModifier*) = DomUtils.hyperscriptHelper("label")(args: _*)
  def input(args: VDomModifier*) = DomUtils.hyperscriptHelper("input")(args: _*)
  def hr(args: VDomModifier*) = DomUtils.hyperscriptHelper("hr")(args: _*)
  def ul(args: VDomModifier*) = DomUtils.hyperscriptHelper("ul")(args: _*)
  def abbr(args: VDomModifier*) = DomUtils.hyperscriptHelper("abbr")(args: _*)
  def address(args: VDomModifier*) = DomUtils.hyperscriptHelper("address")(args: _*)
  def area(args: VDomModifier*) = DomUtils.hyperscriptHelper("area")(args: _*)
  def article(args: VDomModifier*) = DomUtils.hyperscriptHelper("article")(args: _*)
  def aside(args: VDomModifier*) = DomUtils.hyperscriptHelper("aside")(args: _*)
  def audio(args: VDomModifier*) = DomUtils.hyperscriptHelper("audio")(args: _*)
  def b(args: VDomModifier*) = DomUtils.hyperscriptHelper("b")(args: _*)
  def base(args: VDomModifier*) = DomUtils.hyperscriptHelper("base")(args: _*)
  def bdi(args: VDomModifier*) = DomUtils.hyperscriptHelper("bdi")(args: _*)
  def blockquote(args: VDomModifier*) = DomUtils.hyperscriptHelper("blockquote")(args: _*)
  def br(args: VDomModifier*) = DomUtils.hyperscriptHelper("br")(args: _*)
  def li(args: VDomModifier*) = DomUtils.hyperscriptHelper("li")(args: _*)
  def bdo(args: VDomModifier*) = DomUtils.hyperscriptHelper("bdo")(args: _*)
  def canvas(args: VDomModifier*) = DomUtils.hyperscriptHelper("canvas")(args: _*)
  def caption(args: VDomModifier*) = DomUtils.hyperscriptHelper("caption")(args: _*)
  def cite(args: VDomModifier*) = DomUtils.hyperscriptHelper("cite")(args: _*)
  def code(args: VDomModifier*) = DomUtils.hyperscriptHelper("code")(args: _*)
  def col(args: VDomModifier*) = DomUtils.hyperscriptHelper("col")(args: _*)
  def colgroup(args: VDomModifier*) = DomUtils.hyperscriptHelper("colgroup")(args: _*)
  def datalist(args: VDomModifier*) = DomUtils.hyperscriptHelper("datalist")(args: _*)
  def dd(args: VDomModifier*) = DomUtils.hyperscriptHelper("dd")(args: _*)
  def del(args: VDomModifier*) = DomUtils.hyperscriptHelper("del")(args: _*)
  def details(args: VDomModifier*) = DomUtils.hyperscriptHelper("details")(args: _*)
  def dfn(args: VDomModifier*) = DomUtils.hyperscriptHelper("dfn")(args: _*)
  def dialog(args: VDomModifier*) = DomUtils.hyperscriptHelper("dialog")(args: _*)
  def dl(args: VDomModifier*) = DomUtils.hyperscriptHelper("dl")(args: _*)
  def dt(args: VDomModifier*) = DomUtils.hyperscriptHelper("dt")(args: _*)
  def em(args: VDomModifier*) = DomUtils.hyperscriptHelper("em")(args: _*)
  def embed(args: VDomModifier*) = DomUtils.hyperscriptHelper("embed")(args: _*)
  def fieldset(args: VDomModifier*) = DomUtils.hyperscriptHelper("fieldset")(args: _*)
  def figcaption(args: VDomModifier*) = DomUtils.hyperscriptHelper("figcaption")(args: _*)
  def figure(args: VDomModifier*) = DomUtils.hyperscriptHelper("figure")(args: _*)
  def footer(args: VDomModifier*) = DomUtils.hyperscriptHelper("footer")(args: _*)
  def form(args: VDomModifier*) = DomUtils.hyperscriptHelper("form")(args: _*)
  def header(args: VDomModifier*) = DomUtils.hyperscriptHelper("header")(args: _*)
  def h2(args: VDomModifier*) = DomUtils.hyperscriptHelper("h2")(args: _*)
  def h3(args: VDomModifier*) = DomUtils.hyperscriptHelper("h3")(args: _*)
  def h4(args: VDomModifier*) = DomUtils.hyperscriptHelper("h4")(args: _*)
  def h5(args: VDomModifier*) = DomUtils.hyperscriptHelper("h5")(args: _*)
  def h6(args: VDomModifier*) = DomUtils.hyperscriptHelper("h6")(args: _*)
  def i(args: VDomModifier*) = DomUtils.hyperscriptHelper("i")(args: _*)
  def iframe(args: VDomModifier*) = DomUtils.hyperscriptHelper("iframe")(args: _*)
  def img(args: VDomModifier*) = DomUtils.hyperscriptHelper("img")(args: _*)
  def ins(args: VDomModifier*) = DomUtils.hyperscriptHelper("ins")(args: _*)
  def keygen(args: VDomModifier*) = DomUtils.hyperscriptHelper("keygen")(args: _*)
  def legend(args: VDomModifier*) = DomUtils.hyperscriptHelper("legend")(args: _*)
  def main(args: VDomModifier*) = DomUtils.hyperscriptHelper("main")(args: _*)
  def mark(args: VDomModifier*) = DomUtils.hyperscriptHelper("mark")(args: _*)
  def menu(args: VDomModifier*) = DomUtils.hyperscriptHelper("menu")(args: _*)
  def menuitem(args: VDomModifier*) = DomUtils.hyperscriptHelper("menuitem")(args: _*)
  def meter(args: VDomModifier*) = DomUtils.hyperscriptHelper("meter")(args: _*)
  def nav(args: VDomModifier*) = DomUtils.hyperscriptHelper("nav")(args: _*)
  def ol(args: VDomModifier*) = DomUtils.hyperscriptHelper("ol")(args: _*)
  def optgroup(args: VDomModifier*) = DomUtils.hyperscriptHelper("optgroup")(args: _*)
  def option(args: VDomModifier*) = DomUtils.hyperscriptHelper("option")(args: _*)
  def output(args: VDomModifier*) = DomUtils.hyperscriptHelper("output")(args: _*)
  def p(args: VDomModifier*) = DomUtils.hyperscriptHelper("p")(args: _*)
  def param(args: VDomModifier*) = DomUtils.hyperscriptHelper("param")(args: _*)
  def pre(args: VDomModifier*) = DomUtils.hyperscriptHelper("pre")(args: _*)
  def progress(args: VDomModifier*) = DomUtils.hyperscriptHelper("progress")(args: _*)
  def section(args: VDomModifier*) = DomUtils.hyperscriptHelper("section")(args: _*)
  def select(args: VDomModifier*) = DomUtils.hyperscriptHelper("select")(args: _*)
  def small(args: VDomModifier*) = DomUtils.hyperscriptHelper("small")(args: _*)
  def strong(args: VDomModifier*) = DomUtils.hyperscriptHelper("strong")(args: _*)
  def sub(args: VDomModifier*) = DomUtils.hyperscriptHelper("sub")(args: _*)
  def summary(args: VDomModifier*) = DomUtils.hyperscriptHelper("summary")(args: _*)
  def sup(args: VDomModifier*) = DomUtils.hyperscriptHelper("sup")(args: _*)
  def table(args: VDomModifier*) = DomUtils.hyperscriptHelper("table")(args: _*)
  def tbody(args: VDomModifier*) = DomUtils.hyperscriptHelper("tbody")(args: _*)
  def td(args: VDomModifier*) = DomUtils.hyperscriptHelper("td")(args: _*)
  def textarea(args: VDomModifier*) = DomUtils.hyperscriptHelper("textarea")(args: _*)
  def tfoot(args: VDomModifier*) = DomUtils.hyperscriptHelper("tfoot")(args: _*)
  def th(args: VDomModifier*) = DomUtils.hyperscriptHelper("th")(args: _*)
  def thead(args: VDomModifier*) = DomUtils.hyperscriptHelper("thead")(args: _*)
  def time(args: VDomModifier*) = DomUtils.hyperscriptHelper("time")(args: _*)
  def tr(args: VDomModifier*) = DomUtils.hyperscriptHelper("tr")(args: _*)
  def track(args: VDomModifier*) = DomUtils.hyperscriptHelper("track")(args: _*)
  def video(args: VDomModifier*) = DomUtils.hyperscriptHelper("video")(args: _*)
  def wbr(args: VDomModifier*) = DomUtils.hyperscriptHelper("wbr")(args: _*)


  lazy val hidden = AttributeBuilder("hidden")
  lazy val value = AttributeBuilder("value")
  lazy val disabled = AttributeBuilder("disabled")
  lazy val style = AttributeBuilder("style")
  lazy val alt = AttributeBuilder("alt")
  lazy val autocomplete = AttributeBuilder("autocomplete")
  lazy val autofocus = AttributeBuilder("autofocus")
  lazy val charset = AttributeBuilder("charset")
  lazy val cols = AttributeBuilder("cols")
  lazy val rows = AttributeBuilder("rows")
  lazy val colspan = AttributeBuilder("colspan")
  lazy val rowspan = AttributeBuilder("rowspan")
  lazy val download = AttributeBuilder("download")
  lazy val id = AttributeBuilder("id")
  lazy val max = AttributeBuilder("max")
  lazy val min = AttributeBuilder("min")
  lazy val name = AttributeBuilder("name")
  lazy val accept = AttributeBuilder("accept")
  lazy val src = AttributeBuilder("src")
  lazy val srcset = AttributeBuilder("srcset")
  lazy val checked = AttributeBuilder("checked")
  lazy val coords = AttributeBuilder("coords")
  lazy val data = AttributeBuilder("data")
  lazy val list = AttributeBuilder("list")
  lazy val multiple = AttributeBuilder("multiple")
  lazy val datetime = AttributeBuilder("datetime")
  lazy val placeholder = AttributeBuilder("placeholder")
  lazy val radiogroup = AttributeBuilder("radiogroup")
  lazy val readonly = AttributeBuilder("readonly")
  lazy val reversed = AttributeBuilder("reversed")
  lazy val scope = AttributeBuilder("scope")
  lazy val selected = AttributeBuilder("selected")
  lazy val size = AttributeBuilder("size")
  lazy val sizes = AttributeBuilder("sizes")
  lazy val step = AttributeBuilder("step")
  lazy val summary = AttributeBuilder("summary")
  lazy val target = AttributeBuilder("target")
  lazy val usemap = AttributeBuilder("usemap")
  lazy val wrap = AttributeBuilder("wrap")
  lazy val `type` = AttributeBuilder("type")
  lazy val tpe = `type`
  lazy val inputType = `type`
  lazy val className = AttributeBuilder("class")
  lazy val `class` = className
  lazy val cls = className
  lazy val forLabel = AttributeBuilder("for")
  lazy val `for` = forLabel
  lazy val click = MouseEventEmitterBuilder("click")
  lazy val resize = MouseEventEmitterBuilder("resize")
  lazy val mousedown = MouseEventEmitterBuilder("mousedown")
  lazy val mouseover = MouseEventEmitterBuilder("mouseover")
  lazy val mouseenter = MouseEventEmitterBuilder("mouseenter")
  lazy val mousemove = MouseEventEmitterBuilder("mousemove")
  lazy val mouseleave = MouseEventEmitterBuilder("mouseleave")
  lazy val input = InputEventEmitterBuilder("input")
  lazy val change = InputEventEmitterBuilder("change")
  lazy val blur = InputEventEmitterBuilder("blur")
  lazy val keydown = KeyEventEmitterBuilder("keydown")
  lazy val keyup = KeyEventEmitterBuilder("keyup")
  lazy val keypress = KeyEventEmitterBuilder("keypress")
  lazy val inputString = StringEventEmitterBuilder("input")
  lazy val inputBool = BoolEventEmitterBuilder("change")
  lazy val inputNumber = NumberEventEmitterBuilder("input")

  lazy val child = ChildStreamReceiverBuilder()

  lazy val children = ChildrenStreamReceiverBuilder()

  def createInputHandler = createHandler[InputEvent]
  def createMouseHandler = createHandler[MouseEvent]
  def createKeyboardHandler = createHandler[KeyboardEvent]
  def createStringHandler = createHandler[String]
  def createBoolHandler = createHandler[Boolean]
  def createNumberHandler = createHandler[Double]


  sealed trait Sink[T] {
    def <--(observable: Observable[T]) = {
      observable.subscribe(this.asInstanceOf[Subject[T]])
    }
  }

  def createHandler[T]: Observable[T] with Sink[T] = {
    class SubjectSink extends Subject[T](new SubjectFacade) with Sink[T]
    val sink = new SubjectSink
    sink.asInstanceOf[Observable[T] with Sink[T]]
  }




}
