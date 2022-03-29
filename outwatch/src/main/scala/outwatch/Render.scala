package outwatch

import outwatch.helpers.AttributeBuilder

import colibri._
import colibri.effect._

import cats.data.{NonEmptyList, NonEmptySeq, NonEmptyVector, NonEmptyChain, Kleisli}
import cats.effect.{SyncIO, IO}

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.Success

trait Render[-Env, -T] {
  def render(value: T): VModifierM[Env]
}

trait RenderLowPrio0 {
  import RenderOps._

  @inline implicit def EffectRender[F[_]: RunEffect]: Render[Any, F[VModifier]] = new EffectRenderClass[F]
  @inline private class EffectRenderClass[F[_]: RunEffect] extends Render[Any, F[VModifier]] {
    @inline def render(effect: F[VModifier]) = effectToModifier(effect)
  }

  @inline implicit def EffectRenderAs[Env, F[_]: RunEffect, T: Render[Env, *]]: Render[Env, F[T]] = new EffectRenderAsClass[Env, F, T]
  @inline private class EffectRenderAsClass[Env, F[_]: RunEffect, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = effectToModifierRender(effect)
  }
}

trait RenderLowPrio extends RenderLowPrio0 {
  import RenderOps._

  @inline implicit def JsArrayModifierAs[Env, T: Render[Env, *]]: Render[Env, js.Array[T]] = new JsArrayRenderAsClass[Env, T]
  @inline private class JsArrayRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, js.Array[T]] {
    @inline def render(value: js.Array[T]) = iterableToModifierRender(value)
  }

  @inline implicit def ArrayModifierAs[Env, T: Render[Env, *]]: Render[Env, Array[T]] = new ArrayRenderAsClass[Env, T]
  @inline private class ArrayRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, Array[T]] {
    @inline def render(value: Array[T]) = iterableToModifierRender(value)
  }

  @inline implicit def SeqModifierAs[Env, T: Render[Env, *]]: Render[Env, Seq[T]] = new SeqRenderAsClass[Env, T]
  @inline private class SeqRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, Seq[T]] {
    @inline def render(value: Seq[T]) = iterableToModifierRender(value)
  }

  @inline implicit def NonEmptyListModifierAs[Env, T: Render[Env, *]]: Render[Env, NonEmptyList[T]] = new NonEmptyListRenderAsClass[Env, T]
  @inline private class NonEmptyListRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, NonEmptyList[T]] {
    @inline def render(value: NonEmptyList[T]) = iterableToModifierRender(value.toList)
  }

  @inline implicit def NonEmptyVectorModifierAs[Env, T: Render[Env, *]]: Render[Env, NonEmptyVector[T]] = new NonEmptyVectorRenderAsClass[Env, T]
  @inline private class NonEmptyVectorRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, NonEmptyVector[T]] {
    @inline def render(value: NonEmptyVector[T]) = iterableToModifierRender(value.toVector)
  }

  @inline implicit def NonEmptySeqModifierAs[Env, T: Render[Env, *]]: Render[Env, NonEmptySeq[T]] = new NonEmptySeqRenderAsClass[Env, T]
  @inline private class NonEmptySeqRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, NonEmptySeq[T]] {
    @inline def render(value: NonEmptySeq[T]) = iterableToModifierRender(value.toSeq)
  }

  @inline implicit def NonEmptyChainModifierAs[Env, T: Render[Env, *]]: Render[Env, NonEmptyChain[T]] = new NonEmptyChainRenderAsClass[Env, T]
  @inline private class NonEmptyChainRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, NonEmptyChain[T]] {
    @inline def render(value: NonEmptyChain[T]) = iterableToModifierRender(value.toChain.toList)
  }

  @inline implicit def OptionModifierAs[Env, T: Render[Env, *]]: Render[Env, Option[T]] = new OptionRenderAsClass[Env, T]
  @inline private class OptionRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, Option[T]] {
    @inline def render(value: Option[T]) = optionToModifierRender(value)
  }

  @inline implicit def UndefinedModifierAs[Env, T: Render[Env, *]]: Render[Env, js.UndefOr[T]] = new UndefinedRenderAsClass[Env, T]
  @inline private class UndefinedRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  @inline implicit def SyncEffectRenderAs[Env, F[_]: RunSyncEffect, T: Render[Env, *]]: Render[Env, F[T]] = new SyncEffectRenderAsClass[Env, F, T]
  @inline private class SyncEffectRenderAsClass[Env, F[_]: RunSyncEffect, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  @inline implicit def FutureRenderAs[Env, T: Render[Env, *]]: Render[Env, Future[T]] = new FutureRenderAsClass[Env, T]
  @inline private class FutureRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRenderAs[Env, F[_]: Source, T: Render[Env, *]]: Render[Env, F[T]] = new SourceRenderAsClass[Env, F, T]
  @inline private class SourceRenderAsClass[Env, F[_]: Source, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(source: F[T]) = sourceToModifierRender(source)
  }
}

