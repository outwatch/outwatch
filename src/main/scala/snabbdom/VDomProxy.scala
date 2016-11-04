package snabbdom

import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement
import outwatch.dom._
import outwatch.dom.helpers._

import scala.scalajs.js


object VDomProxy {

  import js.JSConverters._

  def attrsToSnabbDom(attributes: Seq[Attribute]): js.Dictionary[String] = {
    attributes.map(attr => attr.title -> attr.value)
      .toMap
      .toJSDictionary
  }

  def emittersToSnabbDom(eventEmitters: Seq[Emitter]): js.Dictionary[js.Function1[_ <: Event,Unit]] = {
    eventEmitters
      .map(emitter => (emitter.eventType, emitterToFunction(emitter)))
      .toMap[String, js.Function1[_ <: Event,Unit]]
      .toJSDictionary
  }

  def emitterToFunction(emitter: Emitter): js.Function1[_ <: Event,Unit] = emitter match {
    case ee: EventEmitter => (e: Event) => ee.sink.next(e)
    case me: MouseEventEmitter => (e: MouseEvent) => me.sink.next(e)
    case ke: KeyEventEmitter => (e: KeyboardEvent) => ke.sink.next(e)
    case ie: InputEventEmitter => (e: InputEvent) => ie.sink.next(e)
    case se: StringEventEmitter => (e: Event) => se.sink.next(e.target.asInstanceOf[HTMLInputElement].value)
    case be: BoolEventEmitter => (e: Event) => be.sink.next(e.target.asInstanceOf[HTMLInputElement].checked)
    case ne: NumberEventEmitter => (e: Event) => ne.sink.next(e.target.asInstanceOf[HTMLInputElement].valueAsNumber)
    case ge: GenericEmitter[_] => (e: Event) => ge.sink.next(ge.t)
  }

}
