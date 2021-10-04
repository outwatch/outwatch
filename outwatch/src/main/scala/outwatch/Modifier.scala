package outwatch

import cats._

import outwatch.helpers.{VModifierBooleanOps, BasicAttrBuilder, PropBuilder, BasicStyleBuilder}
import outwatch.helpers.NativeHelpers._

import colibri._
import colibri.effect.RunSyncEffect

import snabbdom.{DataObject, VNodeProxy}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import org.scalajs.dom

sealed trait VModifierM[-Env] {
  def append[R](args: VModifierM[R]*): VModifierM[Env with R]
  def prepend[R](args: VModifierM[R]*): VModifierM[Env with R]
  def provide(env: Env): VModifierM[Any]
  def provideSome[R](map: R => Env): VModifierM[R]
}

sealed trait VModifierMOps {
  @inline final def empty: VModifier = EmptyModifier

  @inline final def apply(): VModifier = empty

  @inline final def ifTrue(condition: Boolean): VModifierBooleanOps = new VModifierBooleanOps(condition)
  @inline final def ifNot(condition: Boolean): VModifierBooleanOps = new VModifierBooleanOps(!condition)

  @inline final def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString : Attr.Value) = new BasicAttrBuilder[T](key, convert)
  @inline final def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropBuilder[T](key, convert)
  @inline final def style[T](key: String) = new BasicStyleBuilder[T](key)

  @inline final def managed[F[_] : RunSyncEffect, T : CanCancel](subscription: F[T]): VModifier = managedFunction(() => RunSyncEffect[F].unsafeRun(subscription))

  @inline final def managedFunction[T : CanCancel](subscription: () => T): VModifier = CancelableModifier(() => Cancelable.lift(subscription()))

  object managedElement {
    def apply[T : CanCancel](subscription: dom.Element => T): VModifier = VModifier.delay {
      var lastSub: js.UndefOr[T] = js.undefined
      VModifier(
        DomMountHook(proxy => proxy.elm.foreach(elm => lastSub = subscription(elm))),
        DomUnmountHook(_ => lastSub.foreach(CanCancel[T].cancel))
      )
    }

    @inline def asHtml[T : CanCancel](subscription: dom.html.Element => T): VModifier = apply(elem => subscription(elem.asInstanceOf[dom.html.Element]))

    @inline def asSvg[T : CanCancel](subscription: dom.svg.Element => T): VModifier = apply(elem => subscription(elem.asInstanceOf[dom.svg.Element]))
  }
}

object VModifierM extends VModifierMOps {

  @inline def apply[Env, T : Render[Env, *]](t: T): VModifierM[Env] = Render[Env, T].render(t)

