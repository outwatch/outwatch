package outwatch

import colibri._
import colibri.effect._
import cats.Show
import cats.data.{Chain, NonEmptyChain, NonEmptyList, NonEmptySeq, NonEmptyVector}
import cats.effect.{IO, Resource, Sync, SyncIO}

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.Success

trait Render[-T] {
  def render(value: T): VMod
}

trait RenderLowPrio1 {
  import RenderOps._

  @inline implicit def UndefinedModifierAs[T: Render]: Render[js.UndefOr[T]] = new UndefinedRenderAsClass[T]
  @inline private class UndefinedRenderAsClass[T: Render] extends Render[js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  implicit object UndefinedModifier extends Render[js.UndefOr[VMod]] {
    @inline def render(value: js.UndefOr[VMod]): VMod = value.getOrElse(VMod.empty)
  }
}

trait RenderLowPrio0 extends RenderLowPrio1 {
  import RenderOps._

  @inline implicit def EffectRender[F[_]: RunEffect]: Render[F[VMod]] = new EffectRenderClass[F]
  @inline private class EffectRenderClass[F[_]: RunEffect] extends Render[F[VMod]] {
    @inline def render(effect: F[VMod]) = effectToModifier(effect)
  }

  @inline implicit def EffectRenderAs[F[_]: RunEffect, T: Render]: Render[F[T]] = new EffectRenderAsClass[F, T]
  @inline private class EffectRenderAsClass[F[_]: RunEffect, T: Render] extends Render[F[T]] {
    @inline def render(effect: F[T]) = effectToModifierRender(effect)
  }
}

trait RenderLowPrio extends RenderLowPrio0 {
  import RenderOps._

  @inline implicit def JsArrayModifierAs[T: Render]: Render[js.Array[T]] = new JsArrayRenderAsClass[T]
  @inline private class JsArrayRenderAsClass[T: Render] extends Render[js.Array[T]] {
    @inline def render(value: js.Array[T]): VMod = iterableToModifierRender(value)
  }

  @inline implicit def ArrayModifierAs[T: Render]: Render[Array[T]] = new ArrayRenderAsClass[T]
  @inline private class ArrayRenderAsClass[T: Render] extends Render[Array[T]] {
    @inline def render(value: Array[T]): VMod = iterableToModifierRender(value)
  }

  @inline implicit def SeqModifierAs[T: Render]: Render[Seq[T]] = new SeqRenderAsClass[T]
  @inline private class SeqRenderAsClass[T: Render] extends Render[Seq[T]] {
    @inline def render(value: Seq[T]): VMod = iterableToModifierRender(value)
  }

  @inline implicit def ChainModifierAs[T: Render]: Render[Chain[T]] = new ChainRenderAsClass[T]
  @inline private class ChainRenderAsClass[T: Render] extends Render[Chain[T]] {
    @inline def render(value: Chain[T]) = iterableToModifierRender(value.toList)
  }

  @inline implicit def NonEmptyListModifierAs[T: Render]: Render[NonEmptyList[T]] = new NonEmptyListRenderAsClass[T]
  @inline private class NonEmptyListRenderAsClass[T: Render] extends Render[NonEmptyList[T]] {
    @inline def render(value: NonEmptyList[T]) = iterableToModifierRender(value.toList)
  }

  @inline implicit def NonEmptyVectorModifierAs[T: Render]: Render[NonEmptyVector[T]] =
    new NonEmptyVectorRenderAsClass[T]
  @inline private class NonEmptyVectorRenderAsClass[T: Render] extends Render[NonEmptyVector[T]] {
    @inline def render(value: NonEmptyVector[T]) = iterableToModifierRender(value.toVector)
  }

  @inline implicit def NonEmptySeqModifierAs[T: Render]: Render[NonEmptySeq[T]] = new NonEmptySeqRenderAsClass[T]
  @inline private class NonEmptySeqRenderAsClass[T: Render] extends Render[NonEmptySeq[T]] {
    @inline def render(value: NonEmptySeq[T]) = iterableToModifierRender(value.toSeq)
  }

  @inline implicit def NonEmptyChainModifierAs[T: Render]: Render[NonEmptyChain[T]] = new NonEmptyChainRenderAsClass[T]
  @inline private class NonEmptyChainRenderAsClass[T: Render] extends Render[NonEmptyChain[T]] {
    @inline def render(value: NonEmptyChain[T]) = iterableToModifierRender(value.toChain.toList)
  }

  @inline implicit def OptionModifierAs[T: Render]: Render[Option[T]] = new OptionRenderAsClass[T]
  @inline private class OptionRenderAsClass[T: Render] extends Render[Option[T]] {
    @inline def render(value: Option[T]): VMod = optionToModifierRender(value)
  }

