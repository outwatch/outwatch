package outwatch

import outwatch.dom._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.concurrent.Future
import scala.util.Success

import monix.reactive.Observable
import monix.eval.Task

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

  implicit def arrayModifier[T : AsVDomModifier]: AsVDomModifier[Array[T]] = { (value: Array[T]) =>
    var i = 0
    val n = value.length
    val arr = new js.Array[VDomModifier](n)
    while (i < n) {
      arr(i) = VDomModifier(value(i))
      i += 1
    }
    CompositeModifier(arr)
  }

  implicit object SeqModifier extends AsVDomModifier[Seq[VDomModifier]] {
    @inline def asVDomModifier(value: Seq[VDomModifier]): VDomModifier = CompositeModifier(value.toJSArray)
  }

  implicit def seqModifier[T : AsVDomModifier]: AsVDomModifier[Seq[T]] = { (value: Seq[T]) =>
    val arr = new js.Array[VDomModifier]()
    value.foreach { value => arr.push(VDomModifier(value)) }
    CompositeModifier(arr)
  }

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

  implicit def syncEffectRender[F[_] : RunSyncEffect]: AsVDomModifier[F[VDomModifier]] = (effect: F[VDomModifier]) =>
    SyncEffectModifier(() => RunSyncEffect[F].unsafeRun(effect))

  implicit def syncEffectRenderAs[F[_] : RunSyncEffect, T : AsVDomModifier]: AsVDomModifier[F[T]] = (effect: F[T]) =>
    SyncEffectModifier(() => VDomModifier(RunSyncEffect[F].unsafeRun(effect)))

  implicit def effectRender[F[_] : cats.effect.Effect]: AsVDomModifier[F[VDomModifier]] = (effect: F[VDomModifier]) =>
    ModifierStreamReceiver(ValueObservable(Observable.fromTask(Task.fromEffect(effect))))

  implicit def effectRenderAs[F[_] : cats.effect.Effect, T : AsVDomModifier]: AsVDomModifier[F[T]] = (effect: F[T]) =>
    ModifierStreamReceiver(ValueObservable(Observable.fromTask(Task.fromEffect(effect).map(VDomModifier(_)))))

  implicit object FutureRender extends AsVDomModifier[Future[VDomModifier]] {
    def asVDomModifier(future: Future[VDomModifier]): VDomModifier = future.value match {
      case Some(Success(value)) => value
      case _ => ModifierStreamReceiver(ValueObservable(Observable.fromFuture(future)))
    }
  }

  implicit def futureRenderAs[T : AsVDomModifier]: AsVDomModifier[Future[T]] = (future: Future[T]) =>
    future.value match {
      case Some(Success(value)) => VDomModifier(value)
      case _ => ModifierStreamReceiver(ValueObservable(Observable.fromFuture(future)).map(VDomModifier(_)))
    }

  implicit def valueObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[VDomModifier]] = (valueStream: F[VDomModifier]) =>
    ModifierStreamReceiver(ValueObservable(valueStream))

  implicit def valueObservableRenderAs[F[_] : AsValueObservable, T : AsVDomModifier]: AsVDomModifier[F[T]] = (valueStream: F[T]) =>
    ModifierStreamReceiver(ValueObservable(valueStream).map(VDomModifier(_)))

  implicit def childCommandObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[ChildCommand]] = (valueStream: F[ChildCommand]) =>
    SchedulerAction(implicit scheduler => ChildCommand.stream(ValueObservable(valueStream).map(Seq(_))))

  implicit def childCommandSeqObservableRender[F[_] : AsValueObservable]: AsVDomModifier[F[Seq[ChildCommand]]] = (valueStream: F[Seq[ChildCommand]]) =>
    SchedulerAction(implicit scheduler => ChildCommand.stream(ValueObservable(valueStream)))
}
