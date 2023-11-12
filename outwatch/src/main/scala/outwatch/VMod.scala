package outwatch

import outwatch.helpers.{BasicAttrBuilder, BasicStyleBuilder, PropBuilder, VModBooleanOps}
import outwatch.helpers.NativeHelpers._
import snabbdom.{DataObject, VNodeProxy}

import colibri._
import colibri.effect._
import cats.{Contravariant, Monoid, MonoidK}
import cats.syntax.functor._
import cats.effect.Sync

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

sealed trait VModM[-Env] {
  def append[R](args: VModM[R]*): VModM[Env with R]
  def prepend[R](args: VModM[R]*): VModM[Env with R]
  def provide(env: Env): VModM[Any]
  def provideSome[R](map: R => Env): VModM[R]
}

object VModM {

  @inline final def empty: VMod = EmptyModifier

  @inline final def apply(): VMod = empty

  @inline final def ifTrue(condition: Boolean): VModBooleanOps = new VModBooleanOps(condition)
  @inline final def ifNot(condition: Boolean): VModBooleanOps  = new VModBooleanOps(!condition)

  @inline final def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString: Attr.Value) =
    new BasicAttrBuilder[T](key, convert)
  @inline final def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropBuilder[T](key, convert)
  @inline final def style[T](key: String)                                        = new BasicStyleBuilder[T](key)

  @inline def managed[F[_]: Sync: RunEffect, T: CanCancel](subscription: F[T]): VMod = VMod(
    subscription.map[VMod](cancelable => CancelableModifier(() => Cancelable.lift(cancelable))),
  )

  @inline def managedSubscribe[F[_]: Source, T](source: F[T]): VMod = managedDelay(
    Source[F].unsafeSubscribe(source)(Observer.empty),
  )

  @deprecated("Use managedDelay(subscription) instead", "")
  @inline def managedFunction[T: CanCancel](subscription: () => T): VMod = managedDelay(subscription())
  @inline def managedDelay[T: CanCancel](subscription: => T): VMod =
    CancelableModifier(() => Cancelable.lift(subscription))

  object managedElement {
    def apply[T: CanCancel](subscription: dom.Element => T): VMod = VMod.delay {
      var lastSub: js.UndefOr[T] = js.undefined
      VMod(
        DomMountHook(proxy => proxy.elm.foreach(elm => lastSub = subscription(elm))),
        DomUnmountHook(_ => lastSub.foreach(CanCancel[T].unsafeCancel)),
      )
    }

    @inline def asHtml[T: CanCancel](subscription: dom.html.Element => T): VMod =
      apply(elem => subscription(elem.asInstanceOf[dom.html.Element]))

    @inline def asSvg[T: CanCancel](subscription: dom.svg.Element => T): VMod =
      apply(elem => subscription(elem.asInstanceOf[dom.svg.Element]))
  }

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

  @inline def composite[Env](modifiers: Iterable[VModM[Env]]): VModM[Env] =
    CompositeModifier[Env](modifiers.toJSArray)

  @inline def fromEither[Env, T: Render[Env, *]](modifier: Either[Throwable, T]): VModM[Env] =
    modifier.fold(raiseError(_), apply(_))
  @inline def delayEither[Env, T: Render[Env, *]](modifier: => Either[Throwable, T]): VModM[Env] =
    SyncEffectModifier(() => fromEither(modifier))
  @inline def delay[Env](modifier: => VModM[Env]): VModM[Env] = accessM[Env](_ => modifier)
  @inline def raiseError[T](error: Throwable): VMod                = ErrorModifier(error)

  @inline def access[Env](modifier: Env => VMod): VModM[Env] = AccessEnvModifier[Env](modifier)
  @inline def accessM[Env]                                             = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R](modifier: Env => VModM[R]): VModM[Env with R] =
      access(env => modifier(env).provide(env))
  }

  implicit object monoidk extends MonoidK[VModM] {
    @inline def empty[Env]: VModM[Env]                                            = VModM.empty
    @inline def combineK[Env](x: VModM[Env], y: VModM[Env]): VModM[Env] = VModM[Env](x, y)
  }

  @inline implicit def monoid[Env]: Monoid[VModM[Env]] = new VModMonoid[Env]
  @inline class VModMonoid[Env] extends Monoid[VModM[Env]] {
    @inline def empty: VModM[Env]                                           = VModM.empty
    @inline def combine(x: VModM[Env], y: VModM[Env]): VModM[Env] = VModM[Env](x, y)
    // @inline override def combineAll(x: Iterable[VModM[Env]]): VModM[Env] = VModM.composite[Env](x)
  }

  implicit object contravariant extends Contravariant[VModM] {
    def contramap[A, B](fa: VModM[A])(f: B => A): VModM[B] = fa.provideSome(f)
  }

  @inline implicit def subscriptionOwner[Env]: SubscriptionOwner[VModM[Env]] = new VModSubscriptionOwner[Env]
  @inline class VModSubscriptionOwner[Env] extends SubscriptionOwner[VModM[Env]] {
    @inline def own(owner: VModM[Env])(subscription: () => Cancelable): VModM[Env] =
      VModM(owner, VMod.managedDelay(subscription()))
  }

  @inline implicit def renderToModifier[Env, T: Render[Env, *]](value: T): VModM[Env] =
    Render[Env, T].render(value)
}

