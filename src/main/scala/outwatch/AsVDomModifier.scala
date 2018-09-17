package outwatch

import cats.effect.IO
import cats.syntax.functor._
import outwatch.dom.{AsValueObservable, ChildCommand, CompositeModifier, ModifierStreamReceiver, StringVNode, VDomModifier, ValueObservable}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

trait AsVDomModifier[-T] {
  def asVDomModifier(value: T): VDomModifier
}

object AsVDomModifier {

  implicit def jsArrayModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[js.Array[T]] =
    (value: js.Array[T]) => IO {
      CompositeModifier(value.map(v => vm.asVDomModifier(v).unsafeRunSync))
    }

  implicit def seqModifier[T](implicit vm: AsVDomModifier[T]): AsVDomModifier[Seq[T]] =
    (value: Seq[T]) => IO {
      CompositeModifier(value.map(v => vm.asVDomModifier(v).unsafeRunSync).toJSArray)
    }

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

  implicit def valueObservableRender[T : AsVDomModifier, F[_] : AsValueObservable]: AsVDomModifier[F[T]] = (valueStream: F[T]) => IO.pure(
    ModifierStreamReceiver(ValueObservable(valueStream).map(VDomModifier(_)))
  )

  implicit def childCommandObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[ChildCommand]] = (valueStream: F[ChildCommand]) => for {
    stream <- ChildCommand.stream(ValueObservable(valueStream))
  } yield ModifierStreamReceiver(stream)
}
