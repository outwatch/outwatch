package outwatch.dom.helpers

import org.scalajs.dom.Event
import org.scalajs.dom.html

import scala.scalajs.js.annotation.ScalaJSDefined

@ScalaJSDefined
class InputEvent() extends Event {
  override def target = {
    super.target.asInstanceOf[html.Input]
  }
}
