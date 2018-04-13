package outwatch

import cats.effect.Effect
import outwatch.dom.VDomModifierFactory

trait StaticVNodeRenderFactory[F[+ _]] extends VDomModifierFactory[F] {
  implicit val effectF: Effect[F]

  trait StaticVNodeRender[-T] {
    def render(value: T): F[StaticVNode]
  }

  object StaticVNodeRender {

    implicit def vNodeRender: StaticVNodeRender[VNode] = {
      (value: VNode) => value
    }

    implicit def optionRender[T](implicit svnr: StaticVNodeRender[T]): StaticVNodeRender[Option[T]] = {
      (value: Option[T]) => value.fold(effectF.pure(StaticVNode.empty))(svnr.render _)
    }

    implicit object StringRender extends StaticVNodeRender[String] {
      def render(value: String): F[StaticVNode] = effectF.pure(StringVNode(value))
    }

    implicit object IntRender extends StaticVNodeRender[Int] {
      def render(value: Int): F[StaticVNode] = effectF.pure(StringVNode(value.toString))
    }

    implicit object DoubleRender extends StaticVNodeRender[Double] {
      def render(value: Double): F[StaticVNode] = effectF.pure(StringVNode(value.toString))
    }

  }

}