object Render extends RenderLowPrio {
  @inline def apply[Env, T](implicit render: Render[Env, T]): Render[Env, T] = render

  import RenderOps._

  implicit object JsArrayModifier extends Render[Any, js.Array[VModifier]] {
    @inline def render(value: js.Array[VModifier]): VModifier = CompositeModifier(value)
  }

  implicit object ArrayModifier extends Render[Any, Array[VModifier]] {
    @inline def render(value: Array[VModifier]): VModifier = CompositeModifier(value)
  }

  implicit object SeqModifier extends Render[Any, Seq[VModifier]] {
    @inline def render(value: Seq[VModifier]): VModifier = CompositeModifier(value)
  }

  implicit object NonEmptyListModifier extends Render[Any, NonEmptyList[VModifier]] {
    @inline def render(value: NonEmptyList[VModifier]): VModifier = CompositeModifier(value.toList)
  }

  implicit object NonEmptyVectorModifier extends Render[Any, NonEmptyVector[VModifier]] {
    @inline def render(value: NonEmptyVector[VModifier]): VModifier = CompositeModifier(value.toVector)
  }

  implicit object NonEmptySeqModifier extends Render[Any, NonEmptySeq[VModifier]] {
    @inline def render(value: NonEmptySeq[VModifier]): VModifier = CompositeModifier(value.toSeq)
  }

  implicit object NonEmptyChainModifier extends Render[Any, NonEmptyChain[VModifier]] {
    @inline def render(value: NonEmptyChain[VModifier]): VModifier = CompositeModifier(value.toChain.toList)
  }

  implicit object OptionModifier extends Render[Any, Option[VModifier]] {
    @inline def render(value: Option[VModifier]): VModifier = value.getOrElse(VModifier.empty)
  }

  implicit object UndefinedModifier extends Render[Any, js.UndefOr[VModifier]] {
    @inline def render(value: js.UndefOr[VModifier]): VModifier = value.getOrElse(VModifier.empty)
  }

  @inline implicit def vModifierMRender[Env]: Render[Env, VModifierM[Env]] = new VModifierMRenderClass[Env]
  @inline private class VModifierMRenderClass[Env] extends Render[Env, VModifierM[Env]] {
    @inline def render(value: VModifierM[Env]) = value
  }

  implicit object VModifierRender extends Render[Any, VModifier] {
    @inline def render(value: VModifier): VModifier = value
  }

  implicit object StringRender extends Render[Any, String] {
    @inline def render(value: String): VModifier = StringVNode(value)
  }

  implicit object IntRender extends Render[Any, Int] {
    @inline def render(value: Int): VModifier = StringVNode(value.toString)
  }

  implicit object DoubleRender extends Render[Any, Double] {
    @inline def render(value: Double): VModifier = StringVNode(
      value.toString
    )
  }

  implicit object LongRender extends Render[Any, Long] {
    @inline def render(value: Long): VModifier = StringVNode(value.toString)
  }

  implicit object BooleanRender extends Render[Any, Boolean] {
    @inline def render(value: Boolean): VModifier = StringVNode(
      value.toString
    )
  }

  implicit object SyncIORender extends Render[Any, SyncIO[VModifier]] {
    @inline def render(future: SyncIO[VModifier]) = syncToModifier(future)
  }

  @inline implicit def SyncEffectRender[F[_]: RunSyncEffect]: Render[Any, F[VModifier]] = new SyncEffectRenderClass[F]
  @inline private class SyncEffectRenderClass[F[_]: RunSyncEffect] extends Render[Any, F[VModifier]] {
    @inline def render(effect: F[VModifier]) = syncToModifier(effect)
  }

  implicit object IORender extends Render[Any, IO[VModifier]] {
    @inline def render(future: IO[VModifier]) = effectToModifier(future)
  }

  implicit object FutureRender extends Render[Any, Future[VModifier]] {
    @inline def render(future: Future[VModifier]) = futureToModifier(future)
  }

  implicit object ObservableRender extends Render[Any, Observable[VModifier]] {
    @inline def render(source: Observable[VModifier]) = sourceToModifier(source)
  }

  @inline implicit def SourceRender[F[_]: Source] : Render[Any, F[VModifier]] = new SourceRenderClass[F]
  @inline private class SourceRenderClass[F[_]: Source] extends Render[Any, F[VModifier]] {
    @inline def render(source: F[VModifier]) = sourceToModifier(source)
  }

