package outwatch

import outwatch.helpers.AttrBuilder

import colibri._
import colibri.effect._

import cats.data.{Kleisli, NonEmptyChain, NonEmptyList, NonEmptySeq, NonEmptyVector}
import cats.effect.{IO, SyncIO}

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.Success

trait Render[-Env, -T] {
  def render(value: T): VModM[Env]
}

trait RenderLowPrio0 {
  import RenderOps._

  @inline implicit def EffectRender[F[_]: RunEffect]: Render[Any, F[VMod]] = new EffectRenderClass[F]
  @inline private class EffectRenderClass[F[_]: RunEffect] extends Render[Any, F[VMod]] {
    @inline def render(effect: F[VMod]) = effectToModifier(effect)
  }

  @inline implicit def EffectRenderAs[Env, F[_]: RunEffect, T: Render[Env, *]]: Render[Env, F[T]] =
    new EffectRenderAsClass[Env, F, T]
  @inline private class EffectRenderAsClass[Env, F[_]: RunEffect, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = effectToModifierRender(effect)
  }
}

trait RenderLowPrio extends RenderLowPrio0 {
  import RenderOps._

  @inline implicit def JsArrayModifierAs[Env, T: Render[Env, *]]: Render[Env, js.Array[T]] =
    new JsArrayRenderAsClass[Env, T]
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

  @inline implicit def NonEmptyListModifierAs[Env, T: Render[Env, *]]: Render[Env, NonEmptyList[T]] =
    new NonEmptyListRenderAsClass[Env, T]
  @inline private class NonEmptyListRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, NonEmptyList[T]] {
    @inline def render(value: NonEmptyList[T]) = iterableToModifierRender(value.toList)
  }

  @inline implicit def NonEmptyVectorModifierAs[Env, T: Render[Env, *]]: Render[Env, NonEmptyVector[T]] =
    new NonEmptyVectorRenderAsClass[Env, T]
  @inline private class NonEmptyVectorRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, NonEmptyVector[T]] {
    @inline def render(value: NonEmptyVector[T]) = iterableToModifierRender(value.toVector)
  }

  @inline implicit def NonEmptySeqModifierAs[Env, T: Render[Env, *]]: Render[Env, NonEmptySeq[T]] =
    new NonEmptySeqRenderAsClass[Env, T]
  @inline private class NonEmptySeqRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, NonEmptySeq[T]] {
    @inline def render(value: NonEmptySeq[T]) = iterableToModifierRender(value.toSeq)
  }

  @inline implicit def NonEmptyChainModifierAs[Env, T: Render[Env, *]]: Render[Env, NonEmptyChain[T]] =
    new NonEmptyChainRenderAsClass[Env, T]
  @inline private class NonEmptyChainRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, NonEmptyChain[T]] {
    @inline def render(value: NonEmptyChain[T]) = iterableToModifierRender(value.toChain.toList)
  }

  @inline implicit def OptionModifierAs[Env, T: Render[Env, *]]: Render[Env, Option[T]] =
    new OptionRenderAsClass[Env, T]
  @inline private class OptionRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, Option[T]] {
    @inline def render(value: Option[T]) = optionToModifierRender(value)
  }

  @inline implicit def UndefinedModifierAs[Env, T: Render[Env, *]]: Render[Env, js.UndefOr[T]] =
    new UndefinedRenderAsClass[Env, T]
  @inline private class UndefinedRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  @inline implicit def SyncEffectRenderAs[Env, F[_]: RunSyncEffect, T: Render[Env, *]]: Render[Env, F[T]] =
    new SyncEffectRenderAsClass[Env, F, T]
  @inline private class SyncEffectRenderAsClass[Env, F[_]: RunSyncEffect, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  @inline implicit def FutureRenderAs[Env, T: Render[Env, *]]: Render[Env, Future[T]] = new FutureRenderAsClass[Env, T]
  @inline private class FutureRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRenderAs[Env, F[_]: Source, T: Render[Env, *]]: Render[Env, F[T]] =
    new SourceRenderAsClass[Env, F, T]
  @inline private class SourceRenderAsClass[Env, F[_]: Source, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(source: F[T]) = sourceToModifierRender(source)
  }
}

object Render extends RenderLowPrio {
  @inline def apply[Env, T](implicit render: Render[Env, T]): Render[Env, T] = render

  import RenderOps._

