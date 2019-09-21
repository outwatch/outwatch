package outwatch.dom

import outwatch.reactive._
import outwatch.effect.RunSyncEffect

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.Success

import cats.effect.Effect

trait Render[-T] {
  def render(value: T): VDomModifier
}

object Render {
  @inline def apply[T](implicit render: Render[T]): Render[T] = render

  implicit object JsArrayModifier extends Render[js.Array[VDomModifier]] {
    @inline def render(value: js.Array[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  @inline implicit def JsArrayModifierAs[T : Render]: Render[js.Array[T]] = new JsArrayRenderAsClass[T]
  @inline private class JsArrayRenderAsClass[T : Render] extends Render[js.Array[T]] {
    @inline def render(value: js.Array[T]) = iterableToModifierRender(value)
  }

  implicit object ArrayModifier extends Render[Array[VDomModifier]] {
    @inline def render(value: Array[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  @inline implicit def ArrayModifierAs[T : Render]: Render[Array[T]] = new ArrayRenderAsClass[T]
  @inline private class ArrayRenderAsClass[T : Render] extends Render[Array[T]] {
    @inline def render(value: Array[T]) = iterableToModifierRender(value)
  }

  implicit object SeqModifier extends Render[Seq[VDomModifier]] {
    @inline def render(value: Seq[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  @inline implicit def SeqModifierAs[T : Render]: Render[Seq[T]] = new SeqRenderAsClass[T]
  @inline private class SeqRenderAsClass[T : Render] extends Render[Seq[T]] {
    @inline def render(value: Seq[T]) = iterableToModifierRender(value)
  }

  implicit object OptionModifier extends Render[Option[VDomModifier]] {
    @inline def render(value: Option[VDomModifier]): VDomModifier = value.getOrElse(VDomModifier.empty)
  }

  @inline implicit def OptionModifierAs[T : Render]: Render[Option[T]] = new OptionRenderAsClass[T]
  @inline private class OptionRenderAsClass[T : Render] extends Render[Option[T]] {
    @inline def render(value: Option[T]) = optionToModifierRender(value)
  }

  implicit object UndefinedModifier extends Render[js.UndefOr[VDomModifier]] {
    @inline def render(value: js.UndefOr[VDomModifier]): VDomModifier = value.getOrElse(VDomModifier.empty)
  }

  @inline implicit def UndefinedModifierAs[T : Render]: Render[js.UndefOr[T]] = new UndefinedRenderAsClass[T]
  @inline private class UndefinedRenderAsClass[T : Render] extends Render[js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

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

  @inline implicit def SyncEffectRender[F[_] : RunSyncEffect]: Render[F[VDomModifier]] = new SyncEffectRenderClass[F]
  @inline private class SyncEffectRenderClass[F[_] : RunSyncEffect] extends Render[F[VDomModifier]] {
    @inline def render(effect: F[VDomModifier]) = syncToModifier(effect)
  }

  @inline implicit def SyncEffectRenderAs[F[_] : RunSyncEffect, T : Render]: Render[F[T]] = new SyncEffectRenderAsClass[F, T]
  @inline private class SyncEffectRenderAsClass[F[_] : RunSyncEffect, T : Render] extends Render[F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  implicit def EffectRender[F[_] : Effect]: Render[F[VDomModifier]] = new EffectRenderClass[F]
  @inline private class EffectRenderClass[F[_] : Effect] extends Render[F[VDomModifier]] {
    def render(effect: F[VDomModifier]) = asyncToModifier(effect)
  }

  @inline implicit def EffectRenderAs[F[_] : Effect, T : Render]: Render[F[T]] = new EffectRenderAsClass[F, T]
  @inline private class EffectRenderAsClass[F[_] : Effect, T : Render] extends Render[F[T]] {
    @inline def render(effect: F[T]) = asyncToModifierRender(effect)
  }

  implicit object FutureRender extends Render[Future[VDomModifier]] {
    @inline def render(future: Future[VDomModifier]) = futureToModifier(future)
  }

  @inline implicit def FutureRenderAs[T : Render]: Render[Future[T]] = new FutureRenderAsClass[T]
  @inline private class FutureRenderAsClass[T: Render] extends Render[Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRender[F[_] : Source]: Render[F[VDomModifier]] = new SourceRenderClass[F]
  @inline private class SourceRenderClass[F[_] : Source] extends Render[F[VDomModifier]] {
    @inline def render(source: F[VDomModifier]) = sourceToModifier(source)
  }

  @inline implicit def SourceRenderAs[F[_] : Source, T : Render]: Render[F[T]] = new SourceRenderAsClass[F, T]
  @inline private class SourceRenderAsClass[F[_]: Source, T: Render] extends Render[F[T]] {
    @inline def render(source: F[T]) = sourceToModifierRender(source)
  }

  @inline implicit def ChildCommandSourceRender[F[_] : Source]: Render[F[ChildCommand]] = new ChildCommandRenderClass[F]
  @inline private class ChildCommandRenderClass[F[_] : Source] extends Render[F[ChildCommand]] {
    @inline def render(source: F[ChildCommand]) = childCommandToModifier(source)
  }

  @inline implicit def ChildCommandSeqSourceRender[F[_] : Source]: Render[F[Seq[ChildCommand]]] = new ChildCommandSeqRenderClass[F]
  @inline private class ChildCommandSeqRenderClass[F[_] : Source] extends Render[F[Seq[ChildCommand]]] {
    @inline def render(source: F[Seq[ChildCommand]]) = childCommandSeqToModifier(source)
  }

  @noinline private def iterableToModifierRender[T: Render](value: Iterable[T]): VDomModifier = CompositeModifier(value.map(VDomModifier(_)))
  @noinline private def optionToModifierRender[T: Render](value: Option[T]): VDomModifier = value.fold(VDomModifier.empty)(VDomModifier(_))
  @noinline private def undefinedToModifierRender[T: Render](value: js.UndefOr[T]): VDomModifier = value.fold(VDomModifier.empty)(VDomModifier(_))
  @noinline private def syncToModifierRender[F[_] : RunSyncEffect, T: Render](effect: F[T]): VDomModifier = SyncEffectModifier(() => VDomModifier(RunSyncEffect[F].unsafeRun(effect)))
  @noinline private def syncToModifier[F[_] : RunSyncEffect](effect: F[VDomModifier]): VDomModifier = SyncEffectModifier(() => RunSyncEffect[F].unsafeRun(effect))
  @noinline private def asyncToModifier[F[_] : Effect](effect: F[VDomModifier]): VDomModifier = StreamModifier(SourceStream.fromAsync(effect).subscribe(_))
  @noinline private def asyncToModifierRender[F[_] : Effect, T: Render](effect: F[T]): VDomModifier = StreamModifier(SourceStream.fromAsync(effect).map(VDomModifier(_)).subscribe(_))
  @noinline private def sourceToModifier[F[_] : Source](source: F[VDomModifier]): VDomModifier = StreamModifier(Source[F].subscribe(source))
  @noinline private def sourceToModifierRender[F[_] : Source, T: Render](source: F[T]): VDomModifier = StreamModifier(sink => Source[F].subscribe(source)(SinkObserver.contramap[SinkObserver, VDomModifier, T](sink)(VDomModifier(_))))
  @noinline private def childCommandSeqToModifier[F[_] : Source](source: F[Seq[ChildCommand]]): VDomModifier = ChildCommand.stream(SourceStream.lift(source))
  @noinline private def childCommandToModifier[F[_] : Source](source: F[ChildCommand]): VDomModifier = ChildCommand.stream(SourceStream.map(source)(Seq(_)))
  @noinline private def futureToModifierRender[T: Render](future: Future[T]): VDomModifier = future.value match {
    case Some(Success(value)) => VDomModifier(value)
    case _ => StreamModifier(SourceStream.fromFuture(future).map(VDomModifier(_)).subscribe(_))
  }
  @noinline private def futureToModifier(future: Future[VDomModifier]): VDomModifier = future.value match {
    case Some(Success(value)) => value
    case _ => StreamModifier(SourceStream.fromFuture(future).subscribe(_))
  }
}
