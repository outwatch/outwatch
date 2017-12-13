package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom.{DestroyHook, Emitter, Hook, InsertHook, PostPatchHook, PrePatchHook, UpdateHook}
import rxscalajs.Observable


trait EmitterBuilder[E <: Event, O] extends Any {

  private[outwatch] def transform[T](tr: Observable[O] => Observable[T]): TransformingEmitterBuilder[E, T]

  def apply[T](value: T): TransformingEmitterBuilder[E, T] = map(_ => value)

  def apply[T](latest: Observable[T]): TransformingEmitterBuilder[E, T] = transform(_.withLatestFromWith(latest)((_, u) => u))

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): TransformingEmitterBuilder[E, T] = map(f)

  def map[T](f: O => T): TransformingEmitterBuilder[E, T] = transform(_.map(f))

  def filter(predicate: O => Boolean): TransformingEmitterBuilder[E, O] = transform(_.filter(predicate))

  def collect[T](f: PartialFunction[O, T]): TransformingEmitterBuilder[E, T] = transform(_.collect(f))

  def -->(sink: Sink[_ >: O]): IO[Emitter]
}
object EmitterBuilder {
  import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
  import outwatch.dom.{TagWithNumber, TagWithString, TagWithChecked, TypedCurrentTargetEvent}


  class EmitterBuilderTargetOps[E <: Event, O <: Event, Elem <: EventTarget](event: EmitterBuilder[E, O], getTarget: O => Elem) {
    def value(implicit tag: TagWithString[Elem]): EmitterBuilder[E, String] = event.map(ev => tag.value(getTarget(ev)))
    def valueAsNumber(implicit tag: TagWithNumber[Elem]): EmitterBuilder[E, Double] = event.map(ev => tag.valueAsNumber(getTarget(ev)))
    def checked(implicit tag: TagWithChecked[Elem]): EmitterBuilder[E, Boolean] = event.map(ev => tag.checked(getTarget(ev)))
  }
  implicit class WithTargetAs[E <: Event, O <: Event](private val event: EmitterBuilder[E, O]) extends AnyVal {
    def targetAs[Elem <: EventTarget] = new EmitterBuilderTargetOps[E, O with TypedTargetEvent[Elem], Elem](event.asInstanceOf[EmitterBuilder[E, O with TypedTargetEvent[Elem]]], _.target)
  }
  implicit class WithTypedTarget[E <: Event, O <: Event, Elem <: EventTarget](private val event: EmitterBuilder[E, O with TypedTargetEvent[Elem]]) extends AnyVal {
    def target = new EmitterBuilderTargetOps[E, O with TypedTargetEvent[Elem], Elem](event, _.target)
  }
  implicit class WithTypedCurrentTarget[E <: Event, O <: Event, Elem <: EventTarget](private val event: EmitterBuilder[E, O with TypedCurrentTargetEvent[Elem]]) extends EmitterBuilderTargetOps[E, O with TypedCurrentTargetEvent[Elem], Elem](event, _.currentTarget)

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

trait HookBuilder[E, H <: Hook[_]] {
  def hook(sink: Sink[E]): H

  def -->(sink: Sink[E]): IO[H] = IO.pure(hook(sink))
}

object InsertHookBuilder extends HookBuilder[Element, InsertHook] {
  def hook(sink: Sink[Element]) = InsertHook(sink.observer)
}

object PrePatchHookBuilder extends HookBuilder[(Option[Element], Option[Element]), PrePatchHook] {
  def hook(sink: Sink[(Option[Element], Option[Element])]) = PrePatchHook(sink.observer)
}

object UpdateHookBuilder extends HookBuilder[(Element, Element), UpdateHook] {
  def hook(sink: Sink[(Element, Element)]) = UpdateHook(sink.observer)
}

object PostPatchHookBuilder extends HookBuilder[(Element, Element), PostPatchHook] {
  def hook(sink: Sink[(Element, Element)]) = PostPatchHook(sink.observer)
}

object DestroyHookBuilder extends HookBuilder[Element, DestroyHook] {
  def hook(sink: Sink[Element]) = DestroyHook(sink.observer)
}
