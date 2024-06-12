package outwatch

import outwatch.helpers.NativeHelpers._
import snabbdom.{DataObject, VNodeProxy}
import colibri._
import colibri.effect._
import cats.{Functor, Monoid, MonoidK}
import cats.syntax.functor._
import cats.effect.Sync
import org.scalajs.dom
import outwatch.helpers.ModifierBooleanOps

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

sealed trait VModM[-Env] {
  @inline final def ++[R](other: VModM[R]): VModM[Env with R]     = concat(other)
  @inline final def concat[R](other: VModM[R]): VModM[Env with R] = VMod(this, other)

  def provide(env: Env): VModM[Any]
  def provideSome[R](map: R => Env): VModM[R]
  final def provideSomeF[F[+_]: Functor, R](map: R => F[Env])(implicit render: Render[Any, F[VMod]]): VModM[R] =
    AccessEnvModifier[R](env => render.render(map(env).map(provide(_))))
}

object VModM {
  @inline def empty: VMod = EmptyModifier

  @inline def apply(): VMod = empty

  @inline def apply[Env](t: VModM[Env]): VModM[Env] = t

  @inline def apply[Env](modifier: VModM[Env], modifier2: VModM[Env]): VModM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2))

  @inline def apply[Env](
    modifier: VModM[Env],
    modifier2: VModM[Env],
    modifier3: VModM[Env],
  ): VModM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3))

  @inline def apply[Env](
    modifier: VModM[Env],
    modifier2: VModM[Env],
    modifier3: VModM[Env],
    modifier4: VModM[Env],
  ): VModM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4))

  @inline def apply[Env](
    modifier: VModM[Env],
    modifier2: VModM[Env],
    modifier3: VModM[Env],
    modifier4: VModM[Env],
    modifier5: VModM[Env],
  ): VModM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5))

  @inline def apply[Env](
    modifier: VModM[Env],
    modifier2: VModM[Env],
    modifier3: VModM[Env],
    modifier4: VModM[Env],
    modifier5: VModM[Env],
    modifier6: VModM[Env],
  ): VModM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6))

  @inline def apply[Env](
    modifier: VModM[Env],
    modifier2: VModM[Env],
    modifier3: VModM[Env],
    modifier4: VModM[Env],
    modifier5: VModM[Env],
    modifier6: VModM[Env],
    modifier7: VModM[Env],
    modifiers: VModM[Env]*,
  ): VModM[Env] =
    CompositeModifier[Env](
      js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, CompositeModifier(modifiers)),
    )

  @inline def access[Env](modifier: Env => VMod): VModM[Env] = AccessEnvModifier[Env](modifier)
  @inline def accessM[Env]                                   = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R](modifier: Env => VModM[R]): VModM[Env with R] =
      access(env => modifier(env).provide(env))
  }

  @inline final def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString: Attr.Value) =
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
      VMod[Any](
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

  @inline def fromEither[Env, T: Render[Env, *]](modifier: Either[Throwable, T]): VModM[Env] =
    modifier.fold(raiseError(_), apply(_))
  @inline def evalEither[Env, T: Render[Env, *]](modifier: => Either[Throwable, T]): VModM[Env] =
    SyncEffectModifier(() => fromEither(modifier))
  @deprecated("Use VMod.eval instead", "1.0.0")
  @inline def delay[Env](modifier: => VModM[Env]): VModM[Env]             = eval(modifier)
  @inline def eval[Env](modifier: => VModM[Env]): VModM[Env]              = SyncEffectModifier(() => modifier)
  @inline def composite[Env](modifiers: Iterable[VModM[Env]]): VModM[Env] = CompositeModifier(modifiers.toJSArray)
  @inline def raiseError(error: Throwable): VMod                          = ErrorModifier(error)

  @inline def catchError[Env](modifier: VModM[Env])(recover: Throwable => VMod): VModM[Env] =
    configured(modifier)(_.copy(errorModifier = recover))
  @inline def configured[Env](modifier: VModM[Env])(configure: RenderConfig => RenderConfig): VModM[Env] =
    ConfiguredModifier(modifier, configure)

  @deprecated("Use VMod.when(condition)(...) instead", "")
  @inline def ifTrue(condition: Boolean): ModifierBooleanOps = when(condition)
  @deprecated("Use VMod.whenNot(condition)(...) instead", "")
  @inline def ifNot(condition: Boolean): ModifierBooleanOps   = whenNot(condition)
  @inline def when(condition: Boolean): ModifierBooleanOps    = new ModifierBooleanOps(condition)
  @inline def whenNot(condition: Boolean): ModifierBooleanOps = new ModifierBooleanOps(!condition)

  @inline def toggle[Env](condition: Boolean)(ifTrue: => VModM[Env], ifFalse: => VModM[Env] = VMod.empty): VModM[Env] =
    if (condition) ifTrue else ifFalse

  implicit object monoidk extends MonoidK[VModM] {
    @inline def empty[Env]: VModM[Env]                                  = VModM.empty
    @inline def combineK[Env](x: VModM[Env], y: VModM[Env]): VModM[Env] = VModM[Env](x, y)
  }

  @inline implicit def monoid[Env]: Monoid[VModM[Env]] = new VModMonoid[Env]
  @inline class VModMonoid[Env] extends Monoid[VModM[Env]] {
    @inline def empty: VModM[Env]                                 = VModM.empty
    @inline def combine(x: VModM[Env], y: VModM[Env]): VModM[Env] = VModM[Env](x, y)
    // @inline override def combineAll(x: Iterable[VModM[Env]]): VModM[Env] = VModM.composite[Env](x)
  }

  @inline implicit def subscriptionOwner[Env]: SubscriptionOwner[VModM[Env]] = new VModSubscriptionOwner[Env]
  @inline class VModSubscriptionOwner[Env] extends SubscriptionOwner[VModM[Env]] {
    @inline def own(owner: VModM[Env])(subscription: () => Cancelable): VModM[Env] =
      VModM(owner, VMod.managedEval(subscription()))
  }

  implicit def syncEmbed[Env]: SyncEmbed[VModM[Env]] = new SyncEmbed[VModM[Env]] {
    @inline def delay(body: => VModM[Env]): VModM[Env] = VMod.eval(body)
  }

  @inline implicit def renderToVMod[Env, T: Render[Env, *]](value: T): VModM[Env] = Render[Env, T].render(value)
}

