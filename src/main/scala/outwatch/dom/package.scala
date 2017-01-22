package outwatch

import org.scalajs.dom.raw.KeyboardEvent
import org.scalajs.dom.MouseEvent
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


  lazy val hidden = BoolAttributeBuilder("hidden")
  lazy val value = new AttributeBuilder[Any]("value")
  lazy val disabled = BoolAttributeBuilder("disabled")
  lazy val style = new AttributeBuilder[Any]("style")
  lazy val alt = new AttributeBuilder[String]("alt")
  lazy val href = new AttributeBuilder[String]("href")
  lazy val autocomplete = new AttributeBuilder[Any]("autocomplete")
  lazy val autofocus = new AttributeBuilder[Any]("autofocus")
  lazy val charset = new AttributeBuilder[Any]("charset")
  lazy val cols = new AttributeBuilder[Double]("cols")
  lazy val rows = new AttributeBuilder[Double]("rows")
  lazy val colspan = new AttributeBuilder[Any]("colspan")
  lazy val rowspan = new AttributeBuilder[Any]("rowspan")
  lazy val download = new AttributeBuilder[Any]("download")
  lazy val id = new AttributeBuilder[String]("id")
  lazy val max = new AttributeBuilder[Double]("max")
  lazy val min = new AttributeBuilder[Double]("min")
  lazy val name = new AttributeBuilder[String]("name")
  lazy val accept = new AttributeBuilder[Any]("accept")
  lazy val src = new AttributeBuilder[Any]("src")
  lazy val srcset = new AttributeBuilder[Any]("srcset")
  lazy val checked = BoolAttributeBuilder("checked")
  lazy val coords = new AttributeBuilder[Any]("coords")
  lazy val data = new DynamicAttributeBuilder[Any](List("data"))
  lazy val list = new AttributeBuilder[Any]("list")
  lazy val multiple = new AttributeBuilder[Any]("multiple")
  lazy val datetime = new AttributeBuilder[Any]("datetime")
  lazy val placeholder = new AttributeBuilder[String]("placeholder")
  lazy val radiogroup = new AttributeBuilder[Any]("radiogroup")
  lazy val readonly = BoolAttributeBuilder("readonly")
  lazy val required = BoolAttributeBuilder("required")
  lazy val reversed = new AttributeBuilder[Any]("reversed")
  lazy val scope = new AttributeBuilder[Any]("scope")
  lazy val selected = new AttributeBuilder[Any]("selected")
  lazy val size = new AttributeBuilder[Any]("size")
  lazy val sizes = new AttributeBuilder[Any]("sizes")
  lazy val step = new AttributeBuilder[Double]("step")
  lazy val summary = new AttributeBuilder[Any]("summary")
  lazy val target = new AttributeBuilder[Any]("target")
  lazy val usemap = new AttributeBuilder[Any]("usemap")
  lazy val wrap = new AttributeBuilder[Any]("wrap")
  lazy val `type` = new AttributeBuilder[Any]("type")
  lazy val role = new AttributeBuilder[String]("role")
  lazy val tpe = `type`
  lazy val inputType = `type`
  lazy val className = new AttributeBuilder[String]("class")
  lazy val `class` = className
  lazy val cls = className
  lazy val forLabel = new AttributeBuilder[Any]("for")
  lazy val `for` = forLabel

  lazy val click = new MouseEventEmitterBuilder("click")
  lazy val resize = new MouseEventEmitterBuilder("resize")
  lazy val mousedown = new MouseEventEmitterBuilder("mousedown")
  lazy val mouseover = new MouseEventEmitterBuilder("mouseover")
  lazy val mouseenter = new MouseEventEmitterBuilder("mouseenter")
  lazy val mousemove = new MouseEventEmitterBuilder("mousemove")
  lazy val mouseleave = new MouseEventEmitterBuilder("mouseleave")
  lazy val input = new InputEventEmitterBuilder("input")
  lazy val change = new InputEventEmitterBuilder("change")
  lazy val blur = new InputEventEmitterBuilder("blur")
  lazy val keydown = new KeyEventEmitterBuilder("keydown")
  lazy val keyup = new KeyEventEmitterBuilder("keyup")
  lazy val keypress = new KeyEventEmitterBuilder("keypress")
  lazy val inputString = new StringEventEmitterBuilder("input")
  lazy val inputBool = new BoolEventEmitterBuilder("change")
  lazy val inputNumber = new NumberEventEmitterBuilder("input")

  lazy val update = new UpdateHookBuilder()
  lazy val insert = new InsertHookBuilder()
  lazy val destroy = new DestroyHookBuilder()

  lazy val child = new ChildStreamReceiverBuilder()

  lazy val children = new ChildrenStreamReceiverBuilder()

  def createInputHandler() = createHandler[InputEvent]
  def createMouseHandler() = createHandler[MouseEvent]
  def createKeyboardHandler() = createHandler[KeyboardEvent]
  def createStringHandler() = createHandler[String]
  def createBoolHandler() = createHandler[Boolean]
  def createNumberHandler() = createHandler[Double]



  def createHandler[T]: Observable[T] with Sink[T] = {
    Sink.createHandler[T]
  }




}
