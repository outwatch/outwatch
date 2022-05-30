package outwatch

import outwatch.helpers.AttributeBuilder

import colibri._
import colibri.effect._
import cats.data.{NonEmptyChain, NonEmptyList, NonEmptySeq, NonEmptyVector}
import cats.effect.{IO, Resource, Sync, SyncIO}

import scala.scalajs.js
import scala.concurrent.Future
import scala.util.Success

trait Render[-T] {
  def render(value: T): VModifier
}

trait RenderLowPrio1 {
  import RenderOps._

  @inline implicit def UndefinedModifierAs[T: Render]: Render[js.UndefOr[T]] = new UndefinedRenderAsClass[T]
  @inline private class UndefinedRenderAsClass[T: Render] extends Render[js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  implicit object UndefinedModifier extends Render[js.UndefOr[VModifier]] {
    @inline def render(value: js.UndefOr[VModifier]): VModifier = value.getOrElse(VModifier.empty)
  }
}

trait RenderLowPrio0 extends RenderLowPrio1 {
  import RenderOps._

  @inline implicit def EffectRender[F[_]: RunEffect]: Render[F[VModifier]] = new EffectRenderClass[F]
  @inline private class EffectRenderClass[F[_]: RunEffect] extends Render[F[VModifier]] {
    @inline def render(effect: F[VModifier]) = effectToModifier(effect)
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
    @inline def render(value: js.Array[T]): VModifier = iterableToModifierRender(value)
  }

  @inline implicit def ArrayModifierAs[T: Render]: Render[Array[T]] = new ArrayRenderAsClass[T]
  @inline private class ArrayRenderAsClass[T: Render] extends Render[Array[T]] {
    @inline def render(value: Array[T]): VModifier = iterableToModifierRender(value)
  }

  @inline implicit def SeqModifierAs[T: Render]: Render[Seq[T]] = new SeqRenderAsClass[T]
  @inline private class SeqRenderAsClass[T: Render] extends Render[Seq[T]] {
    @inline def render(value: Seq[T]): VModifier = iterableToModifierRender(value)
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
    @inline def render(value: Option[T]): VModifier = optionToModifierRender(value)
  }

  @inline implicit def SyncEffectRenderAs[F[_]: RunSyncEffect, T: Render]: Render[F[T]] =
    new SyncEffectRenderAsClass[F, T]
  @inline private class SyncEffectRenderAsClass[F[_]: RunSyncEffect, T: Render] extends Render[F[T]] {
    @inline def render(effect: F[T]): VModifier = syncToModifierRender(effect)
  }

  @inline implicit def ResourceRender[F[_]: RunEffect: Sync]: Render[Resource[F, VModifier]] =
    new ResourceRenderClass[F]
  @inline private class ResourceRenderClass[F[_]: RunEffect: Sync] extends Render[Resource[F, VModifier]] {
    @inline def render(resource: Resource[F, VModifier]) = resourceToModifier(resource)
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
    @inline def render(source: F[T]): VModifier = sourceToModifierRender(source)
  }
}

object Render extends RenderLowPrio {
  @inline def apply[T](implicit render: Render[T]): Render[T] = render

  import RenderOps._

  implicit object JsArrayModifier extends Render[js.Array[VModifier]] {
    @inline def render(value: js.Array[VModifier]): VModifier = CompositeModifier(value)
  }

  implicit object ArrayModifier extends Render[Array[VModifier]] {
    @inline def render(value: Array[VModifier]): VModifier = CompositeModifier(value)
  }

  implicit object SeqModifier extends Render[Seq[VModifier]] {
    @inline def render(value: Seq[VModifier]): VModifier = CompositeModifier(value)
  }

  implicit object NonEmptyListModifier extends Render[NonEmptyList[VModifier]] {
    @inline def render(value: NonEmptyList[VModifier]): VModifier = CompositeModifier(value.toList)
  }

  implicit object NonEmptyVectorModifier extends Render[NonEmptyVector[VModifier]] {
    @inline def render(value: NonEmptyVector[VModifier]): VModifier = CompositeModifier(value.toVector)
  }

  implicit object NonEmptySeqModifier extends Render[NonEmptySeq[VModifier]] {
    @inline def render(value: NonEmptySeq[VModifier]): VModifier = CompositeModifier(value.toSeq)
  }

  implicit object NonEmptyChainModifier extends Render[NonEmptyChain[VModifier]] {
    @inline def render(value: NonEmptyChain[VModifier]): VModifier = CompositeModifier(value.toChain.toList)
  }

  implicit object OptionModifier extends Render[Option[VModifier]] {
    @inline def render(value: Option[VModifier]): VModifier = value.getOrElse(VModifier.empty)
  }

  implicit object VModifierRender extends Render[VModifier] {
    @inline def render(value: VModifier): VModifier = value
  }

  implicit object StringRender extends Render[String] {
    @inline def render(value: String): VModifier = StringVNode(value)
  }

  implicit object IntRender extends Render[Int] {
    @inline def render(value: Int): VModifier = StringVNode(value.toString)
  }

  implicit object DoubleRender extends Render[Double] {
    @inline def render(value: Double): VModifier = StringVNode(value.toString)
  }

  implicit object LongRender extends Render[Long] {
    @inline def render(value: Long): VModifier = StringVNode(value.toString)
  }

  implicit object BooleanRender extends Render[Boolean] {
    @inline def render(value: Boolean): VModifier = StringVNode(value.toString)
  }

  implicit object SyncIORender extends Render[SyncIO[VModifier]] {
    @inline def render(future: SyncIO[VModifier]) = syncToModifier(future)
  }

