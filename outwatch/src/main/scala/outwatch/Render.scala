package outwatch

import colibri._
import colibri.effect.RunSyncEffect

import outwatch.helpers.AttributeBuilder

import scala.scalajs.js
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

import cats.data.Kleisli
import cats.effect.Effect

trait Render[-Env, -T] {
  def render(value: T): VModifierM[Env]
}

object Render {
  @inline def apply[Env, T](implicit render: Render[Env, T]): Render[Env, T] = render

  implicit object JsArrayModifier extends Render[Any, js.Array[VModifier]] {
    @inline def render(value: js.Array[VModifier]): VModifier = CompositeModifier(value)
  }

  @inline implicit def JsArrayModifierAs[Env, T : Render[Env, *]]: Render[Env, js.Array[T]] = new JsArrayRenderAsClass[Env, T]
  @inline private final class JsArrayRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, js.Array[T]] {
    @inline def render(value: js.Array[T]) = iterableToModifierRender(value)
  }

  implicit object ArrayModifier extends Render[Any, Array[VModifier]] {
    @inline def render(value: Array[VModifier]): VModifier = CompositeModifier(value)
  }

  @inline implicit def ArrayModifierAs[Env, T : Render[Env, *]]: Render[Env, Array[T]] = new ArrayRenderAsClass[Env, T]
  @inline private final class ArrayRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, Array[T]] {
    @inline def render(value: Array[T]) = iterableToModifierRender(value)
  }

  implicit object SeqModifier extends Render[Any, Seq[VModifier]] {
    @inline def render(value: Seq[VModifier]): VModifier = CompositeModifier(value)
  }

  @inline implicit def SeqModifierAs[Env, T : Render[Env, *]]: Render[Env, Seq[T]] = new SeqRenderAsClass[Env, T]
  @inline private final class SeqRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, Seq[T]] {
    @inline def render(value: Seq[T]) = iterableToModifierRender(value)
  }

  implicit object OptionModifier extends Render[Any, Option[VModifier]] {
    @inline def render(value: Option[VModifier]): VModifier = value.getOrElse(VModifier.empty)
  }

  @inline implicit def OptionModifierAs[Env, T : Render[Env, *]]: Render[Env, Option[T]] = new OptionRenderAsClass[Env, T]
  @inline private final class OptionRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, Option[T]] {
    @inline def render(value: Option[T]) = optionToModifierRender(value)
  }

  @inline implicit def EitherModifierAs[LEnv, REnv, L : Render[LEnv, *], R : Render[REnv, *]]: Render[LEnv with REnv, Either[L,R]] = new EitherRenderAsClass[LEnv, REnv, L, R]
  @inline private final class EitherRenderAsClass[LEnv, REnv, L : Render[LEnv, *], R : Render[REnv, *]] extends Render[LEnv with REnv, Either[L, R]] {
    @inline def render(value: Either[L, R]) = eitherToModifierRender(value)
  }

  implicit object UndefinedModifier extends Render[Any, js.UndefOr[VModifier]] {
    @inline def render(value: js.UndefOr[VModifier]): VModifier = value.getOrElse(VModifier.empty)
  }

  @inline implicit def UndefinedModifierAs[Env, T : Render[Env, *]]: Render[Env, js.UndefOr[T]] = new UndefinedRenderAsClass[Env, T]
  @inline private final class UndefinedRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  implicit object VModifierRender extends Render[Any, VModifier] {
    @inline def render(value: VModifier): VModifier = value
  }

  @inline implicit def VModifierMRender[Env]: Render[Env, VModifierM[Env]] = new VModifierMRender[Env]
  @inline private final class VModifierMRender[Env] extends Render[Env, VModifierM[Env]] {
    @inline def render(value: VModifierM[Env]): VModifierM[Env] = value
  }

  implicit object StringRender extends Render[Any, String] {
    @inline def render(value: String): VModifier = StringVNode(value)
  }

  implicit object IntRender extends Render[Any, Int] {
    @inline def render(value: Int): VModifier = StringVNode(value.toString)
  }

  implicit object DoubleRender extends Render[Any, Double] {
    @inline def render(value: Double): VModifier = StringVNode(value.toString)
  }

  implicit object LongRender extends Render[Any, Long] {
    @inline def render(value: Long): VModifier = StringVNode(value.toString)
  }

  implicit object BooleanRender extends Render[Any, Boolean] {
    @inline def render(value: Boolean): VModifier = StringVNode(value.toString)
  }

  @inline implicit def AttributeBuilderRender[A <: VModifier]: Render[Any, AttributeBuilder[Boolean, A]] = new AttributeBuilderRender[A]
  @inline private final class AttributeBuilderRender[A <: VModifier] extends Render[Any, AttributeBuilder[Boolean, A]] {
    @inline def render(builder: AttributeBuilder[Boolean, A]) = builder := true
  }

  @inline implicit def KleisliRenderAs[F[_], Env, T](implicit r: Render[Any, F[T]]): Render[Env, Kleisli[F, Env, T]] = new KleisliRenderAsClass[F, Env, T]
  @inline private final class KleisliRenderAsClass[F[_], Env, T](implicit r: Render[Any, F[T]]) extends Render[Env, Kleisli[F, Env, T]] {
    @inline def render(kleisli: Kleisli[F, Env, T]) = VModifierM.access[Env](env => r.render(kleisli.run(env)))
  }

  @inline implicit def SyncEffectRender[F[_] : RunSyncEffect, Env]: Render[Env, F[VModifierM[Env]]] = new SyncEffectRenderClass[F, Env]
  @inline private final class SyncEffectRenderClass[F[_] : RunSyncEffect, Env] extends Render[Env, F[VModifierM[Env]]] {
    @inline def render(effect: F[VModifierM[Env]]) = syncToModifier(effect)
  }

  @inline implicit def SyncEffectRenderAs[F[_] : RunSyncEffect, Env, T : Render[Env, *]]: Render[Env, F[T]] = new SyncEffectRenderAsClass[F, Env, T]
  @inline private final class SyncEffectRenderAsClass[F[_] : RunSyncEffect, Env, T : Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  implicit def EffectRender[F[_] : Effect, Env]: Render[Env, F[VModifierM[Env]]] = new EffectRenderClass[F, Env]
  @inline private final class EffectRenderClass[F[_] : Effect, Env] extends Render[Env, F[VModifierM[Env]]] {
    def render(effect: F[VModifierM[Env]]) = asyncToModifier(effect)
  }

  @inline implicit def EffectRenderAs[F[_] : Effect, Env, T : Render[Env, *]]: Render[Env, F[T]] = new EffectRenderAsClass[F, Env, T]
  @inline private final class EffectRenderAsClass[F[_] : Effect, Env, T : Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = asyncToModifierRender(effect)
  }

  implicit def FutureRender[Env](implicit ec: ExecutionContext): Render[Env, Future[VModifierM[Env]]] = new FutureRenderClass[Env]
  @inline private final class FutureRenderClass[Env](implicit ec: ExecutionContext) extends Render[Env, Future[VModifierM[Env]]] {
    @inline def render(future: Future[VModifierM[Env]]) = futureToModifier(future)
  }

  @inline implicit def FutureRenderAs[Env, T : Render[Env, *]](implicit ec: ExecutionContext): Render[Env, Future[T]] = new FutureRenderAsClass[Env, T]
  @inline private final class FutureRenderAsClass[Env, T: Render[Env, *]](implicit ec: ExecutionContext) extends Render[Env, Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRender[F[_] : Source, Env]: Render[Env, F[VModifierM[Env]]] = new SourceRenderClass[F, Env]
  @inline private final class SourceRenderClass[F[_] : Source, Env] extends Render[Env, F[VModifierM[Env]]] {
    @inline def render(source: F[VModifierM[Env]]) = sourceToModifier(source)
  }

  @inline implicit def SourceRenderAs[F[_] : Source, Env, T : Render[Env, *]]: Render[Env, F[T]] = new SourceRenderAsClass[F, Env, T]
  @inline private final class SourceRenderAsClass[F[_]: Source, Env, T: Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(source: F[T]) = sourceToModifierRender(source)
  }

  @inline implicit def ChildCommandSourceRender[F[_] : Source]: Render[Any, F[ChildCommand]] = new ChildCommandRenderClass[F]
  @inline private final class ChildCommandRenderClass[F[_] : Source] extends Render[Any, F[ChildCommand]] {
    @inline def render(source: F[ChildCommand]) = childCommandToModifier(source)
  }

  @inline implicit def ChildCommandSeqSourceRender[F[_] : Source]: Render[Any, F[Seq[ChildCommand]]] = new ChildCommandSeqRenderClass[F]
  @inline private final class ChildCommandSeqRenderClass[F[_] : Source] extends Render[Any, F[Seq[ChildCommand]]] {
    @inline def render(source: F[Seq[ChildCommand]]) = childCommandSeqToModifier(source)
  }

  private def iterableToModifierRender[Env, T: Render[Env, *]](value: Iterable[T]): VModifierM[Env] = CompositeModifier(value.map(VModifierM(_)))
  private def optionToModifierRender[Env, T: Render[Env, *]](value: Option[T]): VModifierM[Env] = value.fold[VModifierM[Env]](VModifierM.empty)(VModifierM(_))
  private def eitherToModifierRender[LEnv, REnv, L : Render[LEnv, *], R : Render[REnv, *]](value: Either[L,R]): VModifierM[LEnv with REnv] = value.fold(VModifierM(_), VModifierM(_))
  private def undefinedToModifierRender[Env, T: Render[Env, *]](value: js.UndefOr[T]): VModifierM[Env] = value.fold[VModifierM[Env]](VModifierM.empty)(VModifierM(_))
  private def syncToModifierRender[F[_] : RunSyncEffect, Env, T: Render[Env, *]](effect: F[T]): VModifierM[Env] = VModifierM.delay(VModifierM(RunSyncEffect[F].unsafeRun(effect)))
  private def syncToModifier[F[_] : RunSyncEffect, Env](effect: F[VModifierM[Env]]): VModifierM[Env] = VModifierM.delay(RunSyncEffect[F].unsafeRun(effect))
  private def asyncToModifier[F[_] : Effect, Env](effect: F[VModifierM[Env]]): VModifierM[Env] = StreamModifier(Observable.fromAsync(effect).subscribe(_))
  private def asyncToModifierRender[F[_] : Effect, Env, T: Render[Env, *]](effect: F[T]): VModifierM[Env] = StreamModifier(Observable.fromAsync(effect).map(VModifierM(_)).subscribe(_))
  private def sourceToModifier[F[_] : Source, Env](source: F[VModifierM[Env]]): VModifierM[Env] = StreamModifier(Source[F].subscribe(source))
  private def sourceToModifierRender[F[_] : Source, Env, T: Render[Env, *]](source: F[T]): VModifierM[Env] = StreamModifier(sink => Source[F].subscribe(source)(Observer.contramap[Observer, VModifierM[Env], T](sink)(VModifierM(_))))
  private def childCommandSeqToModifier[F[_] : Source, Env](source: F[Seq[ChildCommand]]): VModifierM[Env] = ChildCommand.stream(source)
  private def childCommandToModifier[F[_] : Source, Env](source: F[ChildCommand]): VModifierM[Env] = ChildCommand.stream(Observable.map(source)(Seq(_)))
  private def futureToModifierRender[Env, T: Render[Env, *]](future: Future[T])(implicit ec: ExecutionContext): VModifierM[Env] = future.value match {
    case Some(Success(value)) => VModifierM(value)
    case _ => StreamModifier(Observable.fromFuture(future).map(VModifierM(_)).subscribe(_))
  }
  private def futureToModifier[Env](future: Future[VModifierM[Env]])(implicit ec: ExecutionContext): VModifierM[Env] = future.value match {
    case Some(Success(value)) => value
    case _ => StreamModifier(Observable.fromFuture(future).subscribe(_))
  }
}
