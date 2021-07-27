package outwatch

import cats._

import outwatch.helpers.{ModifierBooleanOps, BasicAttrBuilder, PropBuilder, BasicStyleBuilder}
import outwatch.helpers.NativeHelpers._

import colibri._
import colibri.effect.RunSyncEffect

import snabbdom.{DataObject, VNodeProxy}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import org.scalajs.dom

sealed trait ModifierM[-Env] {
  def append[R](args: ModifierM[R]*): ModifierM[Env with R]
  def prepend[R](args: ModifierM[R]*): ModifierM[Env with R]
  def provide(env: Env): ModifierM[Any]
  def provideSome[R](map: R => Env): ModifierM[R]
}

sealed trait ModifierMOps {
  @inline final def empty: Modifier = EmptyModifier

  @inline final def apply(): Modifier = empty

  @inline final def ifTrue(condition: Boolean): ModifierBooleanOps = new ModifierBooleanOps(condition)
  @inline final def ifNot(condition: Boolean): ModifierBooleanOps = new ModifierBooleanOps(!condition)

  @inline final def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString : Attr.Value) = new BasicAttrBuilder[T](key, convert)
  @inline final def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropBuilder[T](key, convert)
  @inline final def style[T](key: String) = new BasicStyleBuilder[T](key)

  @inline final def managed[F[_] : RunSyncEffect, T : CanCancel](subscription: F[T]): Modifier = managedFunction(() => RunSyncEffect[F].unsafeRun(subscription))

  @inline final def managedFunction[T : CanCancel](subscription: () => T): Modifier = CancelableModifier(() => Cancelable.lift(subscription()))

  object managedElement {
    def apply[T : CanCancel](subscription: dom.Element => T): Modifier = Modifier.delay {
      var lastSub: js.UndefOr[T] = js.undefined
      Modifier(
        DomMountHook(proxy => proxy.elm.foreach(elm => lastSub = subscription(elm))),
        DomUnmountHook(_ => lastSub.foreach(CanCancel[T].cancel))
      )
    }

    @inline def asHtml[T : CanCancel](subscription: dom.html.Element => T): Modifier = apply(elem => subscription(elem.asInstanceOf[dom.html.Element]))

    @inline def asSvg[T : CanCancel](subscription: dom.svg.Element => T): Modifier = apply(elem => subscription(elem.asInstanceOf[dom.svg.Element]))
  }
}

object ModifierM extends ModifierMOps {

  @inline def apply[Env, T : Render[Env, *]](t: T): ModifierM[Env] = Render[Env, T].render(t)

  @inline def apply[Env](modifier: ModifierM[Env], modifier2: ModifierM[Env]): ModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2))

  @inline def apply[Env](modifier: ModifierM[Env], modifier2: ModifierM[Env], modifier3: ModifierM[Env]): ModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3))

  @inline def apply[Env](modifier: ModifierM[Env], modifier2: ModifierM[Env], modifier3: ModifierM[Env], modifier4: ModifierM[Env]): ModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4))

  @inline def apply[Env](modifier: ModifierM[Env], modifier2: ModifierM[Env], modifier3: ModifierM[Env], modifier4: ModifierM[Env], modifier5: ModifierM[Env]): ModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5))

  @inline def apply[Env](modifier: ModifierM[Env], modifier2: ModifierM[Env], modifier3: ModifierM[Env], modifier4: ModifierM[Env], modifier5: ModifierM[Env], modifier6: ModifierM[Env]): ModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6))

  @inline def apply[Env](modifier: ModifierM[Env], modifier2: ModifierM[Env], modifier3: ModifierM[Env], modifier4: ModifierM[Env], modifier5: ModifierM[Env], modifier6: ModifierM[Env], modifier7: ModifierM[Env], modifiers: ModifierM[Env]*): ModifierM[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, CompositeModifier(modifiers)))

  @inline def composite[Env](modifiers: Iterable[ModifierM[Env]]): ModifierM[Env] = CompositeModifier[Env](modifiers.toJSArray)

  @inline def delay[Env](modifier: => ModifierM[Env]): ModifierM[Env] = accessM[Env](_ => modifier)

  @inline def access[Env](modifier: Env => Modifier): ModifierM[Env] = AccessEnvModifier[Env](modifier)
  @inline def accessM[Env] = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R](modifier: Env => ModifierM[R]): ModifierM[Env with R] = access(env => modifier(env).provide(env))
  }

  implicit object monoidk extends MonoidK[ModifierM] {
    @inline def empty[Env]: ModifierM[Env] = ModifierM.empty
    @inline def combineK[Env](x: ModifierM[Env], y: ModifierM[Env]): ModifierM[Env] = ModifierM[Env](x, y)
  }

  @inline implicit def monoid[Env]: Monoid[ModifierM[Env]] = new ModifierMonoid[Env]
  @inline class ModifierMonoid[Env] extends Monoid[ModifierM[Env]] {
    @inline def empty: ModifierM[Env] = ModifierM.empty
    @inline def combine(x: ModifierM[Env], y: ModifierM[Env]): ModifierM[Env] = ModifierM[Env](x, y)
    // @inline override def combineAll(x: Iterable[ModifierM[Env]]): ModifierM[Env] = ModifierM.composite[Env](x)
  }

  implicit object contravariant extends Contravariant[ModifierM] {
    def contramap[A, B](fa: ModifierM[A])(f: B => A): ModifierM[B] = fa.provideSome(f)
  }

  @inline implicit def subscriptionOwner[Env]: SubscriptionOwner[ModifierM[Env]] = new ModifierSubscriptionOwner[Env]
  @inline class ModifierSubscriptionOwner[Env] extends SubscriptionOwner[ModifierM[Env]] {
    @inline def own(owner: ModifierM[Env])(subscription: () => Cancelable): ModifierM[Env] = ModifierM(owner, Modifier.managedFunction(subscription))
  }

  @inline implicit def renderToModifier[Env, T : Render[Env, *]](value: T): ModifierM[Env] = Render[Env, T].render(value)
}

