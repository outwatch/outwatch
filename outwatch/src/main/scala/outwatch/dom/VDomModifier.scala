package outwatch.dom

import cats.Monoid
import org.scalajs.dom._
import outwatch.dom.helpers.NativeHelpers._
import outwatch.reactive.{SinkObserver, Subscription, SubscriptionOwner}
import snabbdom.{DataObject, VNodeProxy}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

sealed trait VDomModifier

object VDomModifier {
  @inline def empty: VDomModifier = EmptyModifier

  @inline def apply[T : Render](t: T): VDomModifier = Render[T].render(t)

  def apply(modifier: VDomModifier, modifier2: VDomModifier, modifiers: VDomModifier*): VDomModifier = {
    CompositeModifier(Seq(modifier, modifier2, CompositeModifier(modifiers)))
  }

  @inline def delay[T : Render](modifier: => T): VDomModifier = SyncEffectModifier(() => VDomModifier(modifier))

  implicit object monoid extends Monoid[VDomModifier] {
    def empty: VDomModifier = VDomModifier.empty
    def combine(x: VDomModifier, y: VDomModifier): VDomModifier = VDomModifier(x, y)
  }

  implicit object subscriptionOwner extends SubscriptionOwner[VDomModifier] {
    @inline def own(owner: VDomModifier)(subscription: () => Subscription): VDomModifier = VDomModifier(managedFunction(subscription), owner)
  }

  @inline implicit def renderToVDomModifier[T : Render](value: T): VDomModifier = Render[T].render(value)
}

sealed trait StaticVDomModifier extends VDomModifier

final case class VNodeProxyNode(proxy: VNodeProxy) extends StaticVDomModifier

final case class Key(value: Key.Value) extends StaticVDomModifier
object Key {
  type Value = DataObject.KeyValue
}

final case class Emitter(eventType: String, trigger: js.Function1[Event, Unit]) extends StaticVDomModifier

sealed trait Attr extends StaticVDomModifier
object Attr {
  type Value = DataObject.AttrValue
}
final case class BasicAttr(title: String, value: Attr.Value) extends Attr
final case class AccumAttr(title: String, value: Attr.Value, accum: (Attr.Value, Attr.Value)=> Attr.Value) extends Attr

final case class Prop(title: String, value: Prop.Value) extends StaticVDomModifier
object Prop {
  type Value = DataObject.PropValue
}

sealed trait Style extends StaticVDomModifier
final case class AccumStyle(title: String, value: String, accum: (String, String) => String) extends Style
final case class BasicStyle(title: String, value: String) extends Style
final case class DelayedStyle(title: String, value: String) extends Style
final case class RemoveStyle(title: String, value: String) extends Style
final case class DestroyStyle(title: String, value: String) extends Style

sealed trait SnabbdomHook extends StaticVDomModifier
final case class InitHook(trigger: js.Function1[VNodeProxy, Unit]) extends SnabbdomHook
final case class InsertHook(trigger: js.Function1[VNodeProxy, Unit]) extends SnabbdomHook
final case class PrePatchHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends SnabbdomHook
final case class UpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends SnabbdomHook
final case class PostPatchHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends SnabbdomHook
final case class DestroyHook(trigger: js.Function1[VNodeProxy, Unit]) extends SnabbdomHook

sealed trait DomHook extends SnabbdomHook
final case class DomMountHook(trigger: js.Function1[VNodeProxy, Unit]) extends DomHook
final case class DomUnmountHook(trigger: js.Function1[VNodeProxy, Unit]) extends DomHook
final case class DomUpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends DomHook
final case class DomPreUpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends DomHook

final case class NextVDomModifier(modifier: StaticVDomModifier) extends StaticVDomModifier

case object EmptyModifier extends VDomModifier
final case class CompositeModifier(modifiers: Iterable[VDomModifier]) extends VDomModifier
final case class StreamModifier(subscription: SinkObserver[VDomModifier] => Subscription) extends VDomModifier
final case class SyncEffectModifier(unsafeRun: () => VDomModifier) extends VDomModifier
final case class StringVNode(text: String) extends VDomModifier

sealed trait VNode extends VDomModifier {
  def apply(args: VDomModifier*): VNode
  def append(args: VDomModifier*): VNode
  def prepend(args: VDomModifier*): VNode
}
object VNode {
  implicit object subscriptionOwner extends SubscriptionOwner[VNode] {
    @inline def own(owner: VNode)(subscription: () => Subscription): VNode = owner.append(managedFunction(subscription))
  }
}
sealed trait BasicVNode extends VNode {
  def nodeType: String
  def modifiers: js.Array[VDomModifier]
  def apply(args: VDomModifier*): BasicVNode
  def append(args: VDomModifier*): BasicVNode
  def prepend(args: VDomModifier*): BasicVNode
  def thunk(key: Key.Value)(arguments: Any*)(renderFn: => VDomModifier): ThunkVNode = ThunkVNode(this, key, arguments.toJSArray, () => renderFn)
  def thunkConditional(key: Key.Value)(shouldRender: Boolean)(renderFn: => VDomModifier): ConditionalVNode = ConditionalVNode(this, key, shouldRender, () => renderFn)
  @inline def thunkStatic(key: Key.Value)(renderFn: => VDomModifier): ConditionalVNode = thunkConditional(key)(false)(renderFn)
}
final case class ThunkVNode(baseNode: BasicVNode, key: Key.Value, arguments: js.Array[Any], renderFn: () => VDomModifier) extends VNode {
  @inline def apply(args: VDomModifier*): ThunkVNode = append(args: _*)
  @inline def append(args: VDomModifier*): ThunkVNode = copy(baseNode = baseNode(args: _*))
  @inline def prepend(args: VDomModifier*): ThunkVNode = copy(baseNode = baseNode.prepend(args :_*))
}
final case class ConditionalVNode(baseNode: BasicVNode, key: Key.Value, shouldRender: Boolean, renderFn: () => VDomModifier) extends VNode {
  @inline def apply(args: VDomModifier*): ConditionalVNode = append(args: _*)
  @inline def append(args: VDomModifier*): ConditionalVNode = copy(baseNode = baseNode(args: _*))
  @inline def prepend(args: VDomModifier*): ConditionalVNode = copy(baseNode = baseNode.prepend(args: _*))
}
@inline final case class HtmlVNode(nodeType: String, modifiers: js.Array[VDomModifier]) extends BasicVNode {
  @inline def apply(args: VDomModifier*): HtmlVNode = append(args: _*)
  @inline def append(args: VDomModifier*): HtmlVNode = copy(modifiers = appendSeq(modifiers, args))
  @inline def prepend(args: VDomModifier*): HtmlVNode = copy(modifiers = prependSeq(modifiers, args))
}
@inline final case class SvgVNode(nodeType: String, modifiers: js.Array[VDomModifier]) extends BasicVNode {
  @inline def apply(args: VDomModifier*): SvgVNode = append(args: _*)
  @inline def append(args: VDomModifier*): SvgVNode = copy(modifiers = appendSeq(modifiers, args))
  @inline def prepend(args: VDomModifier*): SvgVNode = copy(modifiers = prependSeq(modifiers, args))
}
