package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.{DestroyHook, Emitter, Hook, InsertHook, UpdateHook}
import rxscalajs.Observable


trait EmitterBuilder[E <: Event, O] extends Any {

  private[outwatch] def transform[T](tr: Observable[O] => Observable[T]): TransformingEmitterBuilder[E, T]

  def apply[T](value: T): TransformingEmitterBuilder[E, T] = map(_ => value)

  def apply[T](latest: Observable[T]): TransformingEmitterBuilder[E, T] = transform(_.withLatestFromWith(latest)((_, u) => u))

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): TransformingEmitterBuilder[E, T] = map(f)

  def map[T](f: O => T): TransformingEmitterBuilder[E, T] = transform(_.map(f))

  def filter(predicate: O => Boolean): TransformingEmitterBuilder[E, O] = transform(_.filter(predicate))

  def -->(sink: Sink[_ >: O]): IO[Emitter]
}


object EmitterBuilder {
  def apply[E <: Event](eventType: String) = new SimpleEmitterBuilder[E](eventType)
}


final case class TransformingEmitterBuilder[E <: Event, O] private[helpers] (
  eventType: String,
  transformer: Observable[E] => Observable[O]
) extends EmitterBuilder[E, O] {

  private[outwatch] def transform[T](tr: Observable[O] => Observable[T]): TransformingEmitterBuilder[E, T] = copy(
    transformer = tr compose transformer
  )

  def -->(sink: Sink[_ >: O]): IO[Emitter] = {
    val observer = sink.redirect(transformer).observer
    IO.pure(Emitter(eventType, event => observer.next(event.asInstanceOf[E])))
  }
}

final class SimpleEmitterBuilder[E <: Event] private[helpers](
  val eventType: String
) extends AnyVal with
          EmitterBuilder[E, E] {

  private[outwatch] def transform[O](transformer: Observable[E] => Observable[O]) = new TransformingEmitterBuilder[E, O](eventType, transformer)

  def -->(sink: Sink[_ >: E]): IO[Emitter] = {
    IO.pure(Emitter(eventType, event => sink.observer.next(event.asInstanceOf[E])))
  }
}
object SimpleEmitterBuilder {
  import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
  import outwatch.dom.{TagWithNumber, TagWithString, TagWithBoolean, TypedCurrentTargetEvent}

  implicit class WithTypedTarget[E <: Event, Elem <: Element](event: SimpleEmitterBuilder[E with TypedTargetEvent[Elem]]) {

    object target {
      def string(implicit tag: TagWithString[Elem]): TransformingEmitterBuilder[E with TypedTargetEvent[Elem], String] = event.map(ev => tag.string(ev.target))
      def number(implicit tag: TagWithNumber[Elem]): TransformingEmitterBuilder[E with TypedTargetEvent[Elem], Double] = event.map(ev => tag.number(ev.target))
      def boolean(implicit tag: TagWithBoolean[Elem]): TransformingEmitterBuilder[E with TypedTargetEvent[Elem], Boolean] = event.map(ev => tag.boolean(ev.target))
    }
  }

  implicit class WithTypedCurrentTarget[E <: Event, Elem <: Element](event: SimpleEmitterBuilder[E with TypedCurrentTargetEvent[Elem]]) {

    object currentTarget {
      def string(implicit tag: TagWithString[Elem]): TransformingEmitterBuilder[E with TypedCurrentTargetEvent[Elem], String] = event.map(ev => tag.string(ev.currentTarget))
      def number(implicit tag: TagWithNumber[Elem]): TransformingEmitterBuilder[E with TypedCurrentTargetEvent[Elem], Double] = event.map(ev => tag.number(ev.currentTarget))
      def boolean(implicit tag: TagWithBoolean[Elem]): TransformingEmitterBuilder[E with TypedCurrentTargetEvent[Elem], Boolean] = event.map(ev => tag.boolean(ev.currentTarget))
    }
  }
}

trait HookBuilder[E, H <: Hook] {
  def hook(sink: Sink[E]): H

  def -->(sink: Sink[E]): IO[H] = IO.pure(hook(sink))
}

object InsertHookBuilder extends HookBuilder[Element, InsertHook] {
  def hook(sink: Sink[Element]) = InsertHook(sink.observer)
}

object DestroyHookBuilder extends HookBuilder[Element, DestroyHook] {
  def hook(sink: Sink[Element]) = DestroyHook(sink.observer)
}

object UpdateHookBuilder extends HookBuilder[(Element, Element), UpdateHook] {
  def hook(sink: Sink[(Element, Element)]) = UpdateHook(sink.observer)
}
