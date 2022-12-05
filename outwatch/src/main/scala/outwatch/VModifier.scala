package outwatch

import outwatch.helpers.NativeHelpers._
import snabbdom.{DataObject, VNodeProxy}
import colibri._
import colibri.effect._
import cats.Monoid
import cats.syntax.functor._
import cats.effect.Sync
import org.scalajs.dom
import outwatch.helpers.ModifierBooleanOps

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

sealed trait VModifier {
  @inline final def ++(other: VModifier): VModifier     = concat(other)
  @inline final def concat(other: VModifier): VModifier = VModifier(this, other)
}

object VModifier {
  @inline def empty: VModifier = EmptyModifier

  @inline def apply(): VModifier = empty

  @inline def apply[T: Render](t: T): VModifier = Render[T].render(t)

  @inline def apply(modifier: VModifier, modifier2: VModifier): VModifier =
    CompositeModifier(js.Array(modifier, modifier2))

  @inline def apply(modifier: VModifier, modifier2: VModifier, modifier3: VModifier): VModifier =
    CompositeModifier(js.Array(modifier, modifier2, modifier3))
  @inline final def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString: Attr.Value) =
    new AttrBuilder.ToBasicAttr[T](key, convert)
  @inline final def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) =
    new AttrBuilder.ToProp[T](key, convert)
  @inline final def style[T](key: String) = new AttrBuilder.ToBasicStyle[T](key)

  def managed[F[_]: Sync: RunEffect, T: CanCancel](subscription: F[T]): VModifier = VModifier(
    subscription.map[VModifier](cancelable => CancelableModifier(() => Cancelable.lift(cancelable))),
  )

  def managedSubscribe[F[_]: Source, T](source: F[T]): VModifier = managedEval(
    Source[F].unsafeSubscribe(source)(Observer.empty),
  )

  @deprecated("Use managedEval(subscription) instead", "")
  @inline def managedFunction[T: CanCancel](subscription: () => T): VModifier = managedEval(subscription())
  def managedEval[T: CanCancel](subscription: => T): VModifier                = CancelableModifier(() => Cancelable.lift(subscription))

  object managedElement {
    def apply[T: CanCancel](subscription: dom.Element => T): VModifier = VModifier.eval {
      var lastSub: js.UndefOr[T] = js.undefined
      VModifier(
        DomMountHook(proxy => proxy.elm.foreach(elm => lastSub = subscription(elm))),
        DomUnmountHook(_ => lastSub.foreach(CanCancel[T].unsafeCancel)),
      )
    }

    @inline def asHtml[T: CanCancel](subscription: dom.html.Element => T): VModifier = apply(
      subscription.asInstanceOf[dom.Element => T],
    )

    @inline def asSvg[T: CanCancel](subscription: dom.svg.Element => T): VModifier = apply(
      subscription.asInstanceOf[dom.Element => T],
    )
  }

  @inline def apply(modifier: VModifier, modifier2: VModifier, modifier3: VModifier, modifier4: VModifier): VModifier =
    CompositeModifier(js.Array(modifier, modifier2, modifier3, modifier4))

  @inline def apply(
    modifier: VModifier,
    modifier2: VModifier,
    modifier3: VModifier,
    modifier4: VModifier,
    modifier5: VModifier,
  ): VModifier =
    CompositeModifier(js.Array(modifier, modifier2, modifier3, modifier4, modifier5))

  @inline def apply(
    modifier: VModifier,
    modifier2: VModifier,
    modifier3: VModifier,
    modifier4: VModifier,
    modifier5: VModifier,
    modifier6: VModifier,
  ): VModifier =
    CompositeModifier(js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6))

  @inline def apply(
    modifier: VModifier,
    modifier2: VModifier,
    modifier3: VModifier,
    modifier4: VModifier,
    modifier5: VModifier,
    modifier6: VModifier,
    modifier7: VModifier,
    modifiers: VModifier*,
  ): VModifier =
    CompositeModifier(
      js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, CompositeModifier(modifiers)),
    )

  @inline def fromEither[T: Render](modifier: Either[Throwable, T]): VModifier = modifier.fold(raiseError(_), apply(_))
  @inline def evalEither[T: Render](modifier: => Either[Throwable, T]): VModifier =
    SyncEffectModifier(() => fromEither(modifier))
  @deprecated("Use VModifier.eval instead", "1.0.0")
  @inline def delay[T: Render](modifier: => T): VModifier          = eval(modifier)
  @inline def eval[T: Render](modifier: => T): VModifier           = SyncEffectModifier(() => modifier)
  @inline def composite(modifiers: Iterable[VModifier]): VModifier = CompositeModifier(modifiers.toJSArray)
  @inline def raiseError[T](error: Throwable): VModifier           = ErrorModifier(error)

  @deprecated("Use VModifier.when(condition)(...) instead", "")
  @inline def ifTrue(condition: Boolean): ModifierBooleanOps = when(condition)
  @deprecated("Use VModifier.whenNot(condition)(...) instead", "")
  @inline def ifNot(condition: Boolean): ModifierBooleanOps   = whenNot(condition)
  @inline def when(condition: Boolean): ModifierBooleanOps    = new ModifierBooleanOps(condition)
  @inline def whenNot(condition: Boolean): ModifierBooleanOps = new ModifierBooleanOps(!condition)

  @inline def toggle(condition: Boolean)(ifTrue: => VModifier, ifFalse: => VModifier = VModifier.empty): VModifier =
    if (condition) ifTrue else ifFalse

  implicit object monoid extends Monoid[VModifier] {
    @inline def empty: VModifier                               = VModifier.empty
    @inline def combine(x: VModifier, y: VModifier): VModifier = VModifier(x, y)
  }

  implicit object subscriptionOwner extends SubscriptionOwner[VModifier] {
    @inline def own(owner: VModifier)(subscription: () => Cancelable): VModifier =
      VModifier(managedEval(subscription()), owner)
  }

  implicit object syncEmbed extends SyncEmbed[VModifier] {
    @inline def delay(body: => VModifier): VModifier = VModifier.eval(body)
  }

  @inline implicit def renderToVModifier[T: Render](value: T): VModifier = Render[T].render(value)
}