sealed trait DefaultVMod[-Env] extends VModM[Env] {
  final def provide(env: Env): VModM[Any]           = ProvidedEnvModifier(this, env)
  final def provideSome[R](map: R => Env): VModM[R] = AccessEnvModifier[R](env => provide(map(env)))
}

sealed trait StaticVMod extends DefaultVMod[Any]

final case class VNodeProxyNode(proxy: VNodeProxy) extends StaticVMod

final case class Key(value: Key.Value) extends StaticVMod
object Key {
  type Value = DataObject.KeyValue
}

final case class Emitter(eventType: String, trigger: js.Function1[dom.Event, Unit]) extends StaticVMod

sealed trait Attr extends StaticVMod
object Attr {
  type Value = DataObject.AttrValue
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

case object EmptyModifier                                                               extends DefaultVMod[Any]
final case class CompositeModifier[-Env](modifiers: Iterable[VModM[Env]])               extends DefaultVMod[Env]
final case class StreamModifier[-Env](subscription: Observer[VModM[Env]] => Cancelable) extends DefaultVMod[Env]
final case class ChildCommandsModifier(commands: Observable[Seq[ChildCommand]])         extends DefaultVMod[Any]
final case class CancelableModifier(subscription: () => Cancelable)                     extends DefaultVMod[Any]
final case class SyncEffectModifier[-Env](unsafeRun: () => VModM[Env])                  extends DefaultVMod[Env]
final case class ErrorModifier(error: Throwable)                                        extends DefaultVMod[Any]
final case class StringVNode(text: String)                                              extends DefaultVMod[Any]

final case class ProvidedEnvModifier[Env](modifier: VModM[Env], env: Env) extends DefaultVMod[Any]
final case class AccessEnvModifier[-Env](modifier: Env => VMod)           extends DefaultVMod[Env]

final case class ConfiguredModifier[-Env](modifier: VModM[Env], configure: RenderConfig => RenderConfig)
    extends DefaultVMod[Env]

sealed trait VNodeM[-Env] extends VModM[Env] {
  def append[R](args: VModM[R]*): VNodeM[Env with R]
  def prepend[R](args: VModM[R]*): VNodeM[Env with R]