  @inline implicit def SyncEffectRenderAs[F[_]: RunSyncEffect, T: Render]: Render[F[T]] =
    new SyncEffectRenderAsClass[F, T]
  @inline private class SyncEffectRenderAsClass[F[_]: RunSyncEffect, T: Render] extends Render[F[T]] {
    @inline def render(effect: F[T]): VMod = syncToModifierRender(effect)
  }

  @inline implicit def ResourceRender[F[_]: RunEffect: Sync]: Render[Resource[F, VMod]] =
    new ResourceRenderClass[F]
  @inline private class ResourceRenderClass[F[_]: RunEffect: Sync] extends Render[Resource[F, VMod]] {
    @inline def render(resource: Resource[F, VMod]) = resourceToModifier(resource)
  }

  @inline implicit def ResourceRenderAs[F[_]: RunEffect: Sync, T: Render]: Render[Resource[F, T]] =
    new ResourceRenderAsClass[F, T]
  @inline private class ResourceRenderAsClass[F[_]: RunEffect: Sync, T: Render] extends Render[Resource[F, T]] {
    @inline def render(resource: Resource[F, T]) = resourceToModifierRender(resource)
  }

  @inline implicit def FutureRenderAs[T: Render]: Render[Future[T]] = new FutureRenderAsClass[T]
  @inline private class FutureRenderAsClass[T: Render] extends Render[Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRenderAs[F[_]: Source, T: Render]: Render[F[T]] = new SourceRenderAsClass[F, T]
  @inline private class SourceRenderAsClass[F[_]: Source, T: Render] extends Render[F[T]] {
    @inline def render(source: F[T]): VMod = sourceToModifierRender(source)
  }
}

object Render extends RenderLowPrio {
  @inline def apply[T](implicit render: Render[T]): Render[T] = render

  @inline def show[T: Show]: Render[T] = new ShowRender[T]
  @inline private class ShowRender[T: Show] extends Render[T] {
    @inline def render(value: T): VMod = StringVNode(Show[T].show(value))
  }

  import RenderOps._

  implicit object JsArrayModifier extends Render[js.Array[VMod]] {
    @inline def render(value: js.Array[VMod]): VMod = CompositeModifier(value)
  }

  implicit object ArrayModifier extends Render[Array[VMod]] {
    @inline def render(value: Array[VMod]): VMod = CompositeModifier(value)
  }

  implicit object SeqModifier extends Render[Seq[VMod]] {
    @inline def render(value: Seq[VMod]): VMod = CompositeModifier(value)
  }

  implicit object ChainModifier extends Render[Chain[VMod]] {
    @inline def render(value: Chain[VMod]): VMod = CompositeModifier(value.toList)
  }

  implicit object NonEmptyListModifier extends Render[NonEmptyList[VMod]] {
    @inline def render(value: NonEmptyList[VMod]): VMod = CompositeModifier(value.toList)
  }

  implicit object NonEmptyVectorModifier extends Render[NonEmptyVector[VMod]] {
    @inline def render(value: NonEmptyVector[VMod]): VMod = CompositeModifier(value.toVector)
  }

  implicit object NonEmptySeqModifier extends Render[NonEmptySeq[VMod]] {
    @inline def render(value: NonEmptySeq[VMod]): VMod = CompositeModifier(value.toSeq)
  }

  implicit object NonEmptyChainModifier extends Render[NonEmptyChain[VMod]] {
    @inline def render(value: NonEmptyChain[VMod]): VMod = CompositeModifier(value.toChain.toList)
  }

  implicit object OptionModifier extends Render[Option[VMod]] {
    @inline def render(value: Option[VMod]): VMod = value.getOrElse(VMod.empty)
  }

  implicit object VModRender extends Render[VMod] {
    @inline def render(value: VMod): VMod = value
  }

  implicit object StringRender extends Render[String] {
    @inline def render(value: String): VMod = StringVNode(value)
  }

  implicit object IntRender extends Render[Int] {
    @inline def render(value: Int): VMod = StringVNode(value.toString)
  }

  implicit object DoubleRender extends Render[Double] {
    @inline def render(value: Double): VMod = StringVNode(value.toString)
  }

  implicit object LongRender extends Render[Long] {
    @inline def render(value: Long): VMod = StringVNode(value.toString)
  }

  implicit object BooleanRender extends Render[Boolean] {
    @inline def render(value: Boolean): VMod = StringVNode(value.toString)
  }

  implicit object SyncIORender extends Render[SyncIO[VMod]] {
    @inline def render(future: SyncIO[VMod]) = syncToModifier(future)
  }

  @inline implicit def SyncEffectRender[F[_]: RunSyncEffect]: Render[F[VMod]] = new SyncEffectRenderClass[F]
  @inline private class SyncEffectRenderClass[F[_]: RunSyncEffect] extends Render[F[VMod]] {
    @inline def render(effect: F[VMod]) = syncToModifier(effect)
  }

  implicit object IORender extends Render[IO[VMod]] {
    @inline def render(future: IO[VMod]) = effectToModifier(future)
  }

