package outwatch

import cats.effect.IO
import cats.syntax.functor._
import outwatch.dom.{AsValueObservable, ChildCommand, CompositeModifier, EffectModifier, ModifierStreamReceiver, StringVNode, VDomModifier, ValueObservable}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

trait AsVDomModifier[-T] {
  def asVDomModifier(value: T): VDomModifier
}

object AsVDomModifier {

  implicit def jsArrayModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[js.Array[T]] =
    (value: js.Array[T]) => CompositeModifier(value.map(v => vm.asVDomModifier(v)))

  implicit def seqModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[Seq[T]] =
    (value: Seq[T]) => CompositeModifier(value.map(v => vm.asVDomModifier(v)).toJSArray)

  implicit def optionModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[Option[T]] =
    (value: Option[T]) => value.fold(VDomModifier.empty)(vm.asVDomModifier)

  implicit object VDomModifierAsVDomModifier extends AsVDomModifier[VDomModifier] {
    def asVDomModifier(value: VDomModifier): VDomModifier = value
  }

  implicit object StringAsVDomModifier extends AsVDomModifier[String] {
    def asVDomModifier(value: String): VDomModifier = StringVNode(value)
  }

  implicit object IntAsVDomModifier extends AsVDomModifier[Int] {
    def asVDomModifier(value: Int): VDomModifier = StringVNode(value.toString)
  }

  implicit object DoubleAsVDomModifier extends AsVDomModifier[Double] {
    def asVDomModifier(value: Double): VDomModifier = StringVNode(value.toString)
  }

  implicit object LongAsVDomModifier extends AsVDomModifier[Long] {
    def asVDomModifier(value: Long): VDomModifier = StringVNode(value.toString)
  }

  implicit object BooleanAsVDomModifier extends AsVDomModifier[Boolean] {
    def asVDomModifier(value: Boolean): VDomModifier = StringVNode(value.toString)
  }

  implicit def effectRender[T : AsVDomModifier]: AsVDomModifier[IO[T]] = (effect: IO[T]) =>
    EffectModifier(effect.map(VDomModifier(_)))

  implicit def valueObservableRender[T : AsVDomModifier, F[_] : AsValueObservable]: AsVDomModifier[F[T]] = (valueStream: F[T]) =>
    ModifierStreamReceiver(ValueObservable(valueStream).map(VDomModifier(_)))

  implicit def childCommandObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[ChildCommand]] = (valueStream: F[ChildCommand]) =>
    ModifierStreamReceiver(ChildCommand.stream(ValueObservable(valueStream).map(Seq(_))))

  implicit def childCommandSeqObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[Seq[ChildCommand]]] = (valueStream: F[Seq[ChildCommand]]) =>
    ModifierStreamReceiver(ChildCommand.stream(ValueObservable(valueStream)))
}