sealed trait DefaultModifier[-Env] extends VModM[Env] {
  final def append[R](args: VModM[R]*): VModM[Env with R]  = VModM(this, VModM.composite(args))
  final def prepend[R](args: VModM[R]*): VModM[Env with R] = VModM(VModM.composite(args), this)
  final def provide(env: Env): VMod                             = ProvidedEnvModifier(this, env)
  final def provideSome[R](map: R => Env): VModM[R]             = AccessEnvModifier[R](env => provide(map(env)))
}

sealed trait StaticVMod extends DefaultModifier[Any]

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

case object EmptyModifier                                                       extends DefaultModifier[Any]
final case class ErrorModifier(error: Throwable)                                extends DefaultModifier[Any]
final case class CancelableModifier(subscription: () => Cancelable)             extends DefaultModifier[Any]
final case class StringVNode(text: String)                                      extends DefaultModifier[Any]
final case class ProvidedEnvModifier[Env](modifier: VModM[Env], env: Env)  extends DefaultModifier[Any]
final case class ChildCommandsModifier(commands: Observable[Seq[ChildCommand]]) extends DefaultModifier[Any]
final case class AccessEnvModifier[-Env](modifier: Env => VMod)            extends DefaultModifier[Env]
final case class CompositeModifier[-Env](modifiers: Iterable[VModM[Env]])  extends DefaultModifier[Env]
final case class SyncEffectModifier[-Env](unsafeRun: () => VModM[Env])     extends DefaultModifier[Env]
final case class StreamModifier[-Env](subscription: Observer[VModM[Env]] => Cancelable)
    extends DefaultModifier[Env]

sealed trait VNodeM[-Env] extends VModM[Env] {
  def apply[R](args: VModM[R]*): VNodeM[Env with R]
  def append[R](args: VModM[R]*): VNodeM[Env with R]
  def prepend[R](args: VModM[R]*): VNodeM[Env with R]
  def provide(env: Env): VNodeM[Any]
  def provideSome[R](map: R => Env): VNodeM[R]
}
object VNodeM {
  @inline final def html(name: String): HtmlVNode =
    BasicNamespaceVNodeM(name, js.Array[VMod](), VNodeNamespace.Html)
  @inline final def svg(name: String): SvgVNode = BasicNamespaceVNodeM(name, js.Array[VMod](), VNodeNamespace.Svg)

  @inline def delay[Env](modifier: => VNodeM[Env]): VNodeM[Env] = accessM[Env](_ => modifier)
  @inline def access[Env](node: Env => VNode): VNodeM[Env]      = new AccessEnvVNodeM[Env](node)
  @inline def accessM[Env]                                      = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R](node: Env => VNodeM[R]): VNodeM[Env with R] = access(env => node(env).provide(env))
  }

  @inline implicit def subscriptionOwner[Env]: SubscriptionOwner[VNodeM[Env]] = new VNodeSubscriptionOwner[Env]
  @inline class VNodeSubscriptionOwner[Env] extends SubscriptionOwner[VNodeM[Env]] {
    @inline def own(owner: VNodeM[Env])(subscription: () => Cancelable): VNodeM[Env] =
      owner.append(VMod.managedDelay(subscription()))
  }
}

