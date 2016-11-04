package outwatch.dom

import org.scalajs.dom._
import outwatch.dom.helpers.DomUtils

object OutWatch {
  def render(element: Element, vNode: VNode): Unit = DomUtils.render(element, vNode)
}
