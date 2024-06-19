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

sealed trait VMod {
  @inline final def ++(other: VMod): VMod     = concat(other)
  @inline final def concat(other: VMod): VMod = VMod(this, other)
}

object VMod {
  @inline def empty: VMod = EmptyModifier

  @inline def apply(): VMod = empty

  @inline def apply[T: Render](t: T): VMod = Render[T].render(t)

  @inline def apply(modifier: VMod, modifier2: VMod): VMod =
    CompositeModifier(js.Array(modifier, modifier2))

  @inline def apply(modifier: VMod, modifier2: VMod, modifier3: VMod): VMod =
    CompositeModifier(js.Array(modifier, modifier2, modifier3))
  @inline final def attr[T](key: String, convert: T => Attr.Value = Attr.toValue): AttrBuilder.ToBasicAttr[T] =
    new AttrBuilder.ToBasicAttr[T](key, convert)
  @inline final def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) =
    new AttrBuilder.ToProp[T](key, convert)
  @inline final def style[T](key: String) = new AttrBuilder.ToBasicStyle[T](key)

  def managed[F[_]: Sync: RunEffect, T: CanCancel](subscription: F[T]): VMod = VMod(
    subscription.map[VMod](cancelable => CancelableModifier(() => Cancelable.lift(cancelable))),
  )

  def managedSubscribe[F[_]: Source, T](source: F[T]): VMod = managedEval(
    Source[F].unsafeSubscribe(source)(Observer.empty),
  )

  @deprecated("Use managedEval(subscription) instead", "")
  @inline def managedFunction[T: CanCancel](subscription: () => T): VMod = managedEval(subscription())
  def managedEval[T: CanCancel](subscription: => T): VMod                = CancelableModifier(() => Cancelable.lift(subscription))

  object managedElement {
    def apply[T: CanCancel](subscription: dom.Element => T): VMod = VMod.eval {
      var lastSub: js.UndefOr[T] = js.undefined
      VMod(
        DomMountHook(proxy => proxy.elm.foreach(elm => lastSub = subscription(elm))),
        DomUnmountHook(_ => lastSub.foreach(CanCancel[T].unsafeCancel)),
      )
    }

    @inline def asHtml[T: CanCancel](subscription: dom.html.Element => T): VMod = apply(
      subscription.asInstanceOf[dom.Element => T],
    )

    @inline def asSvg[T: CanCancel](subscription: dom.svg.Element => T): VMod = apply(
      subscription.asInstanceOf[dom.Element => T],
    )
  }

  @inline def apply(modifier: VMod, modifier2: VMod, modifier3: VMod, modifier4: VMod): VMod =
    CompositeModifier(js.Array(modifier, modifier2, modifier3, modifier4))

  @inline def apply(
    modifier: VMod,
    modifier2: VMod,
    modifier3: VMod,
    modifier4: VMod,
    modifier5: VMod,
  ): VMod =
    CompositeModifier(js.Array(modifier, modifier2, modifier3, modifier4, modifier5))

  @inline def apply(
    modifier: VMod,
    modifier2: VMod,
    modifier3: VMod,
    modifier4: VMod,
    modifier5: VMod,
    modifier6: VMod,
  ): VMod =
    CompositeModifier(js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6))

  @inline def apply(
    modifier: VMod,
    modifier2: VMod,
    modifier3: VMod,
    modifier4: VMod,
    modifier5: VMod,
    modifier6: VMod,
    modifier7: VMod,
    modifiers: VMod*,
  ): VMod =
    CompositeModifier(
      js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, CompositeModifier(modifiers)),
    )

  @inline def fromEither[T: Render](modifier: Either[Throwable, T]): VMod = modifier.fold(raiseError(_), apply(_))
  @inline def evalEither[T: Render](modifier: => Either[Throwable, T]): VMod =
    SyncEffectModifier(() => fromEither(modifier))
  @deprecated("Use VMod.eval instead", "1.0.0")
  @inline def delay[T: Render](modifier: => T): VMod     = eval(modifier)
  @inline def eval[T: Render](modifier: => T): VMod      = SyncEffectModifier(() => modifier)
  @inline def composite(modifiers: Iterable[VMod]): VMod = CompositeModifier(modifiers.toJSArray)
  @inline def raiseError(error: Throwable): VMod         = ErrorModifier(error)

  @inline def catchError(modifier: VMod)(recover: Throwable => VMod): VMod =
    configured(modifier)(_.copy(errorModifier = recover))
  @inline def configured(modifier: VMod)(configure: RenderConfig => RenderConfig): VMod =
    ConfiguredModifier(modifier, configure)

  @deprecated("Use VMod.when(condition)(...) instead", "")
  @inline def ifTrue(condition: Boolean): ModifierBooleanOps = when(condition)
  @deprecated("Use VMod.whenNot(condition)(...) instead", "")
  @inline def ifNot(condition: Boolean): ModifierBooleanOps   = whenNot(condition)
  @inline def when(condition: Boolean): ModifierBooleanOps    = new ModifierBooleanOps(condition)
  @inline def whenNot(condition: Boolean): ModifierBooleanOps = new ModifierBooleanOps(!condition)

  @inline def toggle(condition: Boolean)(ifTrue: => VMod, ifFalse: => VMod = VMod.empty): VMod =
    if (condition) ifTrue else ifFalse

  implicit object monoid extends Monoid[VMod] {
    @inline def empty: VMod                     = VMod.empty
    @inline def combine(x: VMod, y: VMod): VMod = VMod(x, y)
  }

  implicit object subscriptionOwner extends SubscriptionOwner[VMod] {
    @inline def own(owner: VMod)(subscription: () => Cancelable): VMod =
      VMod(managedEval(subscription()), owner)
  }

  implicit object syncEmbed extends SyncEmbed[VMod] {
    @inline def delay(body: => VMod): VMod = VMod.eval(body)
  }

  @inline implicit def renderToVMod[T: Render](value: T): VMod = Render[T].render(value)
}