  implicit object JsArrayModifier extends Render[Any, js.Array[VMod]] {
    @inline def render(value: js.Array[VMod]): VMod = CompositeModifier(value)
  }

  implicit object ArrayModifier extends Render[Any, Array[VMod]] {
    @inline def render(value: Array[VMod]): VMod = CompositeModifier(value)
  }

  implicit object SeqModifier extends Render[Any, Seq[VMod]] {
    @inline def render(value: Seq[VMod]): VMod = CompositeModifier(value)
  }

  implicit object NonEmptyListModifier extends Render[Any, NonEmptyList[VMod]] {
    @inline def render(value: NonEmptyList[VMod]): VMod = CompositeModifier(value.toList)
  }

  implicit object NonEmptyVectorModifier extends Render[Any, NonEmptyVector[VMod]] {
    @inline def render(value: NonEmptyVector[VMod]): VMod = CompositeModifier(value.toVector)
  }

  implicit object NonEmptySeqModifier extends Render[Any, NonEmptySeq[VMod]] {
    @inline def render(value: NonEmptySeq[VMod]): VMod = CompositeModifier(value.toSeq)
  }

  implicit object NonEmptyChainModifier extends Render[Any, NonEmptyChain[VMod]] {
    @inline def render(value: NonEmptyChain[VMod]): VMod = CompositeModifier(value.toChain.toList)
  }

  implicit object OptionModifier extends Render[Any, Option[VMod]] {
    @inline def render(value: Option[VMod]): VMod = value.getOrElse(VMod.empty)
  }

  implicit object UndefinedModifier extends Render[Any, js.UndefOr[VMod]] {
    @inline def render(value: js.UndefOr[VMod]): VMod = value.getOrElse(VMod.empty)
  }

  @inline implicit def vModifierMRender[Env]: Render[Env, VModM[Env]] = new VModMRenderClass[Env]
  @inline private class VModMRenderClass[Env] extends Render[Env, VModM[Env]] {
    @inline def render(value: VModM[Env]) = value
  }

  implicit object VModRender extends Render[Any, VMod] {
    @inline def render(value: VMod): VMod = value
  }

  implicit object StringRender extends Render[Any, String] {
    @inline def render(value: String): VMod = StringVNode(value)
  }

  implicit object IntRender extends Render[Any, Int] {
    @inline def render(value: Int): VMod = StringVNode(value.toString)
  }

  implicit object DoubleRender extends Render[Any, Double] {
    @inline def render(value: Double): VMod = StringVNode(
      value.toString,
    )
  }

  implicit object LongRender extends Render[Any, Long] {
    @inline def render(value: Long): VMod = StringVNode(value.toString)
  }

  implicit object BooleanRender extends Render[Any, Boolean] {
    @inline def render(value: Boolean): VMod = StringVNode(
      value.toString,
    )
  }

  implicit object SyncIORender extends Render[Any, SyncIO[VMod]] {
    @inline def render(future: SyncIO[VMod]) = syncToModifier(future)
  }

  @inline implicit def SyncEffectRender[F[_]: RunSyncEffect]: Render[Any, F[VMod]] = new SyncEffectRenderClass[F]
  @inline private class SyncEffectRenderClass[F[_]: RunSyncEffect] extends Render[Any, F[VMod]] {
    @inline def render(effect: F[VMod]) = syncToModifier(effect)
  }

  implicit object IORender extends Render[Any, IO[VMod]] {
    @inline def render(future: IO[VMod]) = effectToModifier(future)
  }

  implicit object FutureRender extends Render[Any, Future[VMod]] {
    @inline def render(future: Future[VMod]) = futureToModifier(future)
  }

  implicit object ObservableRender extends Render[Any, Observable[VMod]] {
    @inline def render(source: Observable[VMod]) = sourceToModifier(source)
  }

  @inline implicit def SourceRender[F[_]: Source]: Render[Any, F[VMod]] = new SourceRenderClass[F]
  @inline private class SourceRenderClass[F[_]: Source] extends Render[Any, F[VMod]] {
    @inline def render(source: F[VMod]) = sourceToModifier(source)
  }

  @inline implicit def KleisliRenderAs[F[_], Env, T](implicit r: Render[Any, F[T]]): Render[Env, Kleisli[F, Env, T]] =
    new KleisliRenderAsClass[F, Env, T]
  @inline private final class KleisliRenderAsClass[F[_], Env, T](implicit r: Render[Any, F[T]])
      extends Render[Env, Kleisli[F, Env, T]] {
    @inline def render(kleisli: Kleisli[F, Env, T]) = VModM.access[Env](env => r.render(kleisli.run(env)))
  }

