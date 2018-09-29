package outwatch.dom

import cats.effect.IO
import monix.execution.Scheduler
import org.scalajs.dom._
import outwatch.AsVDomModifier
import snabbdom.{DataObject, VNodeProxy}

import scala.scalajs.js

sealed trait VDomModifier

object VDomModifier {
  val empty: VDomModifier = EmptyModifier

  def apply(modifier: VDomModifier, modifier2: VDomModifier, modifiers: VDomModifier*): VDomModifier =
    CompositeModifier(js.Array(modifier, modifier2) ++ modifiers)
  def apply[T](t: T)(implicit as: AsVDomModifier[T]): VDomModifier = as.asVDomModifier(t)
}

sealed trait StaticVDomModifier extends VDomModifier

final case class VNodeProxyNode(proxy: VNodeProxy) extends StaticVDomModifier

final case class Key(value: Key.Value) extends StaticVDomModifier
object Key {
  type Value = DataObject.KeyValue
}

final case class Emitter(eventType: String, trigger: Event => Unit) extends StaticVDomModifier

sealed trait Attr extends StaticVDomModifier {
  val value: Attr.Value
}
object Attr {
  type Value = DataObject.AttrValue
}
final case class BasicAttr(title: String, value: Attr.Value) extends Attr
final case class AccumAttr(title: String, value: Attr.Value, accum: (Attr.Value, Attr.Value)=> Attr.Value) extends Attr

final case class Prop(title: String, value: Prop.Value) extends StaticVDomModifier
object Prop {
  type Value = DataObject.PropValue
}

sealed trait Style extends StaticVDomModifier {
  val value: String
}
object Style {
  type Value = DataObject.StyleValue
}
final case class AccumStyle(title: String, value: String, accum: (String, String) => String) extends Style
final case class BasicStyle(title: String, value: String) extends Style
final case class DelayedStyle(title: String, value: String) extends Style
final case class RemoveStyle(title: String, value: String) extends Style
final case class DestroyStyle(title: String, value: String) extends Style

sealed trait Hook extends StaticVDomModifier
final case class DomMountHook(trigger: Element => Unit) extends Hook
final case class DomUnmountHook(trigger: Element => Unit) extends Hook
final case class DomUpdateHook(trigger: Element => Unit) extends Hook
final case class DomPreUpdateHook(trigger: Element => Unit) extends Hook
final case class InsertHook(trigger: Element => Unit) extends Hook
final case class PrePatchHook(trigger: ((Option[Element], Option[Element])) => Unit) extends Hook
final case class UpdateHook(trigger: ((Element, Element)) => Unit) extends Hook
final case class PostPatchHook(trigger: ((Element, Element)) => Unit) extends Hook
final case class DestroyHook(trigger: Element => Unit) extends Hook


case object EmptyModifier extends VDomModifier
final case class CompositeModifier(modifiers: js.Array[_ <: VDomModifier]) extends VDomModifier
final case class ModifierStreamReceiver(stream: ValueObservable[VDomModifier]) extends VDomModifier
final case class EffectModifier(effect: IO[VDomModifier]) extends VDomModifier
final case class SchedulerAction(action: Scheduler => VDomModifier) extends VDomModifier
final case class StringVNode(text: String) extends VDomModifier
final case class VNode(nodeType: String, modifiers: js.Array[VDomModifier]) extends VDomModifier {
  def apply(args: VDomModifier*): VNode = copy(modifiers = modifiers ++ args)
}
