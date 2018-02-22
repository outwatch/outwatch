package outwatch

import cats.effect.IO
import outwatch.dom.{ChildStreamReceiver, ChildrenStreamReceiver, CompositeModifier, Observable, StringModifier, VDomModifier, VNode}

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

  implicit object ObservableRender extends AsVDomModifier[Observable[VNode]] {
    def asVDomModifier(valueStream: Observable[VNode]): VDomModifier = IO.pure(ChildStreamReceiver(valueStream))
  }

  implicit def observableRender[T : StaticVNodeRender]: AsVDomModifier[Observable[T]] = (valueStream: Observable[T]) => IO.pure(
    ChildStreamReceiver(valueStream.map(implicitly[StaticVNodeRender[T]].render))
  )

  implicit object ObservableSeqRender extends AsVDomModifier[Observable[Seq[VNode]]] {
    def asVDomModifier(seqStream: Observable[Seq[VNode]]): VDomModifier = IO.pure(ChildrenStreamReceiver(seqStream))
  }

  implicit def observableSeqRender[T : StaticVNodeRender]: AsVDomModifier[Observable[Seq[T]]] = (seqStream: Observable[Seq[T]]) => IO.pure(
    ChildrenStreamReceiver(seqStream.map(_.map(implicitly[StaticVNodeRender[T]].render)))
  )

}