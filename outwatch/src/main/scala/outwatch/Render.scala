package outwatch

import colibri._
import colibri.effect._

import cats.data.{NonEmptyList, NonEmptySeq, NonEmptyVector, NonEmptyChain}
import cats.effect.{SyncIO, IO}

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.Success

trait Render[-T] {
  def render(value: T): VDomModifier
}

trait RenderLowPrio0 {
  import RenderOps._

  @inline implicit def EffectRender[F[_] : RunEffect]: Render[F[VDomModifier]] = new EffectRenderClass[F]
  @inline private class EffectRenderClass[F[_] : RunEffect] extends Render[F[VDomModifier]] {
    @inline def render(effect: F[VDomModifier]) = effectToModifier(effect)
  }

  @inline implicit def EffectRenderAs[F[_] : RunEffect, T : Render]: Render[F[T]] = new EffectRenderAsClass[F, T]
  @inline private class EffectRenderAsClass[F[_] : RunEffect, T : Render] extends Render[F[T]] {
    @inline def render(effect: F[T]) = effectToModifierRender(effect)
  }
}

trait RenderLowPrio extends RenderLowPrio0 {
  import RenderOps._

  @inline implicit def JsArrayModifierAs[T : Render]: Render[js.Array[T]] = new JsArrayRenderAsClass[T]
  @inline private class JsArrayRenderAsClass[T : Render] extends Render[js.Array[T]] {
    @inline def render(value: js.Array[T]) = iterableToModifierRender(value)
  }

  @inline implicit def ArrayModifierAs[T : Render]: Render[Array[T]] = new ArrayRenderAsClass[T]
  @inline private class ArrayRenderAsClass[T : Render] extends Render[Array[T]] {
    @inline def render(value: Array[T]) = iterableToModifierRender(value)
  }

  @inline implicit def SeqModifierAs[T : Render]: Render[Seq[T]] = new SeqRenderAsClass[T]
  @inline private class SeqRenderAsClass[T : Render] extends Render[Seq[T]] {
    @inline def render(value: Seq[T]) = iterableToModifierRender(value)
  }

  @inline implicit def NonEmptyListModifierAs[T : Render]: Render[NonEmptyList[T]] = new NonEmptyListRenderAsClass[T]
  @inline private class NonEmptyListRenderAsClass[T : Render] extends Render[NonEmptyList[T]] {
    @inline def render(value: NonEmptyList[T]) = iterableToModifierRender(value.toList)
  }

  @inline implicit def NonEmptyVectorModifierAs[T : Render]: Render[NonEmptyVector[T]] = new NonEmptyVectorRenderAsClass[T]
  @inline private class NonEmptyVectorRenderAsClass[T : Render] extends Render[NonEmptyVector[T]] {
    @inline def render(value: NonEmptyVector[T]) = iterableToModifierRender(value.toVector)
  }

  @inline implicit def NonEmptySeqModifierAs[T : Render]: Render[NonEmptySeq[T]] = new NonEmptySeqRenderAsClass[T]
  @inline private class NonEmptySeqRenderAsClass[T : Render] extends Render[NonEmptySeq[T]] {
    @inline def render(value: NonEmptySeq[T]) = iterableToModifierRender(value.toSeq)
  }

  @inline implicit def NonEmptyChainModifierAs[T : Render]: Render[NonEmptyChain[T]] = new NonEmptyChainRenderAsClass[T]
  @inline private class NonEmptyChainRenderAsClass[T : Render] extends Render[NonEmptyChain[T]] {
    @inline def render(value: NonEmptyChain[T]) = iterableToModifierRender(value.toChain.toList)
  }

  @inline implicit def OptionModifierAs[T : Render]: Render[Option[T]] = new OptionRenderAsClass[T]
  @inline private class OptionRenderAsClass[T : Render] extends Render[Option[T]] {
    @inline def render(value: Option[T]) = optionToModifierRender(value)
  }

