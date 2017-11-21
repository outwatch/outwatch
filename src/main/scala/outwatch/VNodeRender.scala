package outwatch

import cats.effect.IO
import outwatch.dom.{StringNode, VNode}

trait VNodeRender[T] {
  def render(value: T): VNode
}

object VNodeRender {

  implicit object StringRenderer extends VNodeRender[String] {
    def render(value: String): VNode = IO.pure(StringNode(value))
  }

  implicit object IntRenderer extends VNodeRender[Int] {
    def render(value: Int): VNode = IO.pure(StringNode(value.toString))
  }

  implicit object DoubleRenderer extends VNodeRender[Double] {
    def render(value: Double): VNode = IO.pure(StringNode(value.toString))
  }

}