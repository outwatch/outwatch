package outwatch

import cats.effect.IO
import outwatch.dom.{CompositeModifier, ModifierStreamReceiver, Observable, StringModifier, VDomModifier}

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
    def asVDomModifier(value: String): VDomModifier = IO.pure(StringModifier(value))
  }

  implicit object IntAsVDomModifier extends AsVDomModifier[Int] {
    def asVDomModifier(value: Int): VDomModifier = IO.pure(StringModifier(value.toString))
  }

  implicit object DoubleAsVDomModifier extends AsVDomModifier[Double] {
    def asVDomModifier(value: Double): VDomModifier = IO.pure(StringModifier(value.toString))
  }

  implicit object ObservableRender extends AsVDomModifier[Observable[VDomModifier]] {
    def asVDomModifier(valueStream: Observable[VDomModifier]): VDomModifier = IO.pure(ModifierStreamReceiver(valueStream))
  }

  implicit def observableRender[T : AsVDomModifier]: AsVDomModifier[Observable[T]] = (valueStream: Observable[T]) => IO.pure(
    ModifierStreamReceiver(valueStream.map(implicitly[AsVDomModifier[T]].asVDomModifier))
  )

  implicit object ObservableSeqRender extends AsVDomModifier[Observable[Seq[VDomModifier]]] {
    def asVDomModifier(seqStream: Observable[Seq[VDomModifier]]): VDomModifier = IO.pure(ModifierStreamReceiver(seqStream.map[VDomModifier](x => x)))
  }

  implicit def observableSeqRender[T : AsVDomModifier]: AsVDomModifier[Observable[Seq[T]]] = (seqStream: Observable[Seq[T]]) => IO.pure(
    ModifierStreamReceiver(seqStream.map[VDomModifier](_.map(implicitly[AsVDomModifier[T]].asVDomModifier)))
  )

}