  @inline def apply[Env](modifier: VModifierM[Env], modifier2: VModifierM[Env]): VModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2))

  @inline def apply[Env](modifier: VModifierM[Env], modifier2: VModifierM[Env], modifier3: VModifierM[Env]): VModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3))

  @inline def apply[Env](modifier: VModifierM[Env], modifier2: VModifierM[Env], modifier3: VModifierM[Env], modifier4: VModifierM[Env]): VModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4))

  @inline def apply[Env](modifier: VModifierM[Env], modifier2: VModifierM[Env], modifier3: VModifierM[Env], modifier4: VModifierM[Env], modifier5: VModifierM[Env]): VModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5))

  @inline def apply[Env](modifier: VModifierM[Env], modifier2: VModifierM[Env], modifier3: VModifierM[Env], modifier4: VModifierM[Env], modifier5: VModifierM[Env], modifier6: VModifierM[Env]): VModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6))

  @inline def apply[Env](modifier: VModifierM[Env], modifier2: VModifierM[Env], modifier3: VModifierM[Env], modifier4: VModifierM[Env], modifier5: VModifierM[Env], modifier6: VModifierM[Env], modifier7: VModifierM[Env], modifiers: VModifierM[Env]*): VModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, CompositeModifier(modifiers)))

  @inline def composite[Env](modifiers: Iterable[VModifierM[Env]]): VModifierM[Env] = CompositeModifier[Env](modifiers.toJSArray)

  @inline def delay[Env](modifier: => VModifierM[Env]): VModifierM[Env] = accessM[Env](_ => modifier)

  @inline def access[Env](modifier: Env => VModifier): VModifierM[Env] = AccessEnvModifier[Env](modifier)
  @inline def accessM[Env] = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R](modifier: Env => VModifierM[R]): VModifierM[Env with R] = access(env => modifier(env).provide(env))
  }

  implicit object monoidk extends MonoidK[VModifierM] {
    @inline def empty[Env]: VModifierM[Env] = VModifierM.empty
    @inline def combineK[Env](x: VModifierM[Env], y: VModifierM[Env]): VModifierM[Env] = VModifierM[Env](x, y)
  }

  @inline implicit def monoid[Env]: Monoid[VModifierM[Env]] = new VModifierMonoid[Env]
  @inline class VModifierMonoid[Env] extends Monoid[VModifierM[Env]] {
    @inline def empty: VModifierM[Env] = VModifierM.empty
    @inline def combine(x: VModifierM[Env], y: VModifierM[Env]): VModifierM[Env] = VModifierM[Env](x, y)
    // @inline override def combineAll(x: Iterable[VModifierM[Env]]): VModifierM[Env] = VModifierM.composite[Env](x)
  }

  implicit object contravariant extends Contravariant[VModifierM] {
    def contramap[A, B](fa: VModifierM[A])(f: B => A): VModifierM[B] = fa.provideSome(f)
  }

  @inline implicit def subscriptionOwner[Env]: SubscriptionOwner[VModifierM[Env]] = new VModifierSubscriptionOwner[Env]
  @inline class VModifierSubscriptionOwner[Env] extends SubscriptionOwner[VModifierM[Env]] {
    @inline def own(owner: VModifierM[Env])(subscription: () => Cancelable): VModifierM[Env] = VModifierM(owner, VModifier.managedFunction(subscription))
  }

  @inline implicit def renderToModifier[Env, T : Render[Env, *]](value: T): VModifierM[Env] = Render[Env, T].render(value)
}

object VModifier extends VModifierMOps {
  @inline def apply[T : Render[Any, *]](t: T): VModifier = Render[Any, T].render(t)

  @inline def apply(modifier: VModifier, modifier2: VModifier): VModifier =
    VModifierM(modifier, modifier2)

  @inline def apply(modifier: VModifier, modifier2: VModifier, modifier3: VModifier): VModifier =
    VModifierM(modifier, modifier2, modifier3)

  @inline def apply(modifier: VModifier, modifier2: VModifier, modifier3: VModifier, modifier4: VModifier): VModifier =
    VModifierM(modifier, modifier2, modifier3, modifier4)

  @inline def apply(modifier: VModifier, modifier2: VModifier, modifier3: VModifier, modifier4: VModifier, modifier5: VModifier): VModifier =
    VModifierM(modifier, modifier2, modifier3, modifier4, modifier5)

  @inline def apply(modifier: VModifier, modifier2: VModifier, modifier3: VModifier, modifier4: VModifier, modifier5: VModifier, modifier6: VModifier): VModifier =
    VModifierM(modifier, modifier2, modifier3, modifier4, modifier5, modifier6)

  @inline def apply(modifier: VModifier, modifier2: VModifier, modifier3: VModifier, modifier4: VModifier, modifier5: VModifier, modifier6: VModifier, modifier7: VModifier, modifiers: VModifier*): VModifier =
    VModifierM(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, modifiers: _*)

  @inline def composite(modifiers: Iterable[VModifier]): VModifier = VModifierM.composite(modifiers)

  @inline def delay(modifier: => VModifier): VModifier = VModifierM.delay(modifier)
}

sealed trait DefaultModifier[-Env] extends VModifierM[Env] {
  final def append[R](args: VModifierM[R]*): VModifierM[Env with R] = VModifierM(this, VModifierM.composite(args))
  final def prepend[R](args: VModifierM[R]*): VModifierM[Env with R] = VModifierM(VModifierM.composite(args), this)
  final def provide(env: Env): VModifier = ProvidedEnvModifier(this, env)
  final def provideSome[R](map: R => Env): VModifierM[R] = AccessEnvModifier[R](env => provide(map(env)))
}

sealed trait StaticModifier extends DefaultModifier[Any]

final case class VNodeProxyNode(proxy: VNodeProxy) extends StaticModifier

final case class Key(value: Key.Value) extends StaticModifier
object Key {
  type Value = DataObject.KeyValue
}