  def provide(env: Env): VNodeM[Any]
  def provideSome[R](map: R => Env): VNodeM[R]

  @inline final def apply[R](args: VModM[R]*): VNodeM[Env with R] = append(args: _*)

  @inline final def thunk(key: Key.Value)(arguments: Any*)(renderFn: => VMod): VNodeM[Env] =
    ThunkVNodeM(this, key, arguments.toJSArray, () => renderFn)
  @inline final def thunkConditional(key: Key.Value)(shouldRender: Boolean)(renderFn: => VMod): VNodeM[Env] =
    ConditionalVNodeM(this, key, shouldRender, () => renderFn)
  @inline final def thunkStatic(key: Key.Value)(renderFn: => VMod): VNodeM[Env] =
    thunkConditional(key)(false)(renderFn)
}
object VNodeM {
  private val emptyModifierArray        = js.Array[VMod]()
  @inline def html(name: String): VNode = HtmlVNodeM(name, emptyModifierArray)
  @inline def svg(name: String): VNode  = SvgVNodeM(name, emptyModifierArray)

  @inline def raiseError(error: Throwable): VNode = dsl.div(VMod.raiseError(error))

  @inline def fromEither[Env](modifier: Either[Throwable, VNodeM[Env]]): VNodeM[Env] =
    modifier.fold(raiseError(_), identity)
  @inline def evalEither[Env](modifier: => Either[Throwable, VNodeM[Env]]): VNodeM[Env] =
    SyncEffectVNodeM(() => fromEither(modifier))

  @inline def eval[Env](body: => VNodeM[Env]): VNodeM[Env] = SyncEffectVNodeM(() => body)

  @inline def access[Env](node: Env => VNode): VNodeM[Env] = new AccessEnvVNodeM[Env](node)
  @inline def accessM[Env]                                 = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R](node: Env => VNodeM[R]): VNodeM[Env with R] = access(env => node(env).provide(env))
  }

  @inline def effect[Env, F[_]: RunSyncEffect](body: F[VNodeM[Env]]): VNodeM[Env] =
    evalEither(RunSyncEffect[F].unsafeRun(body))

  @inline implicit def subscriptionOwner[Env]: SubscriptionOwner[VNodeM[Env]] = new VNodeSubscriptionOwner[Env]
  @inline class VNodeSubscriptionOwner[Env] extends SubscriptionOwner[VNodeM[Env]] {
    @inline def own(owner: VNodeM[Env])(subscription: () => Cancelable): VNodeM[Env] =
      owner.append(VMod.managedEval(subscription()))
  }

  implicit def syncEmbed[Env]: SyncEmbed[VNodeM[Env]] = new SyncEmbed[VNodeM[Env]] {
    @inline def delay(body: => VNodeM[Env]): VNodeM[Env] = VNode.eval(body)
  }
}

sealed trait BasicVNodeM[-Env] extends VNodeM[Env] {
  def nodeType: String
  def modifiers: js.Array[_ <: VModM[Env]]
}

