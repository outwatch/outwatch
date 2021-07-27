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
  def render(value: T): ModifierM[Env]
}

object Render {
  @inline def apply[Env, T](implicit render: Render[Env, T]): Render[Env, T] = render

  implicit object JsArrayModifier extends Render[Any, js.Array[Modifier]] {
    @inline def render(value: js.Array[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def JsArrayModifierAs[Env, T : Render[Env, *]]: Render[Env, js.Array[T]] = new JsArrayRenderAsClass[Env, T]
  @inline private final class JsArrayRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, js.Array[T]] {
    @inline def render(value: js.Array[T]) = iterableToModifierRender(value)
  }

  implicit object ArrayModifier extends Render[Any, Array[Modifier]] {
    @inline def render(value: Array[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def ArrayModifierAs[Env, T : Render[Env, *]]: Render[Env, Array[T]] = new ArrayRenderAsClass[Env, T]
  @inline private final class ArrayRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, Array[T]] {
    @inline def render(value: Array[T]) = iterableToModifierRender(value)
  }

  implicit object SeqModifier extends Render[Any, Seq[Modifier]] {
    @inline def render(value: Seq[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def SeqModifierAs[Env, T : Render[Env, *]]: Render[Env, Seq[T]] = new SeqRenderAsClass[Env, T]
  @inline private final class SeqRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, Seq[T]] {
    @inline def render(value: Seq[T]) = iterableToModifierRender(value)
  }

  implicit object OptionModifier extends Render[Any, Option[Modifier]] {
    @inline def render(value: Option[Modifier]): Modifier = value.getOrElse(Modifier.empty)
  }

  @inline implicit def OptionModifierAs[Env, T : Render[Env, *]]: Render[Env, Option[T]] = new OptionRenderAsClass[Env, T]
  @inline private final class OptionRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, Option[T]] {
    @inline def render(value: Option[T]) = optionToModifierRender(value)
  }

  @inline implicit def EitherModifierAs[LEnv, REnv, L : Render[LEnv, *], R : Render[REnv, *]]: Render[LEnv with REnv, Either[L,R]] = new EitherRenderAsClass[LEnv, REnv, L, R]
  @inline private final class EitherRenderAsClass[LEnv, REnv, L : Render[LEnv, *], R : Render[REnv, *]] extends Render[LEnv with REnv, Either[L, R]] {
    @inline def render(value: Either[L, R]) = eitherToModifierRender(value)
  }

  implicit object UndefinedModifier extends Render[Any, js.UndefOr[Modifier]] {
    @inline def render(value: js.UndefOr[Modifier]): Modifier = value.getOrElse(Modifier.empty)
  }

  @inline implicit def UndefinedModifierAs[Env, T : Render[Env, *]]: Render[Env, js.UndefOr[T]] = new UndefinedRenderAsClass[Env, T]
  @inline private final class UndefinedRenderAsClass[Env, T : Render[Env, *]] extends Render[Env, js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  implicit object ModifierRender extends Render[Any, Modifier] {
    @inline def render(value: Modifier): Modifier = value
  }

  @inline implicit def ModifierMRender[Env]: Render[Env, ModifierM[Env]] = new ModifierMRender[Env]
  @inline private final class ModifierMRender[Env] extends Render[Env, ModifierM[Env]] {
    @inline def render(value: ModifierM[Env]): ModifierM[Env] = value
  }

  implicit object StringRender extends Render[Any, String] {
    @inline def render(value: String): Modifier = StringVNode(value)
  }

  implicit object IntRender extends Render[Any, Int] {
    @inline def render(value: Int): Modifier = StringVNode(value.toString)
  }

  implicit object DoubleRender extends Render[Any, Double] {
    @inline def render(value: Double): Modifier = StringVNode(value.toString)
  }

  implicit object LongRender extends Render[Any, Long] {
    @inline def render(value: Long): Modifier = StringVNode(value.toString)
  }

  implicit object BooleanRender extends Render[Any, Boolean] {
    @inline def render(value: Boolean): Modifier = StringVNode(value.toString)
  }

  @inline implicit def AttributeBuilderRender[A <: Modifier]: Render[Any, AttributeBuilder[Boolean, A]] = new AttributeBuilderRender[A]
  @inline private final class AttributeBuilderRender[A <: Modifier] extends Render[Any, AttributeBuilder[Boolean, A]] {
    @inline def render(builder: AttributeBuilder[Boolean, A]) = builder := true
  }

  @inline implicit def KleisliRenderAs[F[_], Env, T](implicit r: Render[Any, F[T]]): Render[Env, Kleisli[F, Env, T]] = new KleisliRenderAsClass[F, Env, T]
  @inline private final class KleisliRenderAsClass[F[_], Env, T](implicit r: Render[Any, F[T]]) extends Render[Env, Kleisli[F, Env, T]] {
    @inline def render(kleisli: Kleisli[F, Env, T]) = ModifierM.access[Env](env => r.render(kleisli.run(env)))
  }

  @inline implicit def SyncEffectRender[F[_] : RunSyncEffect, Env]: Render[Env, F[ModifierM[Env]]] = new SyncEffectRenderClass[F, Env]
  @inline private final class SyncEffectRenderClass[F[_] : RunSyncEffect, Env] extends Render[Env, F[ModifierM[Env]]] {
    @inline def render(effect: F[ModifierM[Env]]) = syncToModifier(effect)
  }

  @inline implicit def SyncEffectRenderAs[F[_] : RunSyncEffect, Env, T : Render[Env, *]]: Render[Env, F[T]] = new SyncEffectRenderAsClass[F, Env, T]
  @inline private final class SyncEffectRenderAsClass[F[_] : RunSyncEffect, Env, T : Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  implicit def EffectRender[F[_] : Effect, Env]: Render[Env, F[ModifierM[Env]]] = new EffectRenderClass[F, Env]
  @inline private final class EffectRenderClass[F[_] : Effect, Env] extends Render[Env, F[ModifierM[Env]]] {
    def render(effect: F[ModifierM[Env]]) = asyncToModifier(effect)
  }

  @inline implicit def EffectRenderAs[F[_] : Effect, Env, T : Render[Env, *]]: Render[Env, F[T]] = new EffectRenderAsClass[F, Env, T]
  @inline private final class EffectRenderAsClass[F[_] : Effect, Env, T : Render[Env, *]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = asyncToModifierRender(effect)
  }

  implicit def FutureRender[Env](implicit ec: ExecutionContext): Render[Env, Future[ModifierM[Env]]] = new FutureRenderClass[Env]
  @inline private final class FutureRenderClass[Env](implicit ec: ExecutionContext) extends Render[Env, Future[ModifierM[Env]]] {
    @inline def render(future: Future[ModifierM[Env]]) = futureToModifier(future)
  }

  @inline implicit def FutureRenderAs[Env, T : Render[Env, *]](implicit ec: ExecutionContext): Render[Env, Future[T]] = new FutureRenderAsClass[Env, T]
  @inline private final class FutureRenderAsClass[Env, T: Render[Env, *]](implicit ec: ExecutionContext) extends Render[Env, Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRender[F[_] : Source, Env]: Render[Env, F[ModifierM[Env]]] = new SourceRenderClass[F, Env]
  @inline private final class SourceRenderClass[F[_] : Source, Env] extends Render[Env, F[ModifierM[Env]]] {
    @inline def render(source: F[ModifierM[Env]]) = sourceToModifier(source)
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

  private def iterableToModifierRender[Env, T: Render[Env, *]](value: Iterable[T]): ModifierM[Env] = CompositeModifier(value.map(ModifierM(_)))
  private def optionToModifierRender[Env, T: Render[Env, *]](value: Option[T]): ModifierM[Env] = value.fold[ModifierM[Env]](ModifierM.empty)(ModifierM(_))
  private def eitherToModifierRender[LEnv, REnv, L : Render[LEnv, *], R : Render[REnv, *]](value: Either[L,R]): ModifierM[LEnv with REnv] = value.fold(ModifierM(_), ModifierM(_))
  private def undefinedToModifierRender[Env, T: Render[Env, *]](value: js.UndefOr[T]): ModifierM[Env] = value.fold[ModifierM[Env]](ModifierM.empty)(ModifierM(_))
  private def syncToModifierRender[F[_] : RunSyncEffect, Env, T: Render[Env, *]](effect: F[T]): ModifierM[Env] = ModifierM.delay(ModifierM(RunSyncEffect[F].unsafeRun(effect)))
  private def syncToModifier[F[_] : RunSyncEffect, Env](effect: F[ModifierM[Env]]): ModifierM[Env] = ModifierM.delay(RunSyncEffect[F].unsafeRun(effect))
  private def asyncToModifier[F[_] : Effect, Env](effect: F[ModifierM[Env]]): ModifierM[Env] = StreamModifier(Observable.fromAsync(effect).subscribe(_))
  private def asyncToModifierRender[F[_] : Effect, Env, T: Render[Env, *]](effect: F[T]): ModifierM[Env] = StreamModifier(Observable.fromAsync(effect).map(ModifierM(_)).subscribe(_))
  private def sourceToModifier[F[_] : Source, Env](source: F[ModifierM[Env]]): ModifierM[Env] = StreamModifier(Source[F].subscribe(source))
  private def sourceToModifierRender[F[_] : Source, Env, T: Render[Env, *]](source: F[T]): ModifierM[Env] = StreamModifier(sink => Source[F].subscribe(source)(Observer.contramap[Observer, ModifierM[Env], T](sink)(ModifierM(_))))
  private def childCommandSeqToModifier[F[_] : Source, Env](source: F[Seq[ChildCommand]]): ModifierM[Env] = ChildCommand.stream(source)
  private def childCommandToModifier[F[_] : Source, Env](source: F[ChildCommand]): ModifierM[Env] = ChildCommand.stream(Observable.map(source)(Seq(_)))
  private def futureToModifierRender[Env, T: Render[Env, *]](future: Future[T])(implicit ec: ExecutionContext): ModifierM[Env] = future.value match {
    case Some(Success(value)) => ModifierM(value)
    case _ => StreamModifier(Observable.fromFuture(future).map(ModifierM(_)).subscribe(_))
  }
  private def futureToModifier[Env](future: Future[ModifierM[Env]])(implicit ec: ExecutionContext): ModifierM[Env] = future.value match {
    case Some(Success(value)) => value
    case _ => StreamModifier(Observable.fromFuture(future).subscribe(_))
  }
}
