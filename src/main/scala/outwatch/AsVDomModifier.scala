package outwatch

import cats.effect.Effect
import outwatch.dom.{ChildStreamReceiver, ChildrenStreamReceiver, CompositeModifier, Observable, StringModifier, VDomModifierF, VNodeF}
import cats.implicits._

trait AsVDomModifier[F[+_], -T] {
  def asVDomModifier(value: T): VDomModifierF[F]
}

trait AsVDomModifierInstances[F[+_]] {
  implicit val effectF: Effect[F]

  implicit def seqModifier[A](implicit vm: AsVDomModifier[F, A]) = new AsVDomModifier[F, Seq[A]] {
    override def asVDomModifier(value: Seq[A]): VDomModifierF[F] =
      value.toList.traverse(vm.asVDomModifier).map(CompositeModifier)
  }

  implicit def optionModifier[A](implicit vm: AsVDomModifier[F, A]) = new AsVDomModifier[F, Option[A]] {
    def asVDomModifier(value: Option[A]): VDomModifierF[F] =
      value.map(vm.asVDomModifier).getOrElse(VDomModifierF.empty[F])
  }

  implicit def vDomModifierAsVDomModifier: AsVDomModifier[F, VDomModifierF[F]] =
    new AsVDomModifier[F, VDomModifierF[F]] {
      def asVDomModifier(value: VDomModifierF[F]): VDomModifierF[F] = value
    }

  implicit def stringAsVDomModifier = new AsVDomModifier[F, String] {
    def asVDomModifier(value: String): VDomModifierF[F] = effectF.pure(StringModifier(value))
  }

  implicit def IntAsVDomModifier = new AsVDomModifier[F, Int] {
    def asVDomModifier(value: Int): VDomModifierF[F] = effectF.pure(StringModifier(value.toString))
  }

  implicit def DoubleAsVDomModifier = new AsVDomModifier[F, Double] {
    def asVDomModifier(value: Double): VDomModifierF[F] = effectF.pure(StringModifier(value.toString))
  }

  implicit def observableRender = new AsVDomModifier[F, Observable[VNodeF[F]]] {

    def asVDomModifier(valueStream: Observable[VNodeF[F]]): VDomModifierF[F] =
      effectF.pure(ChildStreamReceiver[F](valueStream))
  }

  implicit def observableRender[T: StaticVNodeRender]: AsVDomModifier[F, Observable[T]] =
    new AsVDomModifier[F, Observable[T]] {
      def asVDomModifier(valueStream: Observable[T]): VDomModifierF[F] = effectF.pure(
        ChildStreamReceiver[F](valueStream.map(implicitly[StaticVNodeRender[T]].render[F]))
      )
    }

  implicit def observableSeqRender = new AsVDomModifier[F, Observable[Seq[VNodeF[F]]]] {
    def asVDomModifier(seqStream: Observable[Seq[VNodeF[F]]]): VDomModifierF[F] =
      effectF.pure(ChildrenStreamReceiver[F](seqStream))
  }

  implicit def observableSeqRender[T: StaticVNodeRender] = new AsVDomModifier[F, Observable[Seq[T]]] {
    def asVDomModifier(seqStream: Observable[Seq[T]]): VDomModifierF[F] =
      effectF.pure(ChildrenStreamReceiver[F](seqStream.map(_.map(implicitly[StaticVNodeRender[T]].render[F]))))
  }

}
