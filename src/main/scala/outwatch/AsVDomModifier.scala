package outwatch

import cats.effect.IO
import cats.syntax.functor._
import monix.reactive.Observable
import outwatch.dom.{AsValueObservable, CompositeModifier, ModifierStreamReceiver, StringVNode, VDomModifier, ValueObservable}

trait AsVDomModifier[-T] {
  def asVDomModifier(value: T): VDomModifier
}

object AsVDomModifier {

  implicit def seqModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[Seq[T]] =
    (value: Seq[T]) => value.map(vm.asVDomModifier).sequence.map(CompositeModifier)

  implicit def optionModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[Option[T]] =
    (value: Option[T]) => value.map(vm.asVDomModifier) getOrElse VDomModifier.empty

  implicit object VDomModifierAsVDomModifier extends AsVDomModifier[VDomModifier] {
    def asVDomModifier(value: VDomModifier): VDomModifier = value
  }

  implicit object StringAsVDomModifier extends AsVDomModifier[String] {
    def asVDomModifier(value: String): VDomModifier = IO.pure(StringVNode(value))
  }

  implicit object IntAsVDomModifier extends AsVDomModifier[Int] {
    def asVDomModifier(value: Int): VDomModifier = IO.pure(StringVNode(value.toString))
  }

  implicit object DoubleAsVDomModifier extends AsVDomModifier[Double] {
    def asVDomModifier(value: Double): VDomModifier = IO.pure(StringVNode(value.toString))
  }

  implicit object LongAsVDomModifier extends AsVDomModifier[Long] {
    def asVDomModifier(value: Long): VDomModifier = IO.pure(StringVNode(value.toString))
  }

  implicit object BooleanAsVDomModifier extends AsVDomModifier[Boolean] {
    def asVDomModifier(value: Boolean): VDomModifier = IO.pure(StringVNode(value.toString))
  }

  implicit def observableRender[T : AsVDomModifier]: AsVDomModifier[Observable[T]] = (valueStream: Observable[T]) => IO.pure(
    ModifierStreamReceiver(ValueObservable(valueStream).map(VDomModifier(_)))
  )

  implicit def valueObservableRender[T : AsVDomModifier, F[_] : AsValueObservable]: AsVDomModifier[F[T]] = (valueStream: F[T]) => IO.pure(
    ModifierStreamReceiver(ValueObservable(valueStream).map(VDomModifier(_)))
  )
}