final case class Emitter(eventType: String, trigger: js.Function1[dom.Event, Unit]) extends StaticModifier

sealed trait Attr extends StaticModifier
object Attr {
  type Value = DataObject.AttrValue
}
final case class BasicAttr(title: String, value: Attr.Value) extends Attr
final case class AccumAttr(title: String, value: Attr.Value, accum: (Attr.Value, Attr.Value)=> Attr.Value) extends Attr

final case class Prop(title: String, value: Prop.Value) extends StaticModifier
object Prop {
  type Value = DataObject.PropValue
}

sealed trait Style extends StaticModifier
final case class AccumStyle(title: String, value: String, accum: (String, String) => String) extends Style
final case class BasicStyle(title: String, value: String) extends Style
final case class DelayedStyle(title: String, value: String) extends Style
final case class RemoveStyle(title: String, value: String) extends Style
final case class DestroyStyle(title: String, value: String) extends Style

sealed trait SnabbdomHook extends StaticModifier
final case class InitHook(trigger: js.Function1[VNodeProxy, Unit]) extends SnabbdomHook
final case class InsertHook(trigger: js.Function1[VNodeProxy, Unit]) extends SnabbdomHook
final case class PrePatchHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends SnabbdomHook
final case class UpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends SnabbdomHook
final case class PostPatchHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends SnabbdomHook
final case class DestroyHook(trigger: js.Function1[VNodeProxy, Unit]) extends SnabbdomHook

sealed trait DomHook extends StaticModifier
final case class DomMountHook(trigger: js.Function1[VNodeProxy, Unit]) extends DomHook
final case class DomUnmountHook(trigger: js.Function1[VNodeProxy, Unit]) extends DomHook
final case class DomUpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends DomHook
final case class DomPreUpdateHook(trigger: js.Function2[VNodeProxy, VNodeProxy, Unit]) extends DomHook

final case class NextModifier(modifier: StaticModifier) extends StaticModifier

case object EmptyModifier extends DefaultModifier[Any]
final case class CancelableModifier(subscription: () => Cancelable) extends DefaultModifier[Any]
final case class StringVNode(text: String) extends DefaultModifier[Any]
final case class ProvidedEnvModifier[Env](modifier: VModifierM[Env], env: Env) extends DefaultModifier[Any]
final case class AccessEnvModifier[-Env](modifier: Env => VModifier) extends DefaultModifier[Env]
final case class CompositeModifier[-Env](modifiers: Iterable[VModifierM[Env]]) extends DefaultModifier[Env]
final case class StreamModifier[-Env](subscription: Observer[VModifierM[Env]] => Cancelable) extends DefaultModifier[Env]

sealed trait VNodeM[-Env] extends VModifierM[Env] {
  def apply[R](args: VModifierM[R]*): VNodeM[Env with R]
  def append[R](args: VModifierM[R]*): VNodeM[Env with R]
  def prepend[R](args: VModifierM[R]*): VNodeM[Env with R]
  def provide(env: Env): VNodeM[Any]
  def provideSome[R](map: R => Env): VNodeM[R]
}
sealed trait VNodeMOps {
  @inline final def html(name: String): HtmlVNode = BasicNamespaceVNodeM(name, js.Array[VModifier](), VNodeNamespace.Html)
  @inline final def svg(name: String): SvgVNode = BasicNamespaceVNodeM(name, js.Array[VModifier](), VNodeNamespace.Svg)
}
object VNodeM extends VNodeMOps {
  @inline def delay[Env](modifier: => VNodeM[Env]): VNodeM[Env] = accessM[Env](_ => modifier)
  @inline def access[Env](node: Env => VNode): VNodeM[Env] = new AccessEnvVNodeM[Env](node)
  @inline def accessM[Env] = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R](node: Env => VNodeM[R]): VNodeM[Env with R] = access(env => node(env).provide(env))
  }

  @inline implicit def subscriptionOwner[Env]: SubscriptionOwner[VNodeM[Env]] = new VNodeSubscriptionOwner[Env]
  @inline class VNodeSubscriptionOwner[Env] extends SubscriptionOwner[VNodeM[Env]] {
    @inline def own(owner: VNodeM[Env])(subscription: () => Cancelable): VNodeM[Env] = owner.append(VModifier.managedFunction(subscription))
  }
}
object VNode extends VNodeMOps {
  @inline def delay(node: => VNode): VNode = VNodeM.delay[Any](node)
}

