package outwatch

import cats.effect.Effect
import outwatch.dom.{StaticVNode, StringVNode, VNode}

trait StaticVNodeRender[T] {
  def render[F[_]:Effect](value: T): F[StaticVNode]
}

object StaticVNodeRender {

  implicit object VNodeRender extends StaticVNodeRender[VNode] {
    def render[F[+_]:Effect](value: F[dom.VTree]): F[StaticVNode] = value
  }

  implicit object StringRender extends StaticVNodeRender[String] {
    def render[F[_]:Effect](value: String): F[StaticVNode] = Effect[F].pure(StringVNode(value))
  }

  implicit object IntRender extends StaticVNodeRender[Int] {
    def render[F[_]:Effect](value: Int): F[StaticVNode] = Effect[F].pure(StringVNode(value.toString))
  }

  implicit object DoubleRender extends StaticVNodeRender[Double] {
    def render[F[_]:Effect](value: Double): F[StaticVNode] = Effect[F].pure(StringVNode(value.toString))
  }

}
