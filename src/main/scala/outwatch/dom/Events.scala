package outwatch.dom

import scala.scalajs.js
import org.scalajs.dom
import helpers.{EmitterBuilder, SimpleEmitterBuilder, TransformingEmitterBuilder}
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent

/** Represents an event for which we know what exact type the `currentTarget` is. */
@js.native
trait TypedCurrentTargetEvent[+E <: dom.EventTarget] extends dom.Event {
  override def currentTarget: E = js.native
}

case class UnassignedEvent[Event <: dom.Event](key: String) {
  def onElement[Elem <: dom.Element]: SimpleEmitterBuilder[Event with TypedCurrentTargetEvent[Elem]] = EmitterBuilder(key)

  object currentTarget {
    def string[Elem <: dom.Element : TagContext](implicit tag: TagWithString[Elem]): TransformingEmitterBuilder[Event with TypedCurrentTargetEvent[Elem], String] = onElement[Elem].map(ev => tag.string(ev.currentTarget))
    def number[Elem <: dom.Element : TagContext](implicit tag: TagWithNumber[Elem]): TransformingEmitterBuilder[Event with TypedCurrentTargetEvent[Elem], Double] = onElement[Elem].map(ev => tag.number(ev.currentTarget))
    def boolean[Elem <: dom.Element : TagContext](implicit tag: TagWithBoolean[Elem]): TransformingEmitterBuilder[Event with TypedCurrentTargetEvent[Elem], Boolean] = onElement[Elem].map(ev => tag.boolean(ev.currentTarget))
  }
}
object UnassignedEvent {
  implicit def UnassignedEventIsEmitterBuilder[Event <: dom.Event, Elem <: dom.Element : TagContext](event: UnassignedEvent[Event]): SimpleEmitterBuilder[Event with TypedCurrentTargetEvent[Elem]] = event.onElement[Elem]

  implicit class WithTypedTarget[Target <: dom.Element, Event <: dom.Event](event: UnassignedEvent[Event with TypedTargetEvent[Target]]) {
    object target {
      def string[Elem <: dom.Element : TagContext](implicit tag: TagWithString[Target]): TransformingEmitterBuilder[Event with TypedTargetEvent[Target] with TypedCurrentTargetEvent[Elem], String] = event.onElement[Elem].map(ev => tag.string(ev.target))
      def number[Elem <: dom.Element : TagContext](implicit tag: TagWithNumber[Target]): TransformingEmitterBuilder[Event with TypedTargetEvent[Target] with TypedCurrentTargetEvent[Elem], Double] = event.onElement[Elem].map(ev => tag.number(ev.target))
      def boolean[Elem <: dom.Element : TagContext](implicit tag: TagWithBoolean[Target]): TransformingEmitterBuilder[Event with TypedTargetEvent[Target] with TypedCurrentTargetEvent[Elem], Boolean] = event.onElement[Elem].map(ev => tag.boolean(ev.target))
    }
  }
}
