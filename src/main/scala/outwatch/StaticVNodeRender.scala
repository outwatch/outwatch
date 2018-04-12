package outwatch

import cats.Applicative
import cats.effect.Effect
import outwatch.dom.VDomModifierFactory

trait StaticVNodeRender[F[+_], -T, StaticVNode] {
  def render(value: T): F[StaticVNode]
}

trait StaticVNodeRenderFactory[F[+_]] extends VDomModifierFactory[F] {
  implicit val effectF:Effect[F]

  object StaticVNodeRender {

    implicit def optionRender[T](implicit svnr: StaticVNodeRender[F, T, StaticVNode]): StaticVNodeRender[F, Option[T], StaticVNode] =
      new StaticVNodeRender[F, Option[T], StaticVNode] {
        def render(value: Option[T]): F[StaticVNode] =
          value.fold(effectF.pure(StaticVNode.empty))(svnr.render)
      }

    implicit object StringRender extends StaticVNodeRender[F, String, StaticVNode] {
      def render(value: String): F[StaticVNode] =
        effectF.pure(StringVNode(value))
    }

    implicit object IntRender extends StaticVNodeRender[F, Int, StaticVNode] {
      def render(value: Int): F[StaticVNode] =
        effectF.pure(StringVNode(value.toString))
    }

    implicit object DoubleRender extends StaticVNodeRender[F, Double, StaticVNode] {
      def render(value: Double): F[StaticVNode] =
        effectF.pure(StringVNode(value.toString))
    }

  }
}