sealed trait StaticVMod extends VMod

final case class VNodeProxyNode(proxy: VNodeProxy) extends StaticVMod

final case class Key(value: Key.Value) extends StaticVMod
object Key {
  type Value = DataObject.KeyValue
}

final case class Emitter(eventType: String, trigger: js.Function1[dom.Event, Unit]) extends StaticVMod

sealed trait Attr extends StaticVMod
object Attr {
  type Value = DataObject.AttrValue
  val toValue: Any => Value = {
    case boolean: Boolean => boolean
    case any              => any.toString
  }
}
final case class BasicAttr(title: String, value: Attr.Value)                                                extends Attr
final case class AccumAttr(title: String, value: Attr.Value, accum: (Attr.Value, Attr.Value) => Attr.Value) extends Attr

final case class Prop(title: String, value: Prop.Value) extends StaticVMod
object Prop {
  type Value = DataObject.PropValue
}

sealed trait Style                                                                           extends StaticVMod
final case class AccumStyle(title: String, value: String, accum: (String, String) => String) extends Style
final case class BasicStyle(title: String, value: String)                                    extends Style
final case class DelayedStyle(title: String, value: String)                                  extends Style
final case class RemoveStyle(title: String, value: String)                                   extends Style
final case class DestroyStyle(title: String, value: String)                                  extends Style

sealed trait SnabbdomHook                                                           extends StaticVMod
final case class InitHook(trigger: js.Function1[VNodeProxy, Unit])                  extends SnabbdomHook
final case class InsertHook(trigger: js.Function1[VNodeProxy, Unit])                extends SnabbdomHook
final case class PrePatchHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit])  extends SnabbdomHook
final case class UpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit])    extends SnabbdomHook
final case class PostPatchHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends SnabbdomHook
final case class DestroyHook(trigger: js.Function1[VNodeProxy, Unit])               extends SnabbdomHook

sealed trait DomHook                                                                   extends StaticVMod
final case class DomMountHook(trigger: js.Function1[VNodeProxy, Unit])                 extends DomHook
final case class DomUnmountHook(trigger: js.Function1[VNodeProxy, Unit])               extends DomHook
final case class DomUpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit])    extends DomHook
final case class DomPreUpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends DomHook

final case class NextVMod(modifier: StaticVMod) extends StaticVMod

