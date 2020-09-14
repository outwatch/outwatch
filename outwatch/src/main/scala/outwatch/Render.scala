package outwatch

import colibri._
import colibri.effect.RunSyncEffect

import scala.scalajs.js
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

import cats.data.Kleisli
import cats.effect.Effect

trait Render[Env, -T] {
  def render(value: T): RModifier[Env]
}

object Render {
  @inline def apply[Env, T](implicit render: Render[Env, T]): Render[Env, T] = render

  implicit object JsArrayModifier extends Render[Any, js.Array[Modifier]] {
    @inline def render(value: js.Array[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def JsArrayModifierAs[Env, T : Render[Env, ?]]: Render[Env, js.Array[T]] = new JsArrayRenderAsClass[Env, T]
  @inline private final class JsArrayRenderAsClass[Env, T : Render[Env, ?]] extends Render[Env, js.Array[T]] {
    @inline def render(value: js.Array[T]) = iterableToModifierRender(value)
  }

  implicit object ArrayModifier extends Render[Any, Array[Modifier]] {
    @inline def render(value: Array[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def ArrayModifierAs[Env, T : Render[Env, ?]]: Render[Env, Array[T]] = new ArrayRenderAsClass[Env, T]
  @inline private final class ArrayRenderAsClass[Env, T : Render[Env, ?]] extends Render[Env, Array[T]] {
    @inline def render(value: Array[T]) = iterableToModifierRender(value)
  }

  implicit object SeqModifier extends Render[Any, Seq[Modifier]] {
    @inline def render(value: Seq[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def SeqModifierAs[Env, T : Render[Env, ?]]: Render[Env, Seq[T]] = new SeqRenderAsClass[Env, T]
  @inline private final class SeqRenderAsClass[Env, T : Render[Env, ?]] extends Render[Env, Seq[T]] {
    @inline def render(value: Seq[T]) = iterableToModifierRender(value)
  }

  implicit object OptionModifier extends Render[Any, Option[Modifier]] {
    @inline def render(value: Option[Modifier]): Modifier = value.getOrElse(Modifier.empty)
  }

  @inline implicit def OptionModifierAs[Env, T : Render[Env, ?]]: Render[Env, Option[T]] = new OptionRenderAsClass[Env, T]
  @inline private final class OptionRenderAsClass[Env, T : Render[Env, ?]] extends Render[Env, Option[T]] {
    @inline def render(value: Option[T]) = optionToModifierRender(value)
  }

  implicit object UndefinedModifier extends Render[Any, js.UndefOr[Modifier]] {
    @inline def render(value: js.UndefOr[Modifier]): Modifier = value.getOrElse(Modifier.empty)
  }

  @inline implicit def UndefinedModifierAs[Env, T : Render[Env, ?]]: Render[Env, js.UndefOr[T]] = new UndefinedRenderAsClass[Env, T]
  @inline private final class UndefinedRenderAsClass[Env, T : Render[Env, ?]] extends Render[Env, js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  implicit object ModifierRender extends Render[Any, Modifier] {
    @inline def render(value: Modifier): Modifier = value
  }

  @inline implicit def RModifierRender[Env]: Render[Env, RModifier[Env]] = new RModifierRender[Env]
  @inline private final class RModifierRender[Env] extends Render[Env, RModifier[Env]] {
    @inline def render(value: RModifier[Env]): RModifier[Env] = value
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

  @inline implicit def KleisliRenderAs[F[_], Env, T](implicit r: Render[Any, F[T]]): Render[Env, Kleisli[F, Env, T]] = new KleisliRenderAsClass[F, Env, T]
  @inline private final class KleisliRenderAsClass[F[_], Env, T](implicit r: Render[Any, F[T]]) extends Render[Env, Kleisli[F, Env, T]] {
    @inline def render(kleisli: Kleisli[F, Env, T]) = REnvModifier[Env](env => r.render(kleisli.run(env)))
  }

  @inline implicit def SyncEffectRender[F[_] : RunSyncEffect, Env]: Render[Env, F[RModifier[Env]]] = new SyncEffectRenderClass[F, Env]
  @inline private final class SyncEffectRenderClass[F[_] : RunSyncEffect, Env] extends Render[Env, F[RModifier[Env]]] {
    @inline def render(effect: F[RModifier[Env]]) = syncToModifier(effect)
  }

  @inline implicit def SyncEffectRenderAs[F[_] : RunSyncEffect, Env, T : Render[Env, ?]]: Render[Env, F[T]] = new SyncEffectRenderAsClass[F, Env, T]
  @inline private final class SyncEffectRenderAsClass[F[_] : RunSyncEffect, Env, T : Render[Env, ?]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  implicit def EffectRender[F[_] : Effect, Env]: Render[Env, F[RModifier[Env]]] = new EffectRenderClass[F, Env]
  @inline private final class EffectRenderClass[F[_] : Effect, Env] extends Render[Env, F[RModifier[Env]]] {
    def render(effect: F[RModifier[Env]]) = asyncToModifier(effect)
  }

  @inline implicit def EffectRenderAs[F[_] : Effect, Env, T : Render[Env, ?]]: Render[Env, F[T]] = new EffectRenderAsClass[F, Env, T]
  @inline private final class EffectRenderAsClass[F[_] : Effect, Env, T : Render[Env, ?]] extends Render[Env, F[T]] {
    @inline def render(effect: F[T]) = asyncToModifierRender(effect)
  }

  implicit def FutureRender[Env](implicit ec: ExecutionContext): Render[Env, Future[RModifier[Env]]] = new FutureRenderClass[Env]
  @inline private final class FutureRenderClass[Env](implicit ec: ExecutionContext) extends Render[Env, Future[RModifier[Env]]] {
    @inline def render(future: Future[RModifier[Env]]) = futureToModifier(future)
  }

  @inline implicit def FutureRenderAs[Env, T : Render[Env, ?]](implicit ec: ExecutionContext): Render[Env, Future[T]] = new FutureRenderAsClass[Env, T]
  @inline private final class FutureRenderAsClass[Env, T: Render[Env, ?]](implicit ec: ExecutionContext) extends Render[Env, Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRender[F[_] : Source, Env]: Render[Env, F[RModifier[Env]]] = new SourceRenderClass[F, Env]
  @inline private final class SourceRenderClass[F[_] : Source, Env] extends Render[Env, F[RModifier[Env]]] {
    @inline def render(source: F[RModifier[Env]]) = sourceToModifier(source)
  }

  @inline implicit def SourceRenderAs[F[_] : Source, Env, T : Render[Env, ?]]: Render[Env, F[T]] = new SourceRenderAsClass[F, Env, T]
  @inline private final class SourceRenderAsClass[F[_]: Source, Env, T: Render[Env, ?]] extends Render[Env, F[T]] {
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

  @noinline private def iterableToModifierRender[Env, T: Render[Env, ?]](value: Iterable[T]): RModifier[Env] = CompositeModifier(value.map(RModifier(_)))
  @noinline private def optionToModifierRender[Env, T: Render[Env, ?]](value: Option[T]): RModifier[Env] = value.fold[RModifier[Env]](RModifier.empty)(RModifier(_))
  @noinline private def undefinedToModifierRender[Env, T: Render[Env, ?]](value: js.UndefOr[T]): RModifier[Env] = value.fold[RModifier[Env]](RModifier.empty)(RModifier(_))
  @noinline private def syncToModifierRender[F[_] : RunSyncEffect, Env, T: Render[Env, ?]](effect: F[T]): RModifier[Env] = SyncEffectModifier(() => RModifier(RunSyncEffect[F].unsafeRun(effect)))
  @noinline private def syncToModifier[F[_] : RunSyncEffect, Env](effect: F[RModifier[Env]]): RModifier[Env] = SyncEffectModifier(() => RunSyncEffect[F].unsafeRun(effect))
  @noinline private def asyncToModifier[F[_] : Effect, Env](effect: F[RModifier[Env]]): RModifier[Env] = StreamModifier(Observable.fromAsync(effect).subscribe(_))
  @noinline private def asyncToModifierRender[F[_] : Effect, Env, T: Render[Env, ?]](effect: F[T]): RModifier[Env] = StreamModifier(Observable.fromAsync(effect).map(RModifier(_)).subscribe(_))
  @noinline private def sourceToModifier[F[_] : Source, Env](source: F[RModifier[Env]]): RModifier[Env] = StreamModifier(Source[F].subscribe(source))
  @noinline private def sourceToModifierRender[F[_] : Source, Env, T: Render[Env, ?]](source: F[T]): RModifier[Env] = StreamModifier(sink => Source[F].subscribe(source)(Observer.contramap[Observer, RModifier[Env], T](sink)(RModifier(_))))
  @noinline private def childCommandSeqToModifier[F[_] : Source, Env](source: F[Seq[ChildCommand]]): RModifier[Env] = ChildCommand.stream(source)
  @noinline private def childCommandToModifier[F[_] : Source, Env](source: F[ChildCommand]): RModifier[Env] = ChildCommand.stream(Observable.map(source)(Seq(_)))
  @noinline private def futureToModifierRender[Env, T: Render[Env, ?]](future: Future[T])(implicit ec: ExecutionContext): RModifier[Env] = future.value match {
    case Some(Success(value)) => Modifier(value)
    case _ => StreamModifier(Observable.fromFuture(future).map(RModifier(_)).subscribe(_))
  }
  @noinline private def futureToModifier[Env](future: Future[RModifier[Env]])(implicit ec: ExecutionContext): RModifier[Env] = future.value match {
    case Some(Success(value)) => value
    case _ => RStreamModifier(Observable.fromFuture(future).subscribe(_))
  }
}