  @inline implicit def SyncEffectRender[F[_]: RunSyncEffect]: Render[F[VModifier]] = new SyncEffectRenderClass[F]
  @inline private class SyncEffectRenderClass[F[_]: RunSyncEffect] extends Render[F[VModifier]] {
    @inline def render(effect: F[VModifier]) = syncToModifier(effect)
  }

  implicit object IORender extends Render[IO[VModifier]] {
    @inline def render(future: IO[VModifier]) = effectToModifier(future)
  }

  implicit object FutureRender extends Render[Future[VModifier]] {
    @inline def render(future: Future[VModifier]) = futureToModifier(future)
  }

  implicit object ObservableRender extends Render[Observable[VModifier]] {
    @inline def render(source: Observable[VModifier]) = sourceToModifier(source)
  }

  @inline implicit def SourceRender[F[_]: Source]: Render[F[VModifier]] = new SourceRenderClass[F]
  @inline private class SourceRenderClass[F[_]: Source] extends Render[F[VModifier]] {
    @inline def render(source: F[VModifier]) = sourceToModifier(source)
  }

  @inline implicit def AttributeBuilderRender: Render[AttributeBuilder[Boolean, VModifier]] = new AttributeBuilderRender
  @inline private final class AttributeBuilderRender extends Render[AttributeBuilder[Boolean, VModifier]] {
    @inline def render(builder: AttributeBuilder[Boolean, VModifier]) = builder := true
  }

  @inline implicit def EmitterBuilderRender: Render[EmitterBuilder[VModifier, VModifier]] = new EmitterBuilderRender
  @inline private final class EmitterBuilderRender extends Render[EmitterBuilder[VModifier, VModifier]] {
    @inline def render(builder: EmitterBuilder[VModifier, VModifier]) = builder.render
  }

  @inline implicit def ChildCommandSourceRender[F[_]: Source]: Render[F[ChildCommand]] = new ChildCommandRenderClass[F]
  @inline private class ChildCommandRenderClass[F[_]: Source] extends Render[F[ChildCommand]] {
    @inline def render(source: F[ChildCommand]): VModifier = childCommandToModifier(source)
  }

  @inline implicit def ChildCommandSeqSourceRender[F[_]: Source]: Render[F[Seq[ChildCommand]]] =
    new ChildCommandSeqRenderClass[F]
  @inline private class ChildCommandSeqRenderClass[F[_]: Source] extends Render[F[Seq[ChildCommand]]] {
    @inline def render(source: F[Seq[ChildCommand]]): VModifier = childCommandSeqToModifier(source)
  }
}

private object RenderOps {
  @noinline def iterableToModifierRender[T: Render](value: Iterable[T]): VModifier = CompositeModifier(
    value.map(VModifier(_)),
  )
  @noinline def optionToModifierRender[T: Render](value: Option[T]): VModifier =
    value.fold(VModifier.empty)(VModifier(_))
  @noinline def undefinedToModifierRender[T: Render](value: js.UndefOr[T]): VModifier =
    value.fold(VModifier.empty)(VModifier(_))
  @noinline def syncToModifierRender[F[_]: RunSyncEffect, T: Render](effect: F[T]): VModifier =
    VModifier.evalEither(RunSyncEffect[F].unsafeRun(effect))
  @noinline def syncToModifier[F[_]: RunSyncEffect](effect: F[VModifier]): VModifier =
    VModifier.evalEither(RunSyncEffect[F].unsafeRun(effect))
  @noinline def effectToModifier[F[_]: RunEffect](effect: F[VModifier]): VModifier = StreamModifier(
    Observable.fromEffect(effect).unsafeSubscribe(_),
  )
  @noinline def effectToModifierRender[F[_]: RunEffect, T: Render](effect: F[T]): VModifier =
    StreamModifier(obs => Observable.fromEffect(effect).unsafeSubscribe(obs.contramap(VModifier(_))))
  @noinline def resourceToModifier[F[_]: RunEffect: Sync](resource: Resource[F, VModifier]): VModifier = StreamModifier(
    Observable.fromResource(resource).unsafeSubscribe(_),
  )
  @noinline def resourceToModifierRender[F[_]: RunEffect: Sync, T: Render](resource: Resource[F, T]): VModifier =
    StreamModifier(obs => Observable.fromResource(resource).unsafeSubscribe(obs.contramap(VModifier(_))))
  @noinline def sourceToModifier[F[_]: Source](source: F[VModifier]): VModifier = StreamModifier(
    Source[F].unsafeSubscribe(source),
  )
  @noinline def sourceToModifierRender[F[_]: Source, T: Render](source: F[T]): VModifier =
    StreamModifier(sink => Source[F].unsafeSubscribe(source)(sink.contramap(VModifier(_))))
  @noinline def childCommandSeqToModifier[F[_]: Source](source: F[Seq[ChildCommand]]): VModifier =
    ChildCommandsModifier(Observable.lift(source))
  @noinline def childCommandToModifier[F[_]: Source](source: F[ChildCommand]): VModifier = ChildCommandsModifier(
    Observable.lift(source).map(Seq(_)),
  )
  @noinline def futureToModifierRender[T: Render](future: Future[T]): VModifier = future.value match {
    case Some(Success(value)) => VModifier(value)
    case _                    => StreamModifier(Observable.fromFuture(future).map(VModifier(_)).unsafeSubscribe(_))
  }
  @noinline def futureToModifier(future: Future[VModifier]): VModifier = future.value match {
    case Some(Success(value)) => value
    case _                    => StreamModifier(Observable.fromFuture(future).unsafeSubscribe(_))
  }
}
