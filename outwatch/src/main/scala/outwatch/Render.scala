package outwatch

import colibri._
import colibri.effect.RunSyncEffect

import scala.scalajs.js
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

import cats.effect.Effect

trait Render[-T] {
  def render(value: T): Modifier
}

object Render {
  @inline def apply[T](implicit render: Render[T]): Render[T] = render

  implicit object JsArrayModifier extends Render[js.Array[Modifier]] {
    @inline def render(value: js.Array[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def JsArrayModifierAs[T : Render]: Render[js.Array[T]] = new JsArrayRenderAsClass[T]
  @inline private class JsArrayRenderAsClass[T : Render] extends Render[js.Array[T]] {
    @inline def render(value: js.Array[T]) = iterableToModifierRender(value)
  }

  implicit object ArrayModifier extends Render[Array[Modifier]] {
    @inline def render(value: Array[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def ArrayModifierAs[T : Render]: Render[Array[T]] = new ArrayRenderAsClass[T]
  @inline private class ArrayRenderAsClass[T : Render] extends Render[Array[T]] {
    @inline def render(value: Array[T]) = iterableToModifierRender(value)
  }

  implicit object SeqModifier extends Render[Seq[Modifier]] {
    @inline def render(value: Seq[Modifier]): Modifier = CompositeModifier(value)
  }

  @inline implicit def SeqModifierAs[T : Render]: Render[Seq[T]] = new SeqRenderAsClass[T]
  @inline private class SeqRenderAsClass[T : Render] extends Render[Seq[T]] {
    @inline def render(value: Seq[T]) = iterableToModifierRender(value)
  }

  implicit object OptionModifier extends Render[Option[Modifier]] {
    @inline def render(value: Option[Modifier]): Modifier = value.getOrElse(Modifier.empty)
  }

  @inline implicit def OptionModifierAs[T : Render]: Render[Option[T]] = new OptionRenderAsClass[T]
  @inline private class OptionRenderAsClass[T : Render] extends Render[Option[T]] {
    @inline def render(value: Option[T]) = optionToModifierRender(value)
  }

  implicit object UndefinedModifier extends Render[js.UndefOr[Modifier]] {
    @inline def render(value: js.UndefOr[Modifier]): Modifier = value.getOrElse(Modifier.empty)
  }

  @inline implicit def UndefinedModifierAs[T : Render]: Render[js.UndefOr[T]] = new UndefinedRenderAsClass[T]
  @inline private class UndefinedRenderAsClass[T : Render] extends Render[js.UndefOr[T]] {
    @inline def render(value: js.UndefOr[T]) = undefinedToModifierRender(value)
  }

  implicit object ModifierRender extends Render[Modifier] {
    @inline def render(value: Modifier): Modifier = value
  }

  implicit object StringRender extends Render[String] {
    @inline def render(value: String): Modifier = StringVNode(value)
  }

  implicit object IntRender extends Render[Int] {
    @inline def render(value: Int): Modifier = StringVNode(value.toString)
  }

  implicit object DoubleRender extends Render[Double] {
    @inline def render(value: Double): Modifier = StringVNode(value.toString)
  }

  implicit object LongRender extends Render[Long] {
    @inline def render(value: Long): Modifier = StringVNode(value.toString)
  }

  implicit object BooleanRender extends Render[Boolean] {
    @inline def render(value: Boolean): Modifier = StringVNode(value.toString)
  }

  @inline implicit def SyncEffectRender[F[_] : RunSyncEffect]: Render[F[Modifier]] = new SyncEffectRenderClass[F]
  @inline private class SyncEffectRenderClass[F[_] : RunSyncEffect] extends Render[F[Modifier]] {
    @inline def render(effect: F[Modifier]) = syncToModifier(effect)
  }

  @inline implicit def SyncEffectRenderAs[F[_] : RunSyncEffect, T : Render]: Render[F[T]] = new SyncEffectRenderAsClass[F, T]
  @inline private class SyncEffectRenderAsClass[F[_] : RunSyncEffect, T : Render] extends Render[F[T]] {
    @inline def render(effect: F[T]) = syncToModifierRender(effect)
  }

  implicit def EffectRender[F[_] : Effect]: Render[F[Modifier]] = new EffectRenderClass[F]
  @inline private class EffectRenderClass[F[_] : Effect] extends Render[F[Modifier]] {
    def render(effect: F[Modifier]) = asyncToModifier(effect)
  }

  @inline implicit def EffectRenderAs[F[_] : Effect, T : Render]: Render[F[T]] = new EffectRenderAsClass[F, T]
  @inline private class EffectRenderAsClass[F[_] : Effect, T : Render] extends Render[F[T]] {
    @inline def render(effect: F[T]) = asyncToModifierRender(effect)
  }

  implicit def FutureRender(implicit ec: ExecutionContext): Render[Future[Modifier]] = new FutureRenderClass
  @inline private class FutureRenderClass(implicit ec: ExecutionContext) extends Render[Future[Modifier]] {
    @inline def render(future: Future[Modifier]) = futureToModifier(future)
  }

  @inline implicit def FutureRenderAs[T : Render](implicit ec: ExecutionContext): Render[Future[T]] = new FutureRenderAsClass[T]
  @inline private class FutureRenderAsClass[T: Render](implicit ec: ExecutionContext) extends Render[Future[T]] {
    @inline def render(future: Future[T]) = futureToModifierRender(future)
  }

  @inline implicit def SourceRender[F[_] : Source]: Render[F[Modifier]] = new SourceRenderClass[F]
  @inline private class SourceRenderClass[F[_] : Source] extends Render[F[Modifier]] {
    @inline def render(source: F[Modifier]) = sourceToModifier(source)
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

  @noinline private def iterableToModifierRender[T: Render](value: Iterable[T]): Modifier = CompositeModifier(value.map(Modifier(_)))
  @noinline private def optionToModifierRender[T: Render](value: Option[T]): Modifier = value.fold(Modifier.empty)(Modifier(_))
  @noinline private def undefinedToModifierRender[T: Render](value: js.UndefOr[T]): Modifier = value.fold(Modifier.empty)(Modifier(_))
  @noinline private def syncToModifierRender[F[_] : RunSyncEffect, T: Render](effect: F[T]): Modifier = SyncEffectModifier(() => Modifier(RunSyncEffect[F].unsafeRun(effect)))
  @noinline private def syncToModifier[F[_] : RunSyncEffect](effect: F[Modifier]): Modifier = SyncEffectModifier(() => RunSyncEffect[F].unsafeRun(effect))
  @noinline private def asyncToModifier[F[_] : Effect](effect: F[Modifier]): Modifier = StreamModifier(Observable.fromAsync(effect).subscribe(_))
  @noinline private def asyncToModifierRender[F[_] : Effect, T: Render](effect: F[T]): Modifier = StreamModifier(Observable.fromAsync(effect).map(Modifier(_)).subscribe(_))
  @noinline private def sourceToModifier[F[_] : Source](source: F[Modifier]): Modifier = StreamModifier(Source[F].subscribe(source))
  @noinline private def sourceToModifierRender[F[_] : Source, T: Render](source: F[T]): Modifier = StreamModifier(sink => Source[F].subscribe(source)(Observer.contramap[Observer, Modifier, T](sink)(Modifier(_))))
  @noinline private def childCommandSeqToModifier[F[_] : Source](source: F[Seq[ChildCommand]]): Modifier = ChildCommand.stream(source)
  @noinline private def childCommandToModifier[F[_] : Source](source: F[ChildCommand]): Modifier = ChildCommand.stream(Observable.map(source)(Seq(_)))
  @noinline private def futureToModifierRender[T: Render](future: Future[T])(implicit ec: ExecutionContext): Modifier = future.value match {
    case Some(Success(value)) => Modifier(value)
    case _ => StreamModifier(Observable.fromFuture(future).map(Modifier(_)).subscribe(_))
  }
  @noinline private def futureToModifier(future: Future[Modifier])(implicit ec: ExecutionContext): Modifier = future.value match {
    case Some(Success(value)) => value
    case _ => StreamModifier(Observable.fromFuture(future).subscribe(_))
  }
}
