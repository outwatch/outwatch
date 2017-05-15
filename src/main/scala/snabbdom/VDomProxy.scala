package snabbdom

import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement
import outwatch.dom._
import outwatch.dom.helpers._
import rxscalajs.Observer
import scala.scalajs.js


object VDomProxy {

  import js.JSConverters._

  def attrsToSnabbDom(attributes: Seq[Attribute]): js.Dictionary[String] = {
    attributes.map(attr => attr.title -> attr.value)
      .toMap
      .toJSDictionary
  }


  def emittersToSnabbDom(eventEmitters: Seq[Emitter]): js.Dictionary[js.Function1[Event,Unit]] = {
    eventEmitters
      .groupBy(_.eventType)
      .mapValues(emittersToFunction)
      .toJSDictionary
  }

  private def emittersToFunction(emitters: Seq[Emitter]): js.Function1[Event, Unit] = {
    (e: Event) => emitters.map(emitterToFunction).foreach(_.apply(e))
  }

  private def emitterToFunction(emitter: Emitter): Event => Unit = emitter match {
    case se: StringEventEmitter => (e: Event) => se.sink.next(e.target.asInstanceOf[HTMLInputElement].value)
    case be: BoolEventEmitter => (e: Event) => be.sink.next(e.target.asInstanceOf[HTMLInputElement].checked)
    case ne: NumberEventEmitter => (e: Event) => ne.sink.next(e.target.asInstanceOf[HTMLInputElement].valueAsNumber)
    case ee: EventEmitter[_] => (e: Event) => ee.sink.asInstanceOf[Observer[Event]].next(e)
  }


}
