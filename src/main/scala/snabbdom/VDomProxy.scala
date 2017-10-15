package snabbdom

import org.scalajs.dom._
import outwatch.dom.{Attr, Prop, _}

import scala.scalajs.js

object VDomProxy {

  import js.JSConverters._

  def attrsToSnabbDom(attributes: Seq[Attribute]): (js.Dictionary[Attribute.Value], js.Dictionary[String], js.Dictionary[String]) = {
    val attrsDict = js.Dictionary[Attribute.Value]()
    val propsDict = js.Dictionary[String]()
    val styleDict = js.Dictionary[String]()

    attributes.foreach {
      case a:Attr => attrsDict(a.title) = a.value
      case a:Prop => propsDict(a.title) = a.value
      case a:Style => styleDict(a.title) = a.value
    }

    (attrsDict, propsDict, styleDict)
  }


  def emittersToSnabbDom(eventEmitters: Seq[Emitter]): js.Dictionary[js.Function1[Event,Unit]] = {
    eventEmitters
      .groupBy(_.eventType)
      .mapValues(emittersToFunction)
      .toJSDictionary
  }

  private def emittersToFunction(emitters: Seq[Emitter]): js.Function1[Event, Unit] = {
    (event: Event) => emitters.foreach(_.trigger(event))
  }
}
