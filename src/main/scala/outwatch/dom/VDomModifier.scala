package outwatch.dom

import cats.effect.Effect

import cats.instances.list._
import cats.syntax.all._
import monix.execution.{Ack, Scheduler}
import org.scalajs.dom._
import outwatch.ReactiveTypes
import outwatch.dom.helpers.SeparatedModifiersFactory
import snabbdom.{DataObject, VNodeProxy}

import scala.concurrent.Future

trait VDomModifierFactory[F[+_]] extends ReactiveTypes[F] with SeparatedModifiersFactory[F] {
  implicit val effectF:Effect[F]

  type VNode = F[VTree]
  type VDomModifier = F[Modifier]

  object VDomModifier {
    def empty: VDomModifier = effectF.pure(EmptyModifier)

    def apply(modifiers: VDomModifier*): VDomModifier =
      modifiers.toList.sequence.map(CompositeModifier)
  }



  /*
Modifier
  Property
    Attribute
      TitledAttribute
        Attr
          BasicAttr
          AccumAttr
        Prop
        Style
          BasicStyle
          DelayedStyle
          RemoveStyle
          DestroyStyle
          AccumStyle
      EmptyAttribute
    Hook
      InsertHook
      PrePatchHook
      UpdateHook
      PostPatchHook
      DestroyHook
    Key
  ChildVNode
    StreamVNode
      ChildStreamReceiver
      ChildrenStreamReceiver
    StaticVNode
      StringVNode
      VTree
  Emitter
  AttributeStreamReceiver
  CompositeModifier
  StringModifier
  EmptyModifier
 */


  sealed trait Modifier

  // Modifiers

  sealed trait Property extends Modifier

  case class Emitter(eventType: String, trigger: Event => Future[Ack]) extends Modifier

  private[outwatch] sealed case class AttributeStreamReceiver(attribute: String, attributeStream: Observable[Attribute]) extends Modifier

  private[outwatch] sealed case class CompositeModifier(modifiers: Seq[Modifier]) extends Modifier

  case object EmptyModifier extends Modifier

  private[outwatch] sealed case class StringModifier(string: String) extends Modifier

  sealed trait ChildVNode extends Modifier

  // Properties

  sealed case class Key(value: Key.Value) extends Property

  object Key {
    type Value = DataObject.KeyValue
  }

  sealed trait Attribute extends Property

  object Attribute {
    def apply(title: String, value: Attr.Value): Attribute = BasicAttr(title, value)

    val empty: Attribute = EmptyAttribute
  }


  sealed trait Hook[T] extends Property {
    def observer: Observer[T]
  }

  // Attributes

  private[outwatch] case object EmptyAttribute extends Attribute

  sealed trait TitledAttribute extends Attribute {
    val title: String
  }


  sealed trait Attr extends TitledAttribute {
    val value: Attr.Value
  }

  object Attr {
    type Value = DataObject.AttrValue
  }

  sealed case class BasicAttr(title: String, value: Attr.Value) extends Attr

  /**
    * Attribute that accumulates the previous value in the same VNode with it's value
    */
  sealed case class AccumAttr(title: String, value: Attr.Value, accum: (Attr.Value, Attr.Value) => Attr.Value) extends Attr

  sealed case class Prop(title: String, value: Prop.Value) extends TitledAttribute

  object Prop {
    type Value = DataObject.PropValue
  }

  sealed trait Style extends TitledAttribute {
    val value: String
  }

  object Style {
    type Value = DataObject.StyleValue
  }

  sealed case class AccumStyle(title: String, value: String, accum: (String, String) => String) extends Style

  sealed case class BasicStyle(title: String, value: String) extends Style

  sealed case class DelayedStyle(title: String, value: String) extends Style

  sealed case class RemoveStyle(title: String, value: String) extends Style

  sealed case class DestroyStyle(title: String, value: String) extends Style

  // Hooks

  private[outwatch] sealed case class InsertHook(observer: Observer[Element]) extends Hook[Element]

  private[outwatch] sealed case class PrePatchHook(observer: Observer[(Option[Element], Option[Element])])
    extends Hook[(Option[Element], Option[Element])]

  private[outwatch] sealed case class UpdateHook(observer: Observer[(Element, Element)]) extends Hook[(Element, Element)]

  private[outwatch] sealed case class PostPatchHook(observer: Observer[(Element, Element)]) extends Hook[(Element, Element)]

  private[outwatch] sealed case class DestroyHook(observer: Observer[Element]) extends Hook[Element]

  // Child Nodes

  private[outwatch] sealed trait StreamVNode extends ChildVNode

  private[outwatch] sealed trait StaticVNode extends ChildVNode {
    def toSnabbdom(implicit s: Scheduler): VNodeProxy
  }

  object StaticVNode {
    val empty: StaticVNode = StringVNode("")
  }

  private[outwatch] sealed case class ChildStreamReceiver(childStream: Observable[F[StaticVNode]]) extends StreamVNode

  private[outwatch] sealed case class ChildrenStreamReceiver(childrenStream: Observable[Seq[F[StaticVNode]]]) extends StreamVNode

  // Static Nodes
  private[outwatch] sealed case class StringVNode(string: String) extends StaticVNode {

    override def toSnabbdom(implicit s: Scheduler): VNodeProxy = VNodeProxy.fromString(string)
  }

  // TODO: instead of Seq[VDomModifier] use Vector or JSArray?
  // Fast concatenation and lastOption operations are important
  // Needs to be benchmarked in the Browser
  private[outwatch] sealed case class VTree(nodeType: String, modifiers: Seq[Modifier]) extends StaticVNode {

    //TODO: why .toList.sequence?
    def apply(args: VDomModifier*): VNode =
      args.toList.sequence.map(args => copy(modifiers = modifiers ++ args))

    override def toSnabbdom(implicit s: Scheduler): VNodeProxy = {
      SeparatedModifiers.from(modifiers).toSnabbdom(nodeType)
    }
  }

}