@inline final case class AccessEnvVNodeM[-Env](node: Env => VNode) extends VNodeM[Env] {
  def provide(env: Env): AccessEnvVNodeM[Any] = copy(node = _ => node(env))
  def provideSome[R](map: R => Env): AccessEnvVNodeM[R] = copy(node = r => node(map(r)))
  def apply[R](args: VModifierM[R]*): AccessEnvVNodeM[Env with R] = copy(node = env => node(env).apply(VModifierM.composite(args).provide(env)))
  def append[R](args: VModifierM[R]*): AccessEnvVNodeM[Env with R] = copy(node = env => node(env).append(VModifierM.composite(args).provide(env)))
  def prepend[R](args: VModifierM[R]*): AccessEnvVNodeM[Env with R] = copy(node = env => node(env).prepend(VModifierM.composite(args).provide(env)))
}

@inline final case class ThunkVNodeM[-Env](baseNode: BasicVNodeM[Env], key: Key.Value, condition: VNodeThunkCondition, renderFn: () => VModifierM[Env]) extends VNodeM[Env] {
  def provide(env: Env): ThunkVNodeM[Any] = copy(baseNode = baseNode.provide(env), renderFn = () => renderFn().provide(env))
  def provideSome[R](map: R => Env): ThunkVNodeM[R] = copy(baseNode = baseNode.provideSome(map), renderFn = () => renderFn().provideSome(map))
  def apply[R](args: VModifierM[R]*): VNodeM[Env with R] = copy(baseNode = baseNode.apply(args: _*))
  def append[R](args: VModifierM[R]*): ThunkVNodeM[Env with R] = copy(baseNode = baseNode.append(args: _*))
  def prepend[R](args: VModifierM[R]*): ThunkVNodeM[Env with R] = copy(baseNode = baseNode.prepend(args: _*))
}

@inline final case class BasicNamespaceVNodeM[+N <: VNodeNamespace, -Env](nodeType: String, modifiers: js.Array[_ <: VModifierM[Env]], namespace: N) extends VNodeM[Env] {
  def provide(env: Env): BasicNamespaceVNodeM[N, Any] = copy(modifiers = js.Array(CompositeModifier(modifiers).provide(env)))
  def provideSome[R](map: R => Env): BasicNamespaceVNodeM[N, R] = copy(modifiers = js.Array(CompositeModifier(modifiers).provideSome(map)))
  def apply[R](args: VModifierM[R]*): BasicNamespaceVNodeM[N, Env with R] = copy(modifiers = appendSeq(modifiers, args))
  def append[R](args: VModifierM[R]*): BasicNamespaceVNodeM[N, Env with R] = copy(modifiers = appendSeq(modifiers, args))
  def prepend[R](args: VModifierM[R]*): BasicNamespaceVNodeM[N, Env with R] = copy(modifiers = prependSeq(modifiers, args))
}
object BasicNamespaceVNodeM {
  @inline implicit class BasicVNodeMOps[Env](val self: BasicNamespaceVNodeM[VNodeNamespace, Env]) extends AnyVal {
    @inline def thunk[R](key: Key.Value)(arguments: Any*)(renderFn: => VModifierM[R]): ThunkVNodeM[Env with R] = ThunkVNodeM[Env with R](self, key, VNodeThunkCondition.Compare(arguments.toJSArray), () => renderFn)
    @inline def thunkConditional[R](key: Key.Value)(shouldRender: Boolean)(renderFn: => VModifierM[R]): ThunkVNodeM[Env with R] = ThunkVNodeM[Env with R](self, key, VNodeThunkCondition.Check(shouldRender), () => renderFn)
    @inline def thunkStatic[R](key: Key.Value)(renderFn: => VModifierM[R]): ThunkVNodeM[Env with R] = thunkConditional(key)(false)(renderFn)
  }
}

sealed trait VNodeNamespace
object VNodeNamespace {
  case object Html extends VNodeNamespace
  case object Svg extends VNodeNamespace
}
sealed trait VNodeThunkCondition
object VNodeThunkCondition {
  case class Check(shouldRender: Boolean) extends VNodeThunkCondition
  case class Compare(arguments: js.Array[Any]) extends VNodeThunkCondition
}
