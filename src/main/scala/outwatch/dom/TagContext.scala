package outwatch.dom

import org.scalajs.dom

import scala.annotation.compileTimeOnly
import scala.scalajs.js

trait TagContext[+Elem <: dom.Element]
object TagContext {
  class Assigned[Elem <: dom.Element] extends TagContext[Elem]

  @compileTimeOnly(MacroMessages.error)
  implicit def DummyUnassignedTagContext: TagContext[DummyElement] = ???

  @compileTimeOnly(MacroMessages.error)
  implicit def AnyUnassignedTagContext[Elem <: dom.Element]: TagContext[Elem] = ???

  @compileTimeOnly("Events can only be used in arguments of the VTree.apply method. Otherwise, you need to provide an implicit TagContext or use e.g. onClick.onElement[Element] --> sink.")
  @js.native
  class DummyElement extends dom.Element
  object DummyElement {
    import dom.html

    // corresponds to all non-obsolete elements from: https://github.com/scala-js/scala-js-dom/blob/master/src/main/scala/org/scalajs/dom/html.scala
    @compileTimeOnly(MacroMessages.error)
    type AllElements = html.Anchor with html.Audio with html.Area with html.AreasCollection with html.Base with html.BlockElement with html.Body with html.Button with html.BR with html.Canvas with html.Collection with html.DataList with html.DD with html.Div with html.DList with html.DT with html.Document with html.Element with html.Embed with html.FieldSet with html.Form with html.Head with html.Heading with html.Html with html.HR with html.IFrame with html.Image with html.Input with html.Label with html.Legend with html.LI with html.Link with html.Map with html.Media with html.Meta with html.Mod with html.Object with html.OList with html.OptGroup with html.Option with html.Paragraph with html.Param with html.Pre with html.Phrase with html.Progress with html.Quote with html.Script with html.Select with html.Source with html.Span with html.Style with html.Table with html.TableAlignment with html.TableCaption with html.TableCell with html.TableCol with html.TableDataCell with html.TableHeaderCell with html.TableRow with html.TableSection with html.Title with html.TextArea with html.Track with html.UList with html.Unknown with html.Video

    @compileTimeOnly(MacroMessages.error)
    implicit def DummyWithProperties(dummy: DummyElement): AllElements = ???
    @compileTimeOnly(MacroMessages.error)
    implicit def DummyWithString: TagWithString[DummyElement] = ???
    @compileTimeOnly(MacroMessages.error)
    implicit def DummyWithNumber: TagWithNumber[DummyElement] = ???
    @compileTimeOnly(MacroMessages.error)
    implicit def DummyWithBoolean: TagWithBoolean[DummyElement] = ???
  }
}
trait TagContextImplicits {
  @compileTimeOnly(MacroMessages.error)
  implicit def DummyElementUsableEvent[Event <: dom.Event](dummy: Sink[_ <: Event with TypedCurrentTargetEvent[dom.Element]]): Sink[Event with TypedCurrentTargetEvent[TagContext.DummyElement]] = ???
}
