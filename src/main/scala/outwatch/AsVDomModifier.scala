package outwatch

import cats.effect.Effect
import cats.implicits._
import outwatch.dom.VDomModifierFactory

trait AsVDomModifier[F[+_], -T] extends VDomModifierFactory[F] {
  def asVDomModifier(value: T): VDomModifierF
}

//trait AsVDomModifierInstances[F[+_]] extends VDomModifierFactory[F] {
//  implicit val effectF: Effect[F]
//
//  //TODO: convert to SAM?
//
//  implicit def seqModifier[A](implicit vm: AsVDomModifier[F, A]) = new AsVDomModifier[F, Seq[A]] {
//    override def asVDomModifier(value: Seq[A]): VDomModifierF =
//      value.toList.traverse(vm.asVDomModifier).map(CompositeModifier)
//  }
//
//  implicit def optionModifier[A](implicit vm: AsVDomModifier[F, A]) = new AsVDomModifier[F, Option[A]] {
//    def asVDomModifier(value: Option[A]): VDomModifierF =
//      value.map(vm.asVDomModifier).getOrElse(VDomModifierF.empty)
//  }
//
//  implicit def vDomModifierAsVDomModifier: AsVDomModifier[F, VDomModifierF] =
//    new AsVDomModifier[F, VDomModifierF] {
//      def asVDomModifier(value: VDomModifierF): VDomModifierF = value
//    }
//
//  implicit def stringAsVDomModifier = new AsVDomModifier[F, String] {
//    def asVDomModifier(value: String): VDomModifierF = effectF.pure(StringModifier(value))
//  }
//
//  implicit def IntAsVDomModifier = new AsVDomModifier[F, Int] {
//    def asVDomModifier(value: Int): VDomModifierF = effectF.pure(StringModifier(value.toString))
//  }
//
//  implicit def DoubleAsVDomModifier = new AsVDomModifier[F, Double] {
//    def asVDomModifier(value: Double): VDomModifierF = effectF.pure(StringModifier(value.toString))
//  }
//
//  implicit def observableRender = new AsVDomModifier[F, Observable[VNodeF]] {
//
//    def asVDomModifier(valueStream: Observable[VNodeF]): VDomModifierF =
//      effectF.pure(ChildStreamReceiver(valueStream))
//  }
//
//  implicit def observableRender[T](implicit staticVNodeRender: StaticVNodeRender[F,T]): AsVDomModifier[F, Observable[T]] =
//    new AsVDomModifier[F, Observable[T]] {
//      def asVDomModifier(valueStream: Observable[T]): VDomModifierF = effectF.pure(
//        ChildStreamReceiver(valueStream.map(staticVNodeRender.render))
//      )
//    }
//
//  implicit def observableSeqRender = new AsVDomModifier[F, Observable[Seq[VNodeF]]] {
//    def asVDomModifier(seqStream: Observable[Seq[VNodeF]]): VDomModifierF =
//      effectF.pure(ChildrenStreamReceiver(seqStream))
//  }
//
//  implicit def observableSeqRender[T](implicit staticVNodeRender: StaticVNodeRender[F,T]) = new AsVDomModifier[F, Observable[Seq[T]]] {
//    def asVDomModifier(seqStream: Observable[Seq[T]]): VDomModifierF =
//      effectF.pure(ChildrenStreamReceiver(seqStream.map(_.map(staticVNodeRender.render))))
//  }
//
//}
