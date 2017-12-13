package outwatch

import cats.effect.IO
import outwatch.dom.{StaticVNode, StringVNode, VNode}

trait StaticVNodeRender[T] {
  def render(value: T): IO[StaticVNode]
}

object StaticVNodeRender {

  implicit object VNodeRender extends StaticVNodeRender[VNode] {
    def render(value: VNode): IO[StaticVNode] = value
  }

  implicit object StringRender extends StaticVNodeRender[String] {
    def render(value: String): IO[StaticVNode] = IO.pure(StringVNode(value))
  }

  implicit object IntRender extends StaticVNodeRender[Int] {
    def render(value: Int): IO[StaticVNode] = IO.pure(StringVNode(value.toString))
  }

  implicit object DoubleRender extends StaticVNodeRender[Double] {
    def render(value: Double): IO[StaticVNode] = IO.pure(StringVNode(value.toString))
  }

}