object Modifier extends ModifierMOps {
  @inline def apply[T : Render[Any, *]](t: T): Modifier = Render[Any, T].render(t)

  @inline def apply(modifier: Modifier, modifier2: Modifier): Modifier =
    ModifierM(modifier, modifier2)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier): Modifier =
    ModifierM(modifier, modifier2, modifier3)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier, modifier4: Modifier): Modifier =
    ModifierM(modifier, modifier2, modifier3, modifier4)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier, modifier4: Modifier, modifier5: Modifier): Modifier =
    ModifierM(modifier, modifier2, modifier3, modifier4, modifier5)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier, modifier4: Modifier, modifier5: Modifier, modifier6: Modifier): Modifier =
    ModifierM(modifier, modifier2, modifier3, modifier4, modifier5, modifier6)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier, modifier4: Modifier, modifier5: Modifier, modifier6: Modifier, modifier7: Modifier, modifiers: Modifier*): Modifier =
    ModifierM(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, modifiers: _*)

  @inline def composite(modifiers: Iterable[Modifier]): Modifier = ModifierM.composite(modifiers)

  @inline def delay(modifier: => Modifier): Modifier = ModifierM.delay(modifier)
}

sealed trait DefaultModifier[-Env] extends ModifierM[Env] {
  final def append[R](args: ModifierM[R]*): ModifierM[Env with R] = ModifierM(this, ModifierM.composite(args))
  final def prepend[R](args: ModifierM[R]*): ModifierM[Env with R] = ModifierM(ModifierM.composite(args), this)
  final def provide(env: Env): Modifier = ProvidedEnvModifier(this, env)
  final def provideSome[R](map: R => Env): ModifierM[R] = AccessEnvModifier[R](env => provide(map(env)))
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
final case class ProvidedEnvModifier[Env](modifier: ModifierM[Env], env: Env) extends DefaultModifier[Any]
final case class AccessEnvModifier[-Env](modifier: Env => Modifier) extends DefaultModifier[Env]
final case class CompositeModifier[-Env](modifiers: Iterable[ModifierM[Env]]) extends DefaultModifier[Env]
final case class StreamModifier[-Env](subscription: Observer[ModifierM[Env]] => Cancelable) extends DefaultModifier[Env]

sealed trait VNodeM[-Env] extends ModifierM[Env] {
  def apply[R](args: ModifierM[R]*): VNodeM[Env with R]
  def append[R](args: ModifierM[R]*): VNodeM[Env with R]
  def prepend[R](args: ModifierM[R]*): VNodeM[Env with R]
  def provide(env: Env): VNodeM[Any]
  def provideSome[R](map: R => Env): VNodeM[R]
}
sealed trait VNodeMOps {
  @inline final def html(name: String): HtmlVNode = BasicNamespaceVNodeM(name, js.Array[Modifier](), VNodeNamespace.Html)
  @inline final def svg(name: String): SvgVNode = BasicNamespaceVNodeM(name, js.Array[Modifier](), VNodeNamespace.Svg)
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
    @inline def own(owner: VNodeM[Env])(subscription: () => Cancelable): VNodeM[Env] = owner.append(Modifier.managedFunction(subscription))
  }
}
object VNode extends VNodeMOps {
  @inline def delay(node: => VNode): VNode = VNodeM.delay[Any](node)
}

