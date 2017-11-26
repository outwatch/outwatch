package outwatch.dom

import org.scalajs.dom

import outwatch.dom.helpers.{EmitterBuilder, SimpleEmitterBuilder}

class TagContext[Elem <: dom.Element] {
  def event[Event <: dom.Event](builder: SimpleEmitterBuilder[Event]): SimpleEmitterBuilder[Event with TypedCurrentTargetEvent[Elem]] = EmitterBuilder(builder.eventType)
}
