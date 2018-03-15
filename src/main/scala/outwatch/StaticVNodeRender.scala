package outwatch

import cats.Applicative
import cats.effect.Effect
import outwatch.dom.{StaticVNode, StringVNode}

trait StaticVNodeRender[-T] {
  def render[F[+_]: Effect](value: T): F[StaticVNode]
}

object StaticVNodeRender {

  implicit def optionRender[T](implicit svnr: StaticVNodeRender[T]): StaticVNodeRender[Option[T]] =
    new StaticVNodeRender[Option[T]] {
      def render[F[+_]: Effect](value: Option[T]): F[StaticVNode] =
        value.fold(Applicative[F].pure(StaticVNode.empty))(svnr.render[F])
    }

  implicit object StringRender extends StaticVNodeRender[String] {
    def render[F[+_]: Effect](value: String): F[StaticVNode] =
      Applicative[F].pure(StringVNode(value))
  }

  implicit object IntRender extends StaticVNodeRender[Int] {
    def render[F[+_]: Effect](value: Int): F[StaticVNode] =
      Applicative[F].pure(StringVNode(value.toString))
  }

  implicit object DoubleRender extends StaticVNodeRender[Double] {
    def render[F[+_]: Effect](value: Double): F[StaticVNode] =
      Applicative[F].pure(StringVNode(value.toString))
  }
}
