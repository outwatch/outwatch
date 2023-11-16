package outwatch

import colibri._
import colibri.effect._
import cats.Show
import cats.data.{Chain, Kleisli, NonEmptyChain, NonEmptyList, NonEmptySeq, NonEmptyVector}
import cats.effect.{unsafe, IO, Resource, Sync, SyncIO}

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.Success

trait Render[-Env, -T] {
  def render(value: T): VModM[Env]
}

trait RenderLowPrio1 {
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

  @inline implicit def EffectUnitRender[F[_]: RunEffect]: Render[Any, F[Unit]] = new EffectUnitRenderClass[F]
  @inline private class EffectUnitRenderClass[F[_]: RunEffect] extends Render[Any, F[Unit]] {
    @inline def render(source: F[Unit]) = VMod.managedSubscribe(Observable.fromEffect(source))
  }

  @inline implicit def SourceUnitRender[F[_]: Source]: Render[Any, F[Unit]] = new SourceUnitRenderClass[F]
  @inline private class SourceUnitRenderClass[F[_]: Source] extends Render[Any, F[Unit]] {
    @inline def render(source: F[Unit]) = VMod.managedSubscribe(source)
  }
}

trait RenderLowPrio0 extends RenderLowPrio1 {
  import RenderOps._

  implicit val SyncIOUnitRender: Render[Any, SyncIO[Unit]] = new Render[Any, SyncIO[Unit]] {
    @inline def render(effect: SyncIO[Unit]) = VMod.managedSubscribe(Observable.fromEffect(effect))
  }

  implicit val IOUnitRender: Render[Any, IO[Unit]] = new Render[Any, IO[Unit]] {
    @inline def render(effect: IO[Unit]) = VMod.managedSubscribe(Observable.fromEffect(effect))
  }

  implicit val IORender: Render[Any, IO[VMod]] = new Render[Any, IO[VMod]] {
    @inline def render(effect: IO[VMod]) = effectToModifier(effect)
  }

  implicit val ObservableUnitRender: Render[Any, Observable[Unit]] = new Render[Any, Observable[Unit]] {
    @inline def render(source: Observable[Unit]) = VMod.managedSubscribe(source)
  }
}

trait RenderLowPrio extends RenderLowPrio0 {
  import RenderOps._