  @inline implicit def UndefinedModifierAs[T : Render]: Render[js.UndefOr[T]] = new UndefinedRenderAsClass[T]
  @inline private class UndefinedRenderAsClass[T : Render] extends Render[js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  @inline implicit def SyncEffectRenderAs[F[_] : RunSyncEffect, T : Render]: Render[F[T]] = new SyncEffectRenderAsClass[F, T]
  @inline private class SyncEffectRenderAsClass[F[_] : RunSyncEffect, T : Render] extends Render[F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  @inline implicit def FutureRenderAs[T : Render]: Render[Future[T]] = new FutureRenderAsClass[T]
  @inline private class FutureRenderAsClass[T: Render] extends Render[Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRenderAs[F[_] : Source, T : Render]: Render[F[T]] = new SourceRenderAsClass[F, T]
  @inline private class SourceRenderAsClass[F[_]: Source, T: Render] extends Render[F[T]] {
    @inline def render(source: F[T]) = sourceToModifierRender(source)
  }
}

object Render extends RenderLowPrio {
  @inline def apply[T](implicit render: Render[T]): Render[T] = render

  import RenderOps._

  implicit object JsArrayModifier extends Render[js.Array[VDomModifier]] {
    @inline def render(value: js.Array[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  implicit object ArrayModifier extends Render[Array[VDomModifier]] {
    @inline def render(value: Array[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  implicit object SeqModifier extends Render[Seq[VDomModifier]] {
    @inline def render(value: Seq[VDomModifier]): VDomModifier = CompositeModifier(value)
  }

  implicit object NonEmptyListModifier extends Render[NonEmptyList[VDomModifier]] {
    @inline def render(value: NonEmptyList[VDomModifier]): VDomModifier = CompositeModifier(value.toList)
  }

  implicit object NonEmptyVectorModifier extends Render[NonEmptyVector[VDomModifier]] {
    @inline def render(value: NonEmptyVector[VDomModifier]): VDomModifier = CompositeModifier(value.toVector)
  }

  implicit object NonEmptySeqModifier extends Render[NonEmptySeq[VDomModifier]] {
    @inline def render(value: NonEmptySeq[VDomModifier]): VDomModifier = CompositeModifier(value.toSeq)
  }

  implicit object NonEmptyChainModifier extends Render[NonEmptyChain[VDomModifier]] {
    @inline def render(value: NonEmptyChain[VDomModifier]): VDomModifier = CompositeModifier(value.toChain.toList)
  }

  implicit object OptionModifier extends Render[Option[VDomModifier]] {
    @inline def render(value: Option[VDomModifier]): VDomModifier = value.getOrElse(VDomModifier.empty)
  }

  implicit object UndefinedModifier extends Render[js.UndefOr[VDomModifier]] {
    @inline def render(value: js.UndefOr[VDomModifier]): VDomModifier = value.getOrElse(VDomModifier.empty)
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

  implicit object SyncIORender extends Render[SyncIO[VDomModifier]] {
    @inline def render(future: SyncIO[VDomModifier]) = syncToModifier(future)
  }

  @inline implicit def SyncEffectRender[F[_] : RunSyncEffect]: Render[F[VDomModifier]] = new SyncEffectRenderClass[F]
  @inline private class SyncEffectRenderClass[F[_] : RunSyncEffect] extends Render[F[VDomModifier]] {
    @inline def render(effect: F[VDomModifier]) = syncToModifier(effect)
  }

  implicit object IORender extends Render[IO[VDomModifier]] {
    @inline def render(future: IO[VDomModifier]) = effectToModifier(future)
  }

  implicit object FutureRender extends Render[Future[VDomModifier]] {
    @inline def render(future: Future[VDomModifier]) = futureToModifier(future)
  }

  implicit object ObservableRender extends Render[Observable[VDomModifier]] {
    @inline def render(source: Observable[VDomModifier]) = sourceToModifier(source)
  }

  @inline implicit def SourceRender[F[_] : Source]: Render[F[VDomModifier]] = new SourceRenderClass[F]
  @inline private class SourceRenderClass[F[_] : Source] extends Render[F[VDomModifier]] {
    @inline def render(source: F[VDomModifier]) = sourceToModifier(source)
  }

  @inline implicit def ChildCommandSourceRender[F[_] : Source]: Render[F[ChildCommand]] = new ChildCommandRenderClass[F]
  @inline private class ChildCommandRenderClass[F[_] : Source] extends Render[F[ChildCommand]] {
    @inline def render(source: F[ChildCommand]) = childCommandToModifier(source)
  }

  @inline implicit def ChildCommandSeqSourceRender[F[_] : Source]: Render[F[Seq[ChildCommand]]] = new ChildCommandSeqRenderClass[F]
  @inline private class ChildCommandSeqRenderClass[F[_] : Source] extends Render[F[Seq[ChildCommand]]] {
    @inline def render(source: F[Seq[ChildCommand]]) = childCommandSeqToModifier(source)
  }
}

private object RenderOps {
  @noinline def iterableToModifierRender[T: Render](value: Iterable[T]): VDomModifier = CompositeModifier(value.map(VDomModifier(_)))
  @noinline def optionToModifierRender[T: Render](value: Option[T]): VDomModifier = value.fold(VDomModifier.empty)(VDomModifier(_))
  @noinline def undefinedToModifierRender[T: Render](value: js.UndefOr[T]): VDomModifier = value.fold(VDomModifier.empty)(VDomModifier(_))
  @noinline def syncToModifierRender[F[_] : RunSyncEffect, T: Render](effect: F[T]): VDomModifier = VDomModifier.delayEither(RunSyncEffect[F].unsafeRun(effect))
  @noinline def syncToModifier[F[_] : RunSyncEffect](effect: F[VDomModifier]): VDomModifier = VDomModifier.delayEither(RunSyncEffect[F].unsafeRun(effect))
  @noinline def effectToModifier[F[_]: RunEffect](effect: F[VDomModifier]): VDomModifier = StreamModifier(Observable.fromEffect(effect).unsafeSubscribe(_))
  @noinline def effectToModifierRender[F[_]: RunEffect, T: Render](effect: F[T]): VDomModifier = StreamModifier(obs => Observable.fromEffect(effect).unsafeSubscribe(obs.contramap(VDomModifier(_))))
  @noinline def sourceToModifier[F[_] : Source](source: F[VDomModifier]): VDomModifier = StreamModifier(Source[F].unsafeSubscribe(source))
  @noinline def sourceToModifierRender[F[_] : Source, T: Render](source: F[T]): VDomModifier = StreamModifier(sink => Source[F].unsafeSubscribe(source)(sink.contramap(VDomModifier(_))))
  @noinline def childCommandSeqToModifier[F[_] : Source](source: F[Seq[ChildCommand]]): VDomModifier = ChildCommandsModifier(Observable.lift(source))
  @noinline def childCommandToModifier[F[_] : Source](source: F[ChildCommand]): VDomModifier = ChildCommandsModifier(Observable.lift(source).map(Seq(_)))
  @noinline def futureToModifierRender[T: Render](future: Future[T]): VDomModifier = future.value match {
    case Some(Success(value)) => VDomModifier(value)
    case _ => StreamModifier(Observable.fromFuture(future).map(VDomModifier(_)).unsafeSubscribe(_))
  }
  @noinline def futureToModifier(future: Future[VDomModifier]): VDomModifier = future.value match {
    case Some(Success(value)) => value
    case _ => StreamModifier(Observable.fromFuture(future).unsafeSubscribe(_))
  }
}
