package outwatch

import cats.effect.{IO, SyncIO}
import cats.syntax.functor._
import outwatch.dom._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.collection.breakOut

trait AsVDomModifier[-T] {
  def asVDomModifier(value: T): VDomModifier
}

object AsVDomModifier {

  implicit object JsArrayModifier extends AsVDomModifier[js.Array[VDomModifier]] {
    @inline def asVDomModifier(value: js.Array[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  implicit def jsArrayModifier[T : AsVDomModifier]: AsVDomModifier[js.Array[T]] = (value: js.Array[T]) =>
    CompositeModifier(value.map(VDomModifier(_)))

  implicit object ArrayModifier extends AsVDomModifier[Array[VDomModifier]] {
    @inline def asVDomModifier(value: Array[VDomModifier]): VDomModifier = CompositeModifier(value.toJSArray)
  }

  implicit def arrayModifier[T : AsVDomModifier]: AsVDomModifier[Array[T]] = (value: Array[T]) =>
    CompositeModifier(value.map(VDomModifier(_))(breakOut) : js.Array[VDomModifier])

  implicit object SeqModifier extends AsVDomModifier[Seq[VDomModifier]] {
    @inline def asVDomModifier(value: Seq[VDomModifier]): VDomModifier = CompositeModifier(value.toJSArray)
  }

  implicit def seqModifier[T : AsVDomModifier]: AsVDomModifier[Seq[T]] = (value: Seq[T]) =>
    CompositeModifier(value.map(VDomModifier(_))(breakOut) : js.Array[VDomModifier])

  implicit object OptionModifier extends AsVDomModifier[Option[VDomModifier]] {
    @inline def asVDomModifier(value: Option[VDomModifier]): VDomModifier = value.getOrElse(VDomModifier.empty)
  }

  implicit def optionModifier[T : AsVDomModifier]: AsVDomModifier[Option[T]] = (value: Option[T]) =>
    value.fold(VDomModifier.empty)(VDomModifier(_))

  implicit object UndefinedModifier extends AsVDomModifier[js.UndefOr[VDomModifier]] {
    @inline def asVDomModifier(value: js.UndefOr[VDomModifier]): VDomModifier = value.getOrElse(VDomModifier.empty)
  }

  implicit def undefinedModifier[T : AsVDomModifier]: AsVDomModifier[js.UndefOr[T]] = (value: js.UndefOr[T]) =>
    value.fold(VDomModifier.empty)(VDomModifier(_))

  implicit object VDomModifierAsVDomModifier extends AsVDomModifier[VDomModifier] {
    @inline def asVDomModifier(value: VDomModifier): VDomModifier = value
  }

  implicit object StringAsVDomModifier extends AsVDomModifier[String] {
    @inline def asVDomModifier(value: String): VDomModifier = StringVNode(value)
  }

  implicit object IntAsVDomModifier extends AsVDomModifier[Int] {
    @inline def asVDomModifier(value: Int): VDomModifier = StringVNode(value.toString)
  }

  implicit object DoubleAsVDomModifier extends AsVDomModifier[Double] {
    @inline def asVDomModifier(value: Double): VDomModifier = StringVNode(value.toString)
  }

  implicit object LongAsVDomModifier extends AsVDomModifier[Long] {
    @inline def asVDomModifier(value: Long): VDomModifier = StringVNode(value.toString)
  }

  implicit object BooleanAsVDomModifier extends AsVDomModifier[Boolean] {
    @inline def asVDomModifier(value: Boolean): VDomModifier = StringVNode(value.toString)
  }

  implicit object EffectRender extends AsVDomModifier[SyncIO[VDomModifier]] {
    @inline def asVDomModifier(value: SyncIO[VDomModifier]): VDomModifier = EffectModifierIO(value)
  }

  implicit def effectRenderIO[T : AsVDomModifier]: AsVDomModifier[IO[T]] = (effect: IO[T]) =>
    EffectModifier(effect.map(VDomModifier(_)))

  implicit def effectRender[T : AsVDomModifier]: AsVDomModifier[SyncIO[T]] = (effect: SyncIO[T]) =>
    EffectModifierIO(effect.map(VDomModifier(_)))

  implicit def valueObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[VDomModifier]] = (valueStream: F[VDomModifier]) =>
    ModifierStreamReceiver(ValueObservable(valueStream))

  implicit def valueObservableRenderAs[T : AsVDomModifier, F[_] : AsValueObservable]: AsVDomModifier[F[T]] = (valueStream: F[T]) =>
    ModifierStreamReceiver(ValueObservable(valueStream).map(VDomModifier(_)))

  implicit def childCommandObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[ChildCommand]] = (valueStream: F[ChildCommand]) =>
    SchedulerAction(implicit scheduler => ChildCommand.stream(ValueObservable(valueStream).map(Seq(_))).map(ModifierStreamReceiver(_)))

  implicit def childCommandSeqObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[Seq[ChildCommand]]] = (valueStream: F[Seq[ChildCommand]]) =>
    SchedulerAction(implicit scheduler => ChildCommand.stream(ValueObservable(valueStream)).map(ModifierStreamReceiver(_)))
}