  implicit object FutureRender extends Render[Future[VMod]] {
    @inline def render(future: Future[VMod]) = futureToModifier(future)
  }

  implicit object ObservableRender extends Render[Observable[VMod]] {
    @inline def render(source: Observable[VMod]) = sourceToModifier(source)
  }

  @inline implicit def SourceRender[F[_]: Source]: Render[F[VMod]] = new SourceRenderClass[F]
  @inline private class SourceRenderClass[F[_]: Source] extends Render[F[VMod]] {
    @inline def render(source: F[VMod]) = sourceToModifier(source)
  }

  @inline implicit def AttrBuilderRender: Render[AttrBuilder[Boolean, VMod]] = new AttrBuilderRender
  @inline private final class AttrBuilderRender extends Render[AttrBuilder[Boolean, VMod]] {
    @inline def render(builder: AttrBuilder[Boolean, VMod]) = builder := true
  }

  @inline implicit def EmitterBuilderRender: Render[EmitterBuilder[VMod, VMod]] = new EmitterBuilderRender
  @inline private final class EmitterBuilderRender extends Render[EmitterBuilder[VMod, VMod]] {
    @inline def render(builder: EmitterBuilder[VMod, VMod]) = builder.render
  }

  @inline implicit def ChildCommandSourceRender[F[_]: Source]: Render[F[ChildCommand]] = new ChildCommandRenderClass[F]
  @inline private class ChildCommandRenderClass[F[_]: Source] extends Render[F[ChildCommand]] {
    @inline def render(source: F[ChildCommand]): VMod = childCommandToModifier(source)
  }

  @inline implicit def ChildCommandSeqSourceRender[F[_]: Source]: Render[F[Seq[ChildCommand]]] =
    new ChildCommandSeqRenderClass[F]
  @inline private class ChildCommandSeqRenderClass[F[_]: Source] extends Render[F[Seq[ChildCommand]]] {
    @inline def render(source: F[Seq[ChildCommand]]): VMod = childCommandSeqToModifier(source)
  }
}

private object RenderOps {
  @noinline def iterableToModifierRender[T: Render](value: Iterable[T]): VMod = CompositeModifier(
    value.map(VMod(_)),
  )
  @noinline def optionToModifierRender[T: Render](value: Option[T]): VMod =
    value.fold(VMod.empty)(VMod(_))
  @noinline def undefinedToModifierRender[T: Render](value: js.UndefOr[T]): VMod =
    value.fold(VMod.empty)(VMod(_))
  @noinline def syncToModifierRender[F[_]: RunSyncEffect, T: Render](effect: F[T]): VMod =
    VMod.evalEither(RunSyncEffect[F].unsafeRun(effect))
  @noinline def syncToModifier[F[_]: RunSyncEffect](effect: F[VMod]): VMod =
    VMod.evalEither(RunSyncEffect[F].unsafeRun(effect))
  @noinline def effectToModifier[F[_]: RunEffect](effect: F[VMod]): VMod = StreamModifier(
    Observable.fromEffect(effect).unsafeSubscribe(_),
  )
  @noinline def effectToModifierRender[F[_]: RunEffect, T: Render](effect: F[T]): VMod =
    StreamModifier(obs => Observable.fromEffect(effect).unsafeSubscribe(obs.contramap(VMod(_))))
  @noinline def resourceToModifier[F[_]: RunEffect: Sync](resource: Resource[F, VMod]): VMod = StreamModifier(
    Observable.fromResource(resource).unsafeSubscribe(_),
  )
  @noinline def resourceToModifierRender[F[_]: RunEffect: Sync, T: Render](resource: Resource[F, T]): VMod =
    StreamModifier(obs => Observable.fromResource(resource).unsafeSubscribe(obs.contramap(VMod(_))))
  @noinline def sourceToModifier[F[_]: Source](source: F[VMod]): VMod = StreamModifier(
    Source[F].unsafeSubscribe(source),
  )
  @noinline def sourceToModifierRender[F[_]: Source, T: Render](source: F[T]): VMod =
    StreamModifier(sink => Source[F].unsafeSubscribe(source)(sink.contramap(VMod(_))))
  @noinline def childCommandSeqToModifier[F[_]: Source](source: F[Seq[ChildCommand]]): VMod =
    ChildCommandsModifier(Observable.lift(source))
  @noinline def childCommandToModifier[F[_]: Source](source: F[ChildCommand]): VMod = ChildCommandsModifier(
    Observable.lift(source).map(Seq(_)),
  )
  @noinline def futureToModifierRender[T: Render](future: Future[T]): VMod = future.value match {
    case Some(Success(value)) => VMod(value)
    case _                    => StreamModifier(Observable.fromFuture(future).map(VMod(_)).unsafeSubscribe(_))
  }
  @noinline def futureToModifier(future: Future[VMod]): VMod = future.value match {
    case Some(Success(value)) => value
    case _                    => StreamModifier(Observable.fromFuture(future).unsafeSubscribe(_))
  }
}
