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
    case me: MouseEventEmitter => (e: Event) => me.sink.next(e.asInstanceOf[MouseEvent])
    case we: WheelEventEmitter => (e: Event) => we.sink.next(e.asInstanceOf[WheelEvent])
    case te: TouchEventEmitter => (e: Event) => te.sink.next(e.asInstanceOf[TouchEvent])
    case ke: KeyEventEmitter => (e: Event) => ke.sink.next(e.asInstanceOf[KeyboardEvent])
    case ie: InputEventEmitter => (e: Event) => ie.sink.next(e.asInstanceOf[InputEvent])
    case ce: ClipboardEventEmitter => (e: Event) => ce.sink.next(e.asInstanceOf[ClipboardEvent])
    case de: DragEventEmitter => (e: Event) => de.sink.next(e.asInstanceOf[DragEvent])
    case se: StringEventEmitter => (e: Event) => se.sink.next(e.target.asInstanceOf[HTMLInputElement].value)
    case be: BoolEventEmitter => (e: Event) => be.sink.next(e.target.asInstanceOf[HTMLInputElement].checked)
    case ne: NumberEventEmitter => (e: Event) => ne.sink.next(e.target.asInstanceOf[HTMLInputElement].valueAsNumber)
    case ee: EventEmitter => (e: Event) => ee.sink.asInstanceOf[Observer[Event]].next(e)
  }


}
