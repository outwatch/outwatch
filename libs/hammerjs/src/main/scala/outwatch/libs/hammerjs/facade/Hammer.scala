package outwatch.libs.hammerjs.facade

import org.scalajs.dom
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.annotation._

import com.github.ghik.silencer.silent

// https://hammerjs.github.io/getting-started/
@js.native
@JSImport("hammerjs", JSImport.Namespace)
@silent("never used|dead code")
class Hammer[EVENT <: Event](element: html.Element, options: js.UndefOr[Options] = js.undefined) extends js.Object {
  def on(events: String, handler: js.Function1[EVENT,Unit]):Unit = js.native
  def destroy():Unit = js.native
  def stop():Unit = js.native
  var domEvents:Boolean = js.native
}

trait Options extends js.Object {
  var cssProps:js.UndefOr[CssProps] = js.undefined
}

trait CssProps extends js.Object {
  var userSelect:js.UndefOr[String] = js.undefined
}

trait Event extends js.Object {
  def target:html.Element
  def srcEvent:dom.Event
}

trait PropagatedHammer extends Hammer[PropagatingEvent] {
  def hammer:Hammer[Event]
}



// Hammer.js does not natively support event propagation. This wrapper fixes it.
// https://github.com/hammerjs/hammer.js/issues/807
// https://github.com/josdejong/propagating-hammerjs
@js.native
@JSImport("propagating-hammerjs", JSImport.Namespace)
object propagating extends js.Function1[Hammer[Event], PropagatedHammer] {
  override def apply(arg1: Hammer[Event]): PropagatedHammer = js.native
}

trait PropagatingEvent extends Event {
  def stopPropagation():Unit
  def firstTarget:html.Element
}
