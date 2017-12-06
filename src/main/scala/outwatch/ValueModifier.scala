package outwatch

import cats.effect.IO
import outwatch.dom.{VDomModifier_, StringModifier}

trait ValueModifier[T] {
  def asModifier(value: T): IO[VDomModifier_]
}

object ValueModifier {

  implicit object StringRenderer extends ValueModifier[String] {
    def asModifier(value: String): IO[VDomModifier_] = IO.pure(StringModifier(value))
  }

  implicit object IntRenderer extends ValueModifier[Int] {
    def asModifier(value: Int): IO[VDomModifier_] = IO.pure(StringModifier(value.toString))
  }

  implicit object DoubleRenderer extends ValueModifier[Double] {
    def asModifier(value: Double): IO[VDomModifier_] = IO.pure(StringModifier(value.toString))
  }

}