@inline final case class AccessEnvVNodeM[-Env](node: Env => VNode) extends VNodeM[Env] {
  def provide(env: Env): AccessEnvVNodeM[Any]           = copy(node = _ => node(env))
  def provideSome[R](map: R => Env): AccessEnvVNodeM[R] = copy(node = r => node(map(r)))
  def apply[R](args: VModM[R]*): AccessEnvVNodeM[Env with R] =
    copy(node = env => node(env).apply(VModM.composite(args).provide(env)))
  def append[R](args: VModM[R]*): AccessEnvVNodeM[Env with R] =
    copy(node = env => node(env).append(VModM.composite(args).provide(env)))
  def prepend[R](args: VModM[R]*): AccessEnvVNodeM[Env with R] =
    copy(node = env => node(env).prepend(VModM.composite(args).provide(env)))
}

@inline final case class ThunkVNodeM[-Env](
  baseNode: BasicVNodeM[Env],
  key: Key.Value,
  condition: VNodeThunkCondition,
  renderFn: () => VModM[Env],
) extends VNodeM[Env] {
  def provide(env: Env): ThunkVNodeM[Any] =
    copy(baseNode = baseNode.provide(env), renderFn = () => renderFn().provide(env))
  def provideSome[R](map: R => Env): ThunkVNodeM[R] =
    copy(baseNode = baseNode.provideSome(map), renderFn = () => renderFn().provideSome(map))
  def apply[R](args: VModM[R]*): VNodeM[Env with R]        = copy(baseNode = baseNode.apply(args: _*))
  def append[R](args: VModM[R]*): ThunkVNodeM[Env with R]  = copy(baseNode = baseNode.append(args: _*))
  def prepend[R](args: VModM[R]*): ThunkVNodeM[Env with R] = copy(baseNode = baseNode.prepend(args: _*))
}

@inline final case class BasicNamespaceVNodeM[+N <: VNodeNamespace, -Env](
  nodeType: String,
  modifiers: js.Array[_ <: VModM[Env]],
  namespace: N,
) extends VNodeM[Env] {
  def provide(env: Env): BasicNamespaceVNodeM[N, Any] =
    copy(modifiers = js.Array(CompositeModifier(modifiers).provide(env)))
  def provideSome[R](map: R => Env): BasicNamespaceVNodeM[N, R] =
    copy(modifiers = js.Array(CompositeModifier(modifiers).provideSome(map)))
  def apply[R](args: VModM[R]*): BasicNamespaceVNodeM[N, Env with R] = copy(modifiers = appendSeq(modifiers, args))
  def append[R](args: VModM[R]*): BasicNamespaceVNodeM[N, Env with R] =
    copy(modifiers = appendSeq(modifiers, args))
  def prepend[R](args: VModM[R]*): BasicNamespaceVNodeM[N, Env with R] =
    copy(modifiers = prependSeq(modifiers, args))
  def thunk[R](key: Key.Value)(arguments: Any*)(renderFn: => VModM[R]): ThunkVNodeM[Env with R] =
    ThunkVNodeM[Env with R](this, key, VNodeThunkCondition.Compare(arguments.toJSArray), () => renderFn)
  def thunkConditional[R](key: Key.Value)(shouldRender: Boolean)(renderFn: => VModM[R]): ThunkVNodeM[Env with R] =
    ThunkVNodeM[Env with R](this, key, VNodeThunkCondition.Check(shouldRender), () => renderFn)
  @inline def thunkStatic[R](key: Key.Value)(renderFn: => VModM[R]): ThunkVNodeM[Env with R] =
    thunkConditional(key)(false)(renderFn)
}

sealed trait VNodeNamespace
object VNodeNamespace {
  case object Html extends VNodeNamespace
  case object Svg  extends VNodeNamespace
}
sealed trait VNodeThunkCondition
object VNodeThunkCondition {
  case class Check(shouldRender: Boolean)      extends VNodeThunkCondition
  case class Compare(arguments: js.Array[Any]) extends VNodeThunkCondition
}
