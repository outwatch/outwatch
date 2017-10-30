package outwatch.dom

import cats.Monad
import cats.effect.IO
import org.scalajs.dom._
import outwatch.Sink
import rxscalajs.dom.Response
import outwatch.dom.helpers.DomUtils
import scala.scalajs.js.|
import rxscalajs.{Observable, Observer}
import snabbdom.{DataObject, VNodeProxy, h}

import scala.scalajs.js
import collection.breakOut

sealed trait VDomModifier extends Any

sealed trait Emitter extends VDomModifier {
  val eventType: String
}

sealed trait Property extends VDomModifier

sealed trait Receiver extends VDomModifier

final case class EventEmitter[E <: Event](eventType: String, sink: Observer[E]) extends Emitter
final case class StringEventEmitter(eventType: String, sink: Observer[String]) extends Emitter
final case class BoolEventEmitter(eventType: String, sink: Observer[Boolean]) extends Emitter
final case class NumberEventEmitter(eventType: String, sink: Observer[Double]) extends Emitter

sealed trait Attribute extends Property {
  val title: String
}

object Attribute {
  def apply(title: String, value: String | Boolean) = Attr(title, value)
}

final case class Attr(title: String, value: String | Boolean) extends Attribute
final case class Prop(title: String, value: String) extends Attribute
final case class Style(title: String, value: String) extends Attribute
final case class InsertHook(sink: Observer[Element]) extends Property
final case class DestroyHook(sink: Observer[Element]) extends Property
final case class UpdateHook(sink: Observer[(Element, Element)]) extends Property
final case class Key(value: String) extends Property

final case class AttributeStreamReceiver(attribute: String, attributeStream: Observable[Attribute]) extends Receiver
final case class ChildStreamReceiver(childStream: Observable[VNode]) extends Receiver
final case class ChildrenStreamReceiver(childrenStream: Observable[Seq[VNode]]) extends Receiver

case object EmptyVDomModifier extends VDomModifier

sealed trait VNode extends VDomModifier {
  // TODO: have apply() only on VTree?
  def apply(args: VDomModifier*): VNode = ???
  // TODO: rename asProxy to asSnabbdom?
  def asProxy: VNodeProxy
}

object VDomModifier {
  //TODO: extends AnyVal
  implicit class StringNode(val string: String) extends VNode {
    val asProxy: VNodeProxy = VNodeProxy.fromString(string)
  }

  implicit def optionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse EmptyVDomModifier

  // TODO: instead of Seq[VDomModifier] use Vector or JSArray?
  // Fast concatenation and lastOption operations are important
  // Needs to be benchmarked in the Browser
  final case class VTree(nodeType: String,
                         modifiers: Seq[VDomModifier]) extends VNode {

    def asProxy = {
      val (children, attributeObject) = DomUtils.extractChildrenAndDataObject(modifiers)
      //TODO: use .sequence instead of unsafeRunSync?
      // import cats.instances.list._
      // import cats.syntax.traverse._
      // for { childProxies <- children.map(_.value).sequence }
      // yield h(nodeType, attributeObject, childProxies.map(_.apsProxy)(breakOut))
      val childProxies: js.Array[VNodeProxy] = children.map(_.asProxy)(breakOut)
      h(nodeType, attributeObject, childProxies)
    }

    override def apply(args: VDomModifier*) = VTree(nodeType, modifiers ++ args)
  }
}



