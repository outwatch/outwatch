package outwatch.dom

import outwatch.reactive._
import outwatch.effect.RunSyncEffect

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.Success

import cats.effect.Effect
import cats.implicits._

trait Render[-T] {
  def render(value: T): VDomModifier
}

object Render {
  @inline def apply[T](implicit render: Render[T]): Render[T] = render

  implicit object JsArrayModifier extends Render[js.Array[VDomModifier]] {
    @inline def render(value: js.Array[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  implicit def jsArrayModifier[T : Render]: Render[js.Array[T]] = (value: js.Array[T]) =>
    CompositeModifier(value.map(VDomModifier(_)))

  implicit object ArrayModifier extends Render[Array[VDomModifier]] {
    @inline def render(value: Array[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  implicit def arrayModifier[T : Render]: Render[Array[T]] = { (value: Array[T]) =>
    CompositeModifier(value.map(VDomModifier(_)))
  }

  implicit object SeqModifier extends Render[Seq[VDomModifier]] {
    @inline def render(value: Seq[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  implicit def seqModifier[T : Render]: Render[Seq[T]] = { (value: Seq[T]) =>
    CompositeModifier(value.map(VDomModifier(_)))
  }

  implicit object OptionModifier extends Render[Option[VDomModifier]] {
    @inline def render(value: Option[VDomModifier]): VDomModifier = value.getOrElse(VDomModifier.empty)
  }

  implicit def optionModifier[T : Render]: Render[Option[T]] = (value: Option[T]) =>
    value.fold(VDomModifier.empty)(VDomModifier(_))

  implicit object UndefinedModifier extends Render[js.UndefOr[VDomModifier]] {
    @inline def render(value: js.UndefOr[VDomModifier]): VDomModifier = value.getOrElse(VDomModifier.empty)
  }

  implicit def undefinedModifier[T : Render]: Render[js.UndefOr[T]] = (value: js.UndefOr[T]) =>
    value.fold(VDomModifier.empty)(VDomModifier(_))

  implicit object VDomModifierRender extends Render[VDomModifier] {
    @inline def render(value: VDomModifier): VDomModifier = value
  }

  implicit object StringRender extends Render[String] {
    @inline def render(value: String): VDomModifier = StringVNode(value)
  }

  implicit object IntRender extends Render[Int] {
    @inline def render(value: Int): VDomModifier = StringVNode(value.toString)
  }

  implicit object DoubleRender extends Render[Double] {
    @inline def render(value: Double): VDomModifier = StringVNode(value.toString)
  }

  implicit object LongRender extends Render[Long] {
    @inline def render(value: Long): VDomModifier = StringVNode(value.toString)
  }

  implicit object BooleanRender extends Render[Boolean] {
    @inline def render(value: Boolean): VDomModifier = StringVNode(value.toString)
  }

  implicit def syncEffectRender[F[_] : RunSyncEffect]: Render[F[VDomModifier]] = (effect: F[VDomModifier]) =>
    SyncEffectModifier(() => RunSyncEffect[F].unsafeRun(effect))

  implicit def syncEffectRenderAs[F[_] : RunSyncEffect, T : Render]: Render[F[T]] = (effect: F[T]) =>
    SyncEffectModifier(() => VDomModifier(RunSyncEffect[F].unsafeRun(effect)))

  implicit def effectRender[F[_] : Effect]: Render[F[VDomModifier]] = (effect: F[VDomModifier]) =>
    StreamModifier(SourceStream.fromAsync(effect).subscribe(_))

  implicit def effectRenderAs[F[_] : Effect, T : Render]: Render[F[T]] = (effect: F[T]) =>
    StreamModifier(SourceStream.fromAsync(effect).map(VDomModifier(_)).subscribe(_))

  implicit object FutureRender extends Render[Future[VDomModifier]] {
    def render(future: Future[VDomModifier]) = future.value match {
      case Some(Success(value)) => value
      case _ => StreamModifier(SourceStream.fromFuture(future).subscribe(_))
    }
  }

  implicit def futureRenderAs[T : Render]: Render[Future[T]] = (future: Future[T]) =>
    future.value match {
      case Some(Success(value)) => VDomModifier(value)
      case _ => StreamModifier(SourceStream.fromFuture(future).map(VDomModifier(_)).subscribe(_))
    }

  implicit def sourceRender[F[_] : Source]: Render[F[VDomModifier]] = (source: F[VDomModifier]) =>
    StreamModifier(Source[F].subscribe(source))

  implicit def sourceRenderAs[F[_] : Source, T : Render]: Render[F[T]] = (source: F[T]) =>
    StreamModifier(sink => Source[F].subscribe(source)(SinkObserver.contramap[SinkObserver, VDomModifier, T](sink)(VDomModifier(_))))

  implicit def childCommandObservableRender[F[_] : Source]: Render[F[ChildCommand]] = (source: F[ChildCommand]) =>
    ChildCommand.stream(SourceStream.map(source)(Seq(_)))

  implicit def childCommandSeqObservableRender[F[_] : Source]: Render[F[Seq[ChildCommand]]] = (source: F[Seq[ChildCommand]]) =>
    ChildCommand.stream(SourceStream.lift(source))
}
