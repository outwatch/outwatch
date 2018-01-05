package outwatch

import cats.effect.Effect
import outwatch.dom.{VDomModifier_, StringModifier}

trait ValueModifier[T] {
  def asModifier[F[_]:Effect](value: T): F[VDomModifier_]
}

object ValueModifier {

  implicit object StringRenderer extends ValueModifier[String] {
    def asModifier[F[_]:Effect](value: String): F[VDomModifier_] = Effect[F].pure(StringModifier(value))
  }

  implicit object IntRenderer extends ValueModifier[Int] {
    def asModifier[F[_]:Effect](value: Int): F[VDomModifier_] = Effect[F].pure(StringModifier(value.toString))
  }

  implicit object DoubleRenderer extends ValueModifier[Double] {
    def asModifier[F[_]:Effect](value: Double): F[VDomModifier_] = Effect[F].pure(StringModifier(value.toString))
  }

}