  @inline implicit def EmitterBuilderRender: Render[Any, EmitterBuilder[VMod, VMod]] =
    new EmitterBuilderRender
  @inline private final class EmitterBuilderRender extends Render[Any, EmitterBuilder[VMod, VMod]] {
    @inline def render(builder: EmitterBuilder[VMod, VMod]) = builder.render
  }

  @inline implicit def AttrBuilderRender: Render[Any, AttrBuilder[Boolean, VMod]] =
    new AttrBuilderRender
  @inline private final class AttrBuilderRender extends Render[Any, AttrBuilder[Boolean, VMod]] {
    @inline def render(builder: AttrBuilder[Boolean, VMod]) = builder := true
  }

  @inline implicit def ChildCommandSourceRender[F[_]: Source]: Render[Any, F[ChildCommand]] =
    new ChildCommandRenderClass[F]
  @inline private class ChildCommandRenderClass[F[_]: Source] extends Render[Any, F[ChildCommand]] {
    @inline def render(source: F[ChildCommand]) = childCommandToModifier(source)
  }

  @inline implicit def ChildCommandSeqSourceRender[F[_]: Source]: Render[Any, F[Seq[ChildCommand]]] =
    new ChildCommandSeqRenderClass[F]
  @inline private class ChildCommandSeqRenderClass[F[_]: Source] extends Render[Any, F[Seq[ChildCommand]]] {
    @inline def render(source: F[Seq[ChildCommand]]) = childCommandSeqToModifier(source)
  }
}

private object RenderOps {
  def iterableToModifierRender[Env, T: Render[Env, *]](value: Iterable[T]): VModM[Env] = CompositeModifier(
    value.map(VModM(_)),
  )
  def optionToModifierRender[Env, T: Render[Env, *]](value: Option[T]): VModM[Env] =
    value.fold[VModM[Env]](VModM.empty)(VModM(_))
  def undefinedToModifierRender[Env, T: Render[Env, *]](value: js.UndefOr[T]): VModM[Env] =
    value.fold[VModM[Env]](VModM.empty)(VModM(_))
  def syncToModifierRender[F[_]: RunSyncEffect, Env, T: Render[Env, *]](effect: F[T]): VModM[Env] =
    VModM.delayEither(RunSyncEffect[F].unsafeRun(effect))
  def syncToModifier[F[_]: RunSyncEffect, Env](effect: F[VModM[Env]]): VModM[Env] =
    VModM.delayEither(RunSyncEffect[F].unsafeRun(effect))
  def effectToModifier[F[_]: RunEffect, Env](effect: F[VModM[Env]]): VModM[Env] = StreamModifier(
    Observable.fromEffect(effect).unsafeSubscribe(_),
  )
  def effectToModifierRender[F[_]: RunEffect, Env, T: Render[Env, *]](effect: F[T]): VModM[Env] =
    StreamModifier(obs => Observable.fromEffect(effect).unsafeSubscribe(obs.contramap(VMod(_))))
  def sourceToModifier[F[_]: Source, Env](source: F[VModM[Env]]): VModM[Env] = StreamModifier(
    Source[F].unsafeSubscribe(source),
  )
  def sourceToModifierRender[F[_]: Source, Env, T: Render[Env, *]](source: F[T]): VModM[Env] =
    StreamModifier(sink => Source[F].unsafeSubscribe(source)(sink.contramap(VModM(_))))
  def childCommandSeqToModifier[F[_]: Source, Env](source: F[Seq[ChildCommand]]): VModM[Env] =
    ChildCommandsModifier(Observable.lift(source))
  def childCommandToModifier[F[_]: Source, Env](source: F[ChildCommand]): VModM[Env] = ChildCommandsModifier(
    Observable.lift(source).map(Seq(_)),
  )
  def futureToModifierRender[Env, T: Render[Env, *]](future: Future[T]): VModM[Env] = future.value match {
    case Some(Success(value)) => VModM(value)
    case _                    => StreamModifier(Observable.fromFuture(future).map(VModM(_)).unsafeSubscribe(_))
  }
  def futureToModifier[Env](future: Future[VModM[Env]]): VModM[Env] = future.value match {
    case Some(Success(value)) => value
    case _                    => StreamModifier(Observable.fromFuture(future).unsafeSubscribe(_))
  }
}