sealed trait StaticVModifier extends VModifier

final case class VNodeProxyNode(proxy: VNodeProxy) extends StaticVModifier

final case class Key(value: Key.Value) extends StaticVModifier
object Key {
  type Value = DataObject.KeyValue
}

final case class Emitter(eventType: String, trigger: js.Function1[dom.Event, Unit]) extends StaticVModifier

sealed trait Attr extends StaticVModifier
object Attr {
  type Value = DataObject.AttrValue
}
final case class BasicAttr(title: String, value: Attr.Value)                                                extends Attr
final case class AccumAttr(title: String, value: Attr.Value, accum: (Attr.Value, Attr.Value) => Attr.Value) extends Attr

final case class Prop(title: String, value: Prop.Value) extends StaticVModifier
object Prop {
  type Value = DataObject.PropValue
}

sealed trait Style                                                                           extends StaticVModifier
final case class AccumStyle(title: String, value: String, accum: (String, String) => String) extends Style
final case class BasicStyle(title: String, value: String)                                    extends Style
final case class DelayedStyle(title: String, value: String)                                  extends Style
final case class RemoveStyle(title: String, value: String)                                   extends Style
final case class DestroyStyle(title: String, value: String)                                  extends Style

sealed trait SnabbdomHook                                                           extends StaticVModifier
final case class InitHook(trigger: js.Function1[VNodeProxy, Unit])                  extends SnabbdomHook
final case class InsertHook(trigger: js.Function1[VNodeProxy, Unit])                extends SnabbdomHook
final case class PrePatchHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit])  extends SnabbdomHook
final case class UpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit])    extends SnabbdomHook
final case class PostPatchHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends SnabbdomHook
final case class DestroyHook(trigger: js.Function1[VNodeProxy, Unit])               extends SnabbdomHook

sealed trait DomHook                                                                   extends StaticVModifier
final case class DomMountHook(trigger: js.Function1[VNodeProxy, Unit])                 extends DomHook
final case class DomUnmountHook(trigger: js.Function1[VNodeProxy, Unit])               extends DomHook
final case class DomUpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit])    extends DomHook
final case class DomPreUpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends DomHook

final case class NextVModifier(modifier: StaticVModifier) extends StaticVModifier

case object EmptyModifier                                                        extends VModifier
final case class CompositeModifier(modifiers: Iterable[VModifier])               extends VModifier
final case class StreamModifier(subscription: Observer[VModifier] => Cancelable) extends VModifier
final case class ChildCommandsModifier(commands: Observable[Seq[ChildCommand]])  extends VModifier
final case class CancelableModifier(subscription: () => Cancelable)              extends VModifier
final case class SyncEffectModifier(unsafeRun: () => VModifier)                  extends VModifier
final case class ErrorModifier(error: Throwable)                                 extends VModifier
final case class StringVNode(text: String)                                       extends VModifier

