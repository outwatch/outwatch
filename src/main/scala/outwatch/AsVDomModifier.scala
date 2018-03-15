package outwatch

import cats.effect.{Effect, Sync}
import outwatch.dom.{ChildStreamReceiver, ChildrenStreamReceiver, CompositeModifier, Observable, StringModifier, VDomModifierF, VNodeF}

trait AsVDomModifier[F[+_], -T] {
  def asVDomModifier(value: T)(implicit F: Effect[F]): VDomModifierF[F]
}

object AsVDomModifier {

  implicit def seqModifier[F[+_], A](implicit vm: AsVDomModifier[F, A]) = new AsVDomModifier[F, Seq[A]] {
    import cats.implicits._
    override def asVDomModifier(value: Seq[A])(implicit F: Effect[F]): VDomModifierF[F] =
      value.toList.traverse(vm.asVDomModifier).map(CompositeModifier)
  }

  implicit def optionModifier[F[+_], A](implicit vm: AsVDomModifier[F, A]) = new AsVDomModifier[F, Option[A]] {
    def asVDomModifier(value: Option[A])(implicit F: Effect[F]): VDomModifierF[F] =
      value.map(vm.asVDomModifier).getOrElse(VDomModifierF.empty[F])
  }

  implicit def vDomModifierAsVDomModifier[F[+_]: Sync]: AsVDomModifier[F, VDomModifierF[F]] =
    new AsVDomModifier[F, VDomModifierF[F]] {
      def asVDomModifier(value: VDomModifierF[F])(implicit F: Effect[F]): VDomModifierF[F] = value
    }

  implicit def stringAsVDomModifier[F[+_]] = new AsVDomModifier[F, String] {
    def asVDomModifier(value: String)(implicit F: Effect[F]): VDomModifierF[F] = F.pure(StringModifier(value))
  }

  implicit def IntAsVDomModifier[F[+_]] = new AsVDomModifier[F, Int] {
    def asVDomModifier(value: Int)(implicit F: Effect[F]): VDomModifierF[F] = F.pure(StringModifier(value.toString))
  }

  implicit def DoubleAsVDomModifier[F[+_]] = new AsVDomModifier[F, Double] {
    def asVDomModifier(value: Double)(implicit F: Effect[F]): VDomModifierF[F] = F.pure(StringModifier(value.toString))
  }

  implicit def observableRender[F[+_]] = new AsVDomModifier[F, Observable[VNodeF[F]]] {

    def asVDomModifier(valueStream: Observable[VNodeF[F]])(implicit F: Effect[F]): VDomModifierF[F] =
      F.pure(ChildStreamReceiver[F](valueStream))
  }

  implicit def observableRender[F[+_], T: StaticVNodeRender]: AsVDomModifier[F, Observable[T]] =
    new AsVDomModifier[F, Observable[T]] {
      def asVDomModifier(valueStream: Observable[T])(implicit F: Effect[F]): VDomModifierF[F] = F.pure(
        ChildStreamReceiver[F](valueStream.map(implicitly[StaticVNodeRender[T]].render[F]))
      )
    }

  implicit def observableSeqRender[F[+_]] = new AsVDomModifier[F, Observable[Seq[VNodeF[F]]]] {
    def asVDomModifier(seqStream: Observable[Seq[VNodeF[F]]])(implicit F: Effect[F]): VDomModifierF[F] =
      F.pure(ChildrenStreamReceiver[F](seqStream))
  }

  implicit def observableSeqRender[F[+_], T: StaticVNodeRender] = new AsVDomModifier[F, Observable[Seq[T]]] {
    def asVDomModifier(seqStream: Observable[Seq[T]])(implicit F: Effect[F]): VDomModifierF[F] =
      F.pure(ChildrenStreamReceiver[F](seqStream.map(_.map(implicitly[StaticVNodeRender[T]].render[F]))))
  }

}