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
}
object UnassignedEvent {
  implicit def UnassignedEventIsEmitterBuilder[Event <: dom.Event, Elem <: dom.Element : TagContext](event: UnassignedEvent[Event]): SimpleEmitterBuilder[Event with TypedCurrentTargetEvent[Elem]] = event.onElement[Elem]

  implicit def UnassignedEventIsEmitterBuilderWithTarget[Event <: dom.Event, Elem <: dom.Element : TagContext, Target <: dom.Element](event: UnassignedEvent[Event with TypedTargetEvent[Target]]): SimpleEmitterBuilder.WithTypedTarget[Event with TypedCurrentTargetEvent[Elem], Target] = new SimpleEmitterBuilder.WithTypedTarget(event.onElement[Elem])

  implicit def UnassignedEventIsEmitterBuilderWithCurrentTarget[Event <: dom.Event, Elem <: dom.Element : TagContext](event: UnassignedEvent[Event]): SimpleEmitterBuilder.WithTypedCurrentTarget[Event, Elem] = new SimpleEmitterBuilder.WithTypedCurrentTarget(event.onElement[Elem])
}
