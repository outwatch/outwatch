package outwatch

import cats.effect.Effect
import cats.implicits._
import outwatch.dom.VDomModifierFactory

trait AsVDomModifierFactory[F[+ _]] extends VDomModifierFactory[F] with StaticVNodeRenderFactory[F] {
  implicit val effectF: Effect[F]

  trait AsVDomModifier[-T] {
    def asVDomModifier(value: T): VDomModifier
  }

  object AsVDomModifier {

    implicit def seqModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[Seq[T]] =
      (value: Seq[T]) => value.toList.map(vm.asVDomModifier).sequence.map(CompositeModifier)

    implicit def optionModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[Option[T]] =
      (value: Option[T]) => value.fold(VDomModifier.empty)(vm.asVDomModifier)

    implicit object VDomModifierAsVDomModifier extends AsVDomModifier[VDomModifier] {
      def asVDomModifier(value: VDomModifier): VDomModifier = value
    }

    implicit object StringAsVDomModifier extends AsVDomModifier[String] {
      def asVDomModifier(value: String): VDomModifier = effectF.pure(StringModifier(value))
    }

    implicit object IntAsVDomModifier extends AsVDomModifier[Int] {
      def asVDomModifier(value: Int): VDomModifier = effectF.pure(StringModifier(value.toString))
    }

    implicit object DoubleAsVDomModifier extends AsVDomModifier[Double] {
      def asVDomModifier(value: Double): VDomModifier = effectF.pure(StringModifier(value.toString))
    }

    implicit object ObservableRender extends AsVDomModifier[Observable[VNode]] {
      def asVDomModifier(valueStream: Observable[VNode]): VDomModifier = effectF.pure(ChildStreamReceiver(valueStream))
    }

    implicit def observableRender[T: StaticVNodeRender]: AsVDomModifier[Observable[T]] = (valueStream: Observable[T]) => effectF.pure(
      ChildStreamReceiver(valueStream.map(implicitly[StaticVNodeRender[T]].render))
    )

    implicit object ObservableSeqRender extends AsVDomModifier[Observable[Seq[VNode]]] {
      def asVDomModifier(seqStream: Observable[Seq[VNode]]): VDomModifier = effectF.pure(ChildrenStreamReceiver(seqStream))
    }

    implicit def observableSeqRender[T: StaticVNodeRender]: AsVDomModifier[Observable[Seq[T]]] = (seqStream: Observable[Seq[T]]) => effectF.pure(
      ChildrenStreamReceiver(seqStream.map(_.map(implicitly[StaticVNodeRender[T]].render)))
    )

  }

}
