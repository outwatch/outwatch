package snabbdom

import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement
import outwatch.dom._
import outwatch.dom.{Attr, Prop}
import rxscalajs.Observer

import scala.scalajs.js
import scala.scalajs.js.Dictionary


object VDomProxy {

  import js.JSConverters._

  def attrsToSnabbDom(attributes: Seq[Attribute]): (js.Dictionary[String], js.Dictionary[String]) = {
    val (attrs, props) = attributes.partition {
      case (a: Attr) => true
      case (p: Prop) => false
    }
    val attrDict = js.Dictionary(attrs.map(attr => attr.title -> attr.value): _*)
    val propDict = js.Dictionary(props.map(prop => prop.title -> prop.value): _*)

    (attrDict, propDict)
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