case object EmptyModifier                                                       extends VMod
final case class CompositeModifier(modifiers: Iterable[VMod])                   extends VMod
final case class StreamModifier(subscription: Observer[VMod] => Cancelable)     extends VMod
final case class ChildCommandsModifier(commands: Observable[Seq[ChildCommand]]) extends VMod
final case class CancelableModifier(subscription: () => Cancelable)             extends VMod
final case class SyncEffectModifier(unsafeRun: () => VMod)                      extends VMod
final case class ErrorModifier(error: Throwable)                                extends VMod
final case class StringVNode(text: String)                                      extends VMod

final case class ConfiguredModifier(modifier: VMod, configure: RenderConfig => RenderConfig) extends VMod

sealed trait VNode extends VMod {
  @inline final def apply(args: VMod*): VNode = append(args: _*)
  def append(args: VMod*): VNode
  def prepend(args: VMod*): VNode

  @inline final def thunk(key: Key.Value)(arguments: Any*)(renderFn: => VMod): VNode =
    ThunkVNode(this, key, arguments.toJSArray, () => renderFn)
  @inline final def thunkConditional(key: Key.Value)(shouldRender: Boolean)(renderFn: => VMod): VNode =
    ConditionalVNode(this, key, shouldRender, () => renderFn)
  @inline final def thunkStatic(key: Key.Value)(renderFn: => VMod): VNode =
    thunkConditional(key)(false)(renderFn)
}
object VNode {
  private val emptyModifierArray        = js.Array[VMod]()
  @inline def html(name: String): VNode = HtmlVNode(name, emptyModifierArray)
  @inline def svg(name: String): VNode  = SvgVNode(name, emptyModifierArray)

  @inline def raiseError(error: Throwable): VNode = dsl.div(VMod.raiseError(error))

  @inline def fromEither(modifier: Either[Throwable, VNode]): VNode    = modifier.fold(raiseError(_), identity)
  @inline def evalEither(modifier: => Either[Throwable, VNode]): VNode = SyncEffectVNode(() => fromEither(modifier))

  @inline def eval(body: => VNode): VNode = SyncEffectVNode(() => body)

  @inline def effect[F[_]: RunSyncEffect](body: F[VNode]): VNode = evalEither(RunSyncEffect[F].unsafeRun(body))

  implicit object subscriptionOwner extends SubscriptionOwner[VNode] {
    @inline def own(owner: VNode)(subscription: () => Cancelable): VNode =
      owner.append(VMod.managedEval(subscription()))
  }

  implicit object syncEmbed extends SyncEmbed[VNode] {
    @inline def delay(body: => VNode): VNode = VNode.eval(body)
  }
}

sealed trait BasicVNode extends VNode {
  def nodeType: String
  def modifiers: js.Array[VMod]
}
@inline final case class ThunkVNode(
  baseNode: VNode,
  key: Key.Value,
  arguments: js.Array[Any],
  renderFn: () => VMod,
) extends VNode {
  @inline def append(args: VMod*): VNode  = copy(baseNode = baseNode(args: _*))
  @inline def prepend(args: VMod*): VNode = copy(baseNode = baseNode.prepend(args: _*))
}
@inline final case class ConditionalVNode(
  baseNode: VNode,
  key: Key.Value,
  shouldRender: Boolean,
  renderFn: () => VMod,
) extends VNode {
  @inline def append(args: VMod*): VNode  = copy(baseNode = baseNode(args: _*))
  @inline def prepend(args: VMod*): VNode = copy(baseNode = baseNode.prepend(args: _*))
}
@inline final case class HtmlVNode(nodeType: String, modifiers: js.Array[VMod]) extends BasicVNode {
  @inline def append(args: VMod*): VNode  = copy(modifiers = appendSeq(modifiers, args))
  @inline def prepend(args: VMod*): VNode = copy(modifiers = prependSeq(modifiers, args))
}
@inline final case class SvgVNode(nodeType: String, modifiers: js.Array[VMod]) extends BasicVNode {
  @inline def append(args: VMod*): VNode  = copy(modifiers = appendSeq(modifiers, args))
  @inline def prepend(args: VMod*): VNode = copy(modifiers = prependSeq(modifiers, args))
}
@inline final case class SyncEffectVNode(unsafeRun: () => VNode) extends VNode {
  @inline def append(args: VMod*): VNode  = SyncEffectVNode(() => unsafeRun().append(args))
  @inline def prepend(args: VMod*): VNode = SyncEffectVNode(() => unsafeRun().prepend(args))
}