@inline final case class AccessEnvVNodeM[-Env](node: Env => VNode) extends VNodeM[Env] {
  @inline def provide(env: Env): VNodeM[Any]           = copy(node = _ => node(env))
  @inline def provideSome[R](map: R => Env): VNodeM[R] = copy(node = r => node(map(r)))
  @inline def append[R](args: VModM[R]*): VNodeM[Env with R] =
    copy(node = env => node(env).append(VMod.composite(args).provide(env)))
  @inline def prepend[R](args: VModM[R]*): VNodeM[Env with R] =
    copy(node = env => node(env).prepend(VMod.composite(args).provide(env)))
}
@inline final case class ThunkVNodeM[-Env](
  baseNode: VNodeM[Env],
  key: Key.Value,
  arguments: js.Array[Any],
  renderFn: () => VModM[Env],
) extends VNodeM[Env] {
  @inline def provide(env: Env): VNodeM[Any] =
    copy(baseNode = baseNode.provide(env), renderFn = () => renderFn().provide(env))
  @inline def provideSome[R](map: R => Env): VNodeM[R] =
    copy(baseNode = baseNode.provideSome(map), renderFn = () => renderFn().provideSome(map))
  @inline def append[R](args: VModM[R]*): VNodeM[Env with R]  = copy(baseNode = baseNode(args: _*))
  @inline def prepend[R](args: VModM[R]*): VNodeM[Env with R] = copy(baseNode = baseNode.prepend(args: _*))
}
@inline final case class ConditionalVNodeM[-Env](
  baseNode: VNodeM[Env],
  key: Key.Value,
  shouldRender: Boolean,
  renderFn: () => VModM[Env],
) extends VNodeM[Env] {
  @inline def provide(env: Env): VNodeM[Any] =
    copy(baseNode = baseNode.provide(env), renderFn = () => renderFn().provide(env))
  @inline def provideSome[R](map: R => Env): VNodeM[R] =
    copy(baseNode = baseNode.provideSome(map), renderFn = () => renderFn().provideSome(map))
  @inline def append[R](args: VModM[R]*): VNodeM[Env with R]  = copy(baseNode = baseNode(args: _*))
  @inline def prepend[R](args: VModM[R]*): VNodeM[Env with R] = copy(baseNode = baseNode.prepend(args: _*))
}
@inline final case class HtmlVNodeM[-Env](nodeType: String, modifiers: js.Array[_ <: VModM[Env]])
    extends BasicVNodeM[Env] {
  @inline def provide(env: Env): VNodeM[Any] = copy(modifiers = js.Array(CompositeModifier(modifiers).provide(env)))
  @inline def provideSome[R](map: R => Env): VNodeM[R] =
    copy(modifiers = js.Array(CompositeModifier(modifiers).provideSome(map)))
  @inline def append[R](args: VModM[R]*): VNodeM[Env with R] =
    copy(modifiers = appendSeq[VModM, Env, R](modifiers, args))
  @inline def prepend[R](args: VModM[R]*): VNodeM[Env with R] = copy(modifiers = prependSeq(modifiers, args))
}
@inline final case class SvgVNodeM[-Env](nodeType: String, modifiers: js.Array[_ <: VModM[Env]])
    extends BasicVNodeM[Env] {
  @inline def provide(env: Env): VNodeM[Any] = copy(modifiers = js.Array(CompositeModifier(modifiers).provide(env)))
  @inline def provideSome[R](map: R => Env): VNodeM[R] =
    copy(modifiers = js.Array(CompositeModifier(modifiers).provideSome(map)))
  @inline def append[R](args: VModM[R]*): VNodeM[Env with R]  = copy(modifiers = appendSeq(modifiers, args))
  @inline def prepend[R](args: VModM[R]*): VNodeM[Env with R] = copy(modifiers = prependSeq(modifiers, args))
}
@inline final case class SyncEffectVNodeM[-Env](unsafeRun: () => VNodeM[Env]) extends VNodeM[Env] {
  @inline def provide(env: Env): VNodeM[Any]                  = copy(unsafeRun = () => unsafeRun().provide(env))
  @inline def provideSome[R](map: R => Env): VNodeM[R]        = copy(unsafeRun = () => unsafeRun().provideSome(map))
  @inline def append[R](args: VModM[R]*): VNodeM[Env with R]  = copy(unsafeRun = () => unsafeRun().append(args))
  @inline def prepend[R](args: VModM[R]*): VNodeM[Env with R] = copy(unsafeRun = () => unsafeRun().prepend(args))
}
