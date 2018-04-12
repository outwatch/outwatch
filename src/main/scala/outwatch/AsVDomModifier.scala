package outwatch

import cats.effect.Effect
import cats.implicits._
import outwatch.dom.VDomModifierFactory

trait AsVDomModifierInstances[F[+_]] extends VDomModifierFactory[F] with StaticVNodeRenderFactory[F] {
  implicit val effectF: Effect[F]

  trait AsVDomModifier[-T] {
    def asVDomModifier(value: T): VDomModifierF
  }

  //TODO: convert to SAM?

  implicit def seqModifier[A](implicit vm: AsVDomModifier[A]) = new AsVDomModifier[Seq[A]] {
    override def asVDomModifier(value: Seq[A]): VDomModifierF =
      value.toList.traverse(vm.asVDomModifier).map(CompositeModifier)
  }

  implicit def optionModifier[A](implicit vm: AsVDomModifier[A]) = new AsVDomModifier[Option[A]] {
    def asVDomModifier(value: Option[A]): VDomModifierF =
      value.map(vm.asVDomModifier).getOrElse(VDomModifierF.empty)
  }

  implicit def vDomModifierAsVDomModifier: AsVDomModifier[VDomModifierF] =
    new AsVDomModifier[VDomModifierF] {
      def asVDomModifier(value: VDomModifierF): VDomModifierF = value
    }

  implicit def stringAsVDomModifier = new AsVDomModifier[String] {
    def asVDomModifier(value: String): VDomModifierF = effectF.pure(StringModifier(value))
  }

  implicit def IntAsVDomModifier = new AsVDomModifier[Int] {
    def asVDomModifier(value: Int): VDomModifierF = effectF.pure(StringModifier(value.toString))
  }

  implicit def DoubleAsVDomModifier = new AsVDomModifier[Double] {
    def asVDomModifier(value: Double): VDomModifierF = effectF.pure(StringModifier(value.toString))
  }

  implicit def observableRender = new AsVDomModifier[Observable[VNodeF]] {

    def asVDomModifier(valueStream: Observable[VNodeF]): VDomModifierF =
      effectF.pure(ChildStreamReceiver(valueStream))
  }

  implicit def observableRender[T](implicit staticVNodeRender: StaticVNodeRender[T]): AsVDomModifier[Observable[T]] =
    new AsVDomModifier[Observable[T]] {
      def asVDomModifier(valueStream: Observable[T]): VDomModifierF = effectF.pure(
        ChildStreamReceiver(valueStream.map(staticVNodeRender.render))
      )
    }

  implicit def observableSeqRender = new AsVDomModifier[Observable[Seq[VNodeF]]] {
    def asVDomModifier(seqStream: Observable[Seq[VNodeF]]): VDomModifierF =
      effectF.pure(ChildrenStreamReceiver(seqStream))
  }

  implicit def observableSeqRender[T](implicit staticVNodeRender: StaticVNodeRender[T]) = new AsVDomModifier[Observable[Seq[T]]] {
    def asVDomModifier(seqStream: Observable[Seq[T]]): VDomModifierF =
      effectF.pure(ChildrenStreamReceiver(seqStream.map(_.map(staticVNodeRender.render))))
  }

}