sealed trait VNode extends VModifier {
  def apply(args: VModifier*): VNode
  def append(args: VModifier*): VNode
  def prepend(args: VModifier*): VNode

  @inline final def thunk(key: Key.Value)(arguments: Any*)(renderFn: => VModifier): VNode =
    ThunkVNode(this, key, arguments.toJSArray, () => renderFn)
  @inline final def thunkConditional(key: Key.Value)(shouldRender: Boolean)(renderFn: => VModifier): VNode =
    ConditionalVNode(this, key, shouldRender, () => renderFn)
  @inline final def thunkStatic(key: Key.Value)(renderFn: => VModifier): VNode =
    thunkConditional(key)(false)(renderFn)
}
object VNode {
  private val emptyModifierArray        = js.Array[VModifier]()
  @inline def html(name: String): VNode = HtmlVNode(name, emptyModifierArray)
  @inline def svg(name: String): VNode  = SvgVNode(name, emptyModifierArray)

  @inline def raiseError[T](error: Throwable): VNode = dsl.div(VModifier.raiseError(error))

  @inline def fromEither(modifier: Either[Throwable, VNode]): VNode    = modifier.fold(raiseError(_), identity)
  @inline def evalEither(modifier: => Either[Throwable, VNode]): VNode = SyncEffectVNode(() => fromEither(modifier))

  @inline def eval(body: => VNode): VNode = SyncEffectVNode(() => body)

  @inline def effect[F[_]: RunSyncEffect](body: F[VNode]): VNode = evalEither(RunSyncEffect[F].unsafeRun(body))

  implicit object subscriptionOwner extends SubscriptionOwner[VNode] {
    @inline def own(owner: VNode)(subscription: () => Cancelable): VNode =
      owner.append(VModifier.managedEval(subscription()))
  }

  implicit object syncEmbed extends SyncEmbed[VNode] {
    @inline def delay(body: => VNode): VNode = VNode.eval(body)
  }
}

sealed trait BasicVNode extends VNode {
  def nodeType: String
  def modifiers: js.Array[VModifier]
}
@inline final case class ThunkVNode(
  baseNode: VNode,
  key: Key.Value,
  arguments: js.Array[Any],
  renderFn: () => VModifier,
) extends VNode {
  @inline def apply(args: VModifier*): VNode   = append(args: _*)
  @inline def append(args: VModifier*): VNode  = copy(baseNode = baseNode(args: _*))
  @inline def prepend(args: VModifier*): VNode = copy(baseNode = baseNode.prepend(args: _*))
}
@inline final case class ConditionalVNode(
  baseNode: VNode,
  key: Key.Value,
  shouldRender: Boolean,
  renderFn: () => VModifier,
) extends VNode {
  @inline def apply(args: VModifier*): VNode   = append(args: _*)
  @inline def append(args: VModifier*): VNode  = copy(baseNode = baseNode(args: _*))
  @inline def prepend(args: VModifier*): VNode = copy(baseNode = baseNode.prepend(args: _*))
}
@inline final case class HtmlVNode(nodeType: String, modifiers: js.Array[VModifier]) extends BasicVNode {
  @inline def apply(args: VModifier*): VNode   = append(args: _*)
  @inline def append(args: VModifier*): VNode  = copy(modifiers = appendSeq(modifiers, args))
  @inline def prepend(args: VModifier*): VNode = copy(modifiers = prependSeq(modifiers, args))
}
@inline final case class SvgVNode(nodeType: String, modifiers: js.Array[VModifier]) extends BasicVNode {
  @inline def apply(args: VModifier*): VNode   = append(args: _*)
  @inline def append(args: VModifier*): VNode  = copy(modifiers = appendSeq(modifiers, args))
  @inline def prepend(args: VModifier*): VNode = copy(modifiers = prependSeq(modifiers, args))
}
@inline final case class SyncEffectVNode(unsafeRun: () => VNode) extends VNode {
  @inline def apply(args: VModifier*): VNode   = append(args: _*)
  @inline def append(args: VModifier*): VNode  = SyncEffectVNode(() => unsafeRun().append(args))
  @inline def prepend(args: VModifier*): VNode = SyncEffectVNode(() => unsafeRun().prepend(args))
}
