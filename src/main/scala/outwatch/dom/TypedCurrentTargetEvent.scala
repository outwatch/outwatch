package outwatch.dom

import scala.scalajs.js
import org.scalajs.dom

/** Represents an event for which we know what exact type the `currentTarget` is. */
@js.native
trait TypedCurrentTargetEvent[+E <: dom.EventTarget] extends dom.Event {
  override def currentTarget: E = js.native
}