  @inline implicit def KleisliRenderAs[F[_], Env, T](implicit r: Render[Any, F[T]]): Render[Env, Kleisli[F, Env, T]] = new KleisliRenderAsClass[F, Env, T]
  @inline private final class KleisliRenderAsClass[F[_], Env, T](implicit r: Render[Any, F[T]]) extends Render[Env, Kleisli[F, Env, T]] {
    @inline def render(kleisli: Kleisli[F, Env, T]) = VModifierM.access[Env](env => r.render(kleisli.run(env)))
  }

  @inline implicit def EmitterBuilderRender: Render[Any, EmitterBuilder[VModifier, VModifier]] = new EmitterBuilderRender
  @inline private final class EmitterBuilderRender extends Render[Any, EmitterBuilder[VModifier, VModifier]] {
    @inline def render(builder: EmitterBuilder[VModifier, VModifier]) = builder.render
  }

  @inline implicit def AttributeBuilderRender: Render[Any, AttributeBuilder[Boolean, VModifier]] = new AttributeBuilderRender
  @inline private final class AttributeBuilderRender extends Render[Any, AttributeBuilder[Boolean, VModifier]] {
    @inline def render(builder: AttributeBuilder[Boolean, VModifier]) = builder := true
  }

  @inline implicit def ChildCommandSourceRender[F[_]: Source]: Render[Any, F[ChildCommand]] = new ChildCommandRenderClass[F]
  @inline private class ChildCommandRenderClass[F[_]: Source] extends Render[Any, F[ChildCommand]] {
    @inline def render(source: F[ChildCommand]) = childCommandToModifier(source)
  }

  @inline implicit def ChildCommandSeqSourceRender[F[_]: Source]: Render[Any, F[Seq[ChildCommand]]] = new ChildCommandSeqRenderClass[F]
  @inline private class ChildCommandSeqRenderClass[F[_]: Source] extends Render[Any, F[Seq[ChildCommand]]] {
    @inline def render(source: F[Seq[ChildCommand]]) = childCommandSeqToModifier(source)
  }
}

private object RenderOps {
  def iterableToModifierRender[Env, T: Render[Env, *]](value: Iterable[T]): VModifierM[Env] = CompositeModifier(value.map(VModifierM(_)))
  def optionToModifierRender[Env, T: Render[Env, *]](value: Option[T]): VModifierM[Env] = value.fold[VModifierM[Env]](VModifierM.empty)(VModifierM(_))
  def undefinedToModifierRender[Env, T: Render[Env, *]](value: js.UndefOr[T]): VModifierM[Env] = value.fold[VModifierM[Env]](VModifierM.empty)(VModifierM(_))
  def syncToModifierRender[F[_]: RunSyncEffect, Env, T: Render[Env, *]](effect: F[T]): VModifierM[Env] = VModifierM.delayEither(RunSyncEffect[F].unsafeRun(effect))
  def syncToModifier[F[_]: RunSyncEffect, Env](effect: F[VModifierM[Env]]): VModifierM[Env] = VModifierM.delayEither(RunSyncEffect[F].unsafeRun(effect))
  def effectToModifier[F[_]: RunEffect, Env](effect: F[VModifierM[Env]]): VModifierM[Env] = StreamModifier(Observable.fromEffect(effect).unsafeSubscribe(_))
  def effectToModifierRender[F[_]: RunEffect, Env, T: Render[Env, *]](effect: F[T]): VModifierM[Env] = StreamModifier(obs => Observable.fromEffect(effect).unsafeSubscribe(obs.contramap(VModifier(_))))
  def sourceToModifier[F[_]: Source, Env](source: F[VModifierM[Env]]): VModifierM[Env] = StreamModifier(Source[F].unsafeSubscribe(source))
  def sourceToModifierRender[F[_]: Source, Env, T: Render[Env, *]](source: F[T]): VModifierM[Env] = StreamModifier(sink => Source[F].unsafeSubscribe(source)(sink.contramap(VModifierM(_))))
  def childCommandSeqToModifier[F[_]: Source, Env](source: F[Seq[ChildCommand]]): VModifierM[Env] = ChildCommandsModifier(Observable.lift(source))
  def childCommandToModifier[F[_]: Source, Env](source: F[ChildCommand]): VModifierM[Env] = ChildCommandsModifier(Observable.lift(source).map(Seq(_)))
  def futureToModifierRender[Env, T: Render[Env, *]](future: Future[T]): VModifierM[Env] = future.value match {
    case Some(Success(value)) => VModifierM(value)
    case _ => StreamModifier(Observable.fromFuture(future).map(VModifierM(_)).unsafeSubscribe(_))
  }
  def futureToModifier[Env](future: Future[VModifierM[Env]]
  ): VModifierM[Env] = future.value match {
    case Some(Success(value)) => value
    case _ => StreamModifier(Observable.fromFuture(future).unsafeSubscribe(_))
  }
}
