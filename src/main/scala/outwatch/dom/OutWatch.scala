package outwatch.dom

import cats.effect.IO
import org.scalajs.dom._
import outwatch.dom.helpers.DomUtils

object OutWatch {
  def render(querySelector: String, vNode: VNode): IO[Unit] =
    DomUtils.render(document.querySelector(querySelector), vNode)
}
