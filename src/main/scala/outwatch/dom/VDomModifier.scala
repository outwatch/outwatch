package outwatch.dom

import cats.effect.IO
import org.scalajs.dom._
import outwatch.dom.helpers.DomUtils
import scala.scalajs.js.|
import rxscalajs.Observer
import snabbdom.{VNodeProxy, h}

import scala.scalajs.js
import collection.breakOut

sealed trait VDomModifier_ extends Any

case class Emitter(eventType: String, trigger: Event => Unit) extends VDomModifier_

sealed trait Property extends VDomModifier_
sealed trait Attribute extends Property {
  val title: String
}

object Attribute {
  def apply(title: String, value: String | Boolean) = Attr(title, value)
}


final case class Attr(title: String, value: String | Boolean) extends Attribute
final case class Prop(title: String, value: String) extends Attribute
final case class Style(title: String, value: String) extends Attribute
final case class Key(value: String) extends Property

sealed trait Hook extends Property
final case class InsertHook(observer: Observer[Element]) extends Hook
final case class DestroyHook(observer: Observer[Element]) extends Hook
final case class UpdateHook(observer: Observer[(Element, Element)]) extends Hook

sealed trait Receiver extends VDomModifier_
final case class AttributeStreamReceiver(attribute: String, attributeStream: Observable[Attribute]) extends Receiver
final case class ChildStreamReceiver(childStream: Observable[VNode]) extends Receiver
final case class ChildrenStreamReceiver(childrenStream: Observable[Seq[VNode]]) extends Receiver

case object EmptyVDomModifier extends VDomModifier_

sealed trait VNode_ extends VDomModifier_ {
  // TODO: have apply() only on VTree?
  def apply(args: VDomModifier*): VNode = ???
  // TODO: rename asProxy to asSnabbdom?
  def asProxy: VNodeProxy
}

//TODO: extends AnyVal
private[outwatch] case class StringNode(string: String) extends VNode_ {
  val asProxy: VNodeProxy = VNodeProxy.fromString(string)
}

// TODO: instead of Seq[VDomModifier] use Vector or JSArray?
// Fast concatenation and lastOption operations are important
// Needs to be benchmarked in the Browser
private[outwatch] final case class VTree(nodeType: String,
                       modifiers: Seq[VDomModifier]) extends VNode_ {

  def asProxy = {
    val modifiers_ = modifiers.map(_.unsafeRunSync())
    val (children, attributeObject) = DomUtils.extractChildrenAndDataObject(modifiers_)
    //TODO: use .sequence instead of unsafeRunSync?
    // import cats.instances.list._
    // import cats.syntax.traverse._
    // for { childProxies <- children.map(_.value).sequence }
    // yield h(nodeType, attributeObject, childProxies.map(_.apsProxy)(breakOut))
    val childProxies: js.Array[VNodeProxy] = children.map(_.asProxy)(breakOut)
    h(nodeType, attributeObject, childProxies)
  }

  override def apply(args: VDomModifier*) = IO.pure(VTree(nodeType, modifiers ++ args))
}