@inline final case class AccessEnvVNodeM[-Env](node: Env => VNode) extends VNodeM[Env] {
  def provide(env: Env): AccessEnvVNodeM[Any] = copy(node = _ => node(env))
  def provideSome[R](map: R => Env): AccessEnvVNodeM[R] = copy(node = r => node(map(r)))
  def apply[R](args: ModifierM[R]*): AccessEnvVNodeM[Env with R] = copy(node = env => node(env).apply(ModifierM.composite(args).provide(env)))
  def append[R](args: ModifierM[R]*): AccessEnvVNodeM[Env with R] = copy(node = env => node(env).append(ModifierM.composite(args).provide(env)))
  def prepend[R](args: ModifierM[R]*): AccessEnvVNodeM[Env with R] = copy(node = env => node(env).prepend(ModifierM.composite(args).provide(env)))
}

@inline final case class ThunkVNodeM[-Env](baseNode: BasicVNodeM[Env], key: Key.Value, condition: VNodeThunkCondition, renderFn: () => ModifierM[Env]) extends VNodeM[Env] {
  def provide(env: Env): ThunkVNodeM[Any] = copy(baseNode = baseNode.provide(env), renderFn = () => renderFn().provide(env))
  def provideSome[R](map: R => Env): ThunkVNodeM[R] = copy(baseNode = baseNode.provideSome(map), renderFn = () => renderFn().provideSome(map))
  def apply[R](args: ModifierM[R]*): VNodeM[Env with R] = copy(baseNode = baseNode.apply(args: _*))
  def append[R](args: ModifierM[R]*): ThunkVNodeM[Env with R] = copy(baseNode = baseNode.append(args: _*))
  def prepend[R](args: ModifierM[R]*): ThunkVNodeM[Env with R] = copy(baseNode = baseNode.prepend(args: _*))
}

@inline final case class BasicNamespaceVNodeM[+N <: VNodeNamespace, -Env](nodeType: String, modifiers: js.Array[_ <: ModifierM[Env]], namespace: N) extends VNodeM[Env] {
  def provide(env: Env): BasicNamespaceVNodeM[N, Any] = copy(modifiers = js.Array(CompositeModifier(modifiers).provide(env)))
  def provideSome[R](map: R => Env): BasicNamespaceVNodeM[N, R] = copy(modifiers = js.Array(CompositeModifier(modifiers).provideSome(map)))
  def apply[R](args: ModifierM[R]*): BasicNamespaceVNodeM[N, Env with R] = copy(modifiers = appendSeq(modifiers, args))
  def append[R](args: ModifierM[R]*): BasicNamespaceVNodeM[N, Env with R] = copy(modifiers = appendSeq(modifiers, args))
  def prepend[R](args: ModifierM[R]*): BasicNamespaceVNodeM[N, Env with R] = copy(modifiers = prependSeq(modifiers, args))
}
object BasicNamespaceVNodeM {
  @inline implicit class BasicVNodeMOps[Env](val self: BasicNamespaceVNodeM[VNodeNamespace, Env]) extends AnyVal {
    @inline def thunk[R](key: Key.Value)(arguments: Any*)(renderFn: => ModifierM[R]): ThunkVNodeM[Env with R] = ThunkVNodeM[Env with R](self, key, VNodeThunkCondition.Compare(arguments.toJSArray), () => renderFn)
    @inline def thunkConditional[R](key: Key.Value)(shouldRender: Boolean)(renderFn: => ModifierM[R]): ThunkVNodeM[Env with R] = ThunkVNodeM[Env with R](self, key, VNodeThunkCondition.Check(shouldRender), () => renderFn)
    @inline def thunkStatic[R](key: Key.Value)(renderFn: => ModifierM[R]): ThunkVNodeM[Env with R] = thunkConditional(key)(false)(renderFn)
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