  @inline implicit def IOUnitRenderIORuntime(implicit ioRuntime: unsafe.IORuntime): Render[Any, IO[Unit]] =
    new IOUnitRenderIORuntimeClass
  @inline private class IOUnitRenderIORuntimeClass(implicit ioRuntime: unsafe.IORuntime) extends Render[Any, IO[Unit]] {
    @inline def render(effect: IO[Unit]) = VMod.managedSubscribe(Observable.fromEffect(effect))
  }

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
    @inline def render(value: Seq[T]): VModM[Env] = iterableToModifierRender(value)
  }

  @inline implicit def ChainModifierAs[Env, T: Render[Env, *]]: Render[Env, Chain[T]] = new ChainRenderAsClass[Env, T]
  @inline private class ChainRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, Chain[T]] {
    @inline def render(value: Chain[T]) = iterableToModifierRender(value.toList)
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

  @inline implicit def SyncEffectRender[F[_]: RunSyncEffect]: Render[Any, F[VMod]] = new SyncEffectRenderClass[F]
  @inline private class SyncEffectRenderClass[F[_]: RunSyncEffect] extends Render[Any, F[VMod]] {
    @inline def render(effect: F[VMod]) = syncToModifier(effect)
  }

  @inline implicit def SyncEffectRenderAs[Env, F[_]: RunSyncEffect, T: Render[Env, *]]: Render[Env, F[T]] =
    new SyncEffectRenderAsClass[Env, F, T]
  @inline private class SyncEffectRenderAsClass[Env, F[_]: RunSyncEffect, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  @inline implicit def ResourceRender[F[_]: RunEffect: Sync]: Render[Any, Resource[F, VMod]] =
    new ResourceRenderClass[F]
  @inline private class ResourceRenderClass[F[_]: RunEffect: Sync] extends Render[Any, Resource[F, VMod]] {
    @inline def render(resource: Resource[F, VMod]) = resourceToModifier(resource)
  }

  @inline implicit def ResourceRenderAs[Env, F[_]: RunEffect: Sync, T: Render[Env, *]]: Render[Env, Resource[F, T]] =
    new ResourceRenderAsClass[Env, F, T]
  @inline private class ResourceRenderAsClass[Env, F[_]: RunEffect: Sync, T: Render[Env, *]]
      extends Render[Env, Resource[F, T]] {
    @inline def render(resource: Resource[F, T]) = resourceToModifierRender(resource)
  }

  @inline implicit def FutureRenderAs[Env, T: Render[Env, *]]: Render[Env, Future[T]] = new FutureRenderAsClass[Env, T]
  @inline private class FutureRenderAsClass[Env, T: Render[Env, *]] extends Render[Env, Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRender[F[_]: Source]: Render[Any, F[VMod]] = new SourceRenderClass[F]
  @inline private class SourceRenderClass[F[_]: Source] extends Render[Any, F[VMod]] {
    @inline def render(source: F[VMod]) = sourceToModifier(source)
  }

  @inline implicit def SourceRenderAs[Env, F[_]: Source, T: Render[Env, *]]: Render[Env, F[T]] =
    new SourceRenderAsClass[Env, F, T]
  @inline private class SourceRenderAsClass[Env, F[_]: Source, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(source: F[T]) = sourceToModifierRender(source)
  }

  @inline implicit def VModMRender[Env]: Render[Env, VModM[Env]] = new VModMRenderClass[Env]
  @inline private class VModMRenderClass[Env] extends Render[Env, VModM[Env]] {
    @inline def render(value: VModM[Env]) = value
  }
}

object Render extends RenderLowPrio {
  @inline def apply[Env, T](implicit render: Render[Env, T]): Render[Env, T] = render

  @inline def show[T: Show]: Render[Any, T] = new ShowRender[T]
  @inline private class ShowRender[T: Show] extends Render[Any, T] {
    @inline def render(value: T): VMod = StringVNode(Show[T].show(value))
  }

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

  implicit object ChainModifier extends Render[Any, Chain[VMod]] {
    @inline def render(value: Chain[VMod]): VMod = CompositeModifier(value.toList)
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

  @inline implicit def IORenderIORuntime(implicit ioRuntime: unsafe.IORuntime): Render[Any, IO[VMod]] =
    new IORenderIORuntimeClass
  @inline private class IORenderIORuntimeClass(implicit ioRuntime: unsafe.IORuntime) extends Render[Any, IO[VMod]] {
    @inline def render(effect: IO[VMod]) = effectToModifier(effect)
  }

  implicit object FutureRender extends Render[Any, Future[VMod]] {
    @inline def render(future: Future[VMod]) = futureToModifier(future)
  }

  implicit object ObservableRender extends Render[Any, Observable[VMod]] {
    @inline def render(source: Observable[VMod]) = sourceToModifier(source)
  }

  @inline implicit def AttrBuilderRender: Render[Any, AttrBuilder[Boolean, VMod]] = new AttrBuilderRender
  @inline private final class AttrBuilderRender extends Render[Any, AttrBuilder[Boolean, VMod]] {
    @inline def render(builder: AttrBuilder[Boolean, VMod]) = builder := true
  }

  @inline implicit def EmitterBuilderRender: Render[Any, EmitterBuilder[VMod, VMod]] = new EmitterBuilderRender
  @inline private final class EmitterBuilderRender extends Render[Any, EmitterBuilder[VMod, VMod]] {
    @inline def render(builder: EmitterBuilder[VMod, VMod]) = builder.render
  }

  @inline implicit def KleisliRenderAs[F[_], Env, T](implicit r: Render[Any, F[T]]): Render[Env, Kleisli[F, Env, T]] =
    new KleisliRenderAsClass[F, Env, T]
  @inline private final class KleisliRenderAsClass[F[_], Env, T](implicit r: Render[Any, F[T]])
      extends Render[Env, Kleisli[F, Env, T]] {
    @inline def render(kleisli: Kleisli[F, Env, T]) = VModM.access[Env](env => r.render(kleisli.run(env)))
  }
  @inline implicit def ChildCommandSourceRender[F[_]: Source]: Render[Any, F[ChildCommand]] =
    new ChildCommandRenderClass[F]
  @inline private class ChildCommandRenderClass[F[_]: Source] extends Render[Any, F[ChildCommand]] {
    @inline def render(source: F[ChildCommand]): VMod = childCommandToModifier(source)
  }

  @inline implicit def ChildCommandSeqSourceRender[F[_]: Source]: Render[Any, F[Seq[ChildCommand]]] =
    new ChildCommandSeqRenderClass[F]
  @inline private class ChildCommandSeqRenderClass[F[_]: Source] extends Render[Any, F[Seq[ChildCommand]]] {
    @inline def render(source: F[Seq[ChildCommand]]): VMod = childCommandSeqToModifier(source)
  }
}

private object RenderOps {
  @noinline def iterableToModifierRender[Env, T: Render[Env, *]](value: Iterable[T]): VModM[Env] =
    CompositeModifier(value.map(VMod(_)))
  @noinline def optionToModifierRender[Env, T: Render[Env, *]](value: Option[T]): VModM[Env] =
    value.fold[VModM[Env]](VMod.empty)(VMod(_))
  @noinline def undefinedToModifierRender[Env, T: Render[Env, *]](value: js.UndefOr[T]): VModM[Env] =
    value.fold[VModM[Env]](VMod.empty)(VMod(_))
  @noinline def syncToModifierRender[Env, F[_]: RunSyncEffect, T: Render[Env, *]](effect: F[T]): VModM[Env] =
    VMod.evalEither(RunSyncEffect[F].unsafeRun(effect))
  @noinline def syncToModifier[Env, F[_]: RunSyncEffect](effect: F[VModM[Env]]): VModM[Env] =
    VMod.evalEither(RunSyncEffect[F].unsafeRun(effect))
  @noinline def effectToModifier[Env, F[_]: RunEffect](effect: F[VModM[Env]]): VModM[Env] =
    StreamModifier(Observable.fromEffect(effect).unsafeSubscribe(_))
  @noinline def effectToModifierRender[Env, F[_]: RunEffect, T: Render[Env, *]](effect: F[T]): VModM[Env] =
    StreamModifier(obs => Observable.fromEffect(effect).unsafeSubscribe(obs.contramap(VMod(_))))
  @noinline def resourceToModifier[Env, F[_]: RunEffect: Sync](resource: Resource[F, VModM[Env]]): VModM[Env] =
    StreamModifier(Observable.fromResource(resource).unsafeSubscribe(_))
  @noinline def resourceToModifierRender[Env, F[_]: RunEffect: Sync, T: Render[Env, *]](
    resource: Resource[F, T],
  ): VModM[Env] =
    StreamModifier(obs => Observable.fromResource(resource).unsafeSubscribe(obs.contramap(VMod(_))))
  @noinline def sourceToModifier[Env, F[_]: Source](source: F[VModM[Env]]): VModM[Env] =
    StreamModifier(Source[F].unsafeSubscribe(source))
  @noinline def sourceToModifierRender[Env, F[_]: Source, T: Render[Env, *]](source: F[T]): VModM[Env] =
    StreamModifier(sink => Source[F].unsafeSubscribe(source)(sink.contramap(VMod(_))))
  @noinline def childCommandSeqToModifier[F[_]: Source](source: F[Seq[ChildCommand]]): VModM[Any] =
    ChildCommandsModifier(Observable.lift(source))
  @noinline def childCommandToModifier[F[_]: Source](source: F[ChildCommand]): VModM[Any] =
    ChildCommandsModifier(Observable.lift(source).map(Seq(_)))
  @noinline def futureToModifierRender[Env, T: Render[Env, *]](future: Future[T]): VModM[Env] =
    future.value match {
      case Some(Success(value)) => VMod(value)
      case _                    => StreamModifier(Observable.fromFuture(future).map(VMod(_)).unsafeSubscribe(_))
    }
  @noinline def futureToModifier[Env](future: Future[VModM[Env]]): VModM[Env] = future.value match {
    case Some(Success(value)) => value
    case _                    => StreamModifier(Observable.fromFuture(future).unsafeSubscribe(_))
  }
}
