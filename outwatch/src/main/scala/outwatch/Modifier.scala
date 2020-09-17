package outwatch

import cats._
import cats.implicits._

import outwatch.helpers.ModifierBooleanOps
import outwatch.helpers.NativeHelpers._

import colibri._
import colibri.effect.RunSyncEffect

import snabbdom.{DataObject, VNodeProxy}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import org.scalajs.dom

sealed trait RModifier[-Env] {
  type Self[-R] <: RModifier[R]

  def provide(env: Env): Self[Any]
  def provideMap[R](map: R => Env): Self[R]

  def append[R](args: RModifier[R]*): Self[Env with R]
  def prepend[R](args: RModifier[R]*): Self[Env with R]
}

trait RModifierOps {
  @inline def empty: RModifier[Any] = EmptyModifier

  @inline def apply(): RModifier[Any] = empty

  @inline def ifTrue(condition: Boolean): ModifierBooleanOps = new ModifierBooleanOps(condition)
  @inline def ifNot(condition: Boolean): ModifierBooleanOps = new ModifierBooleanOps(!condition)

  @inline def managed[F[_] : RunSyncEffect, T : CanCancel](subscription: F[T]): Modifier = managedFunction(() => RunSyncEffect[F].unsafeRun(subscription))

  def managed[F[_] : RunSyncEffect : Applicative : Functor, T : CanCancel : Monoid](sub1: F[T], sub2: F[T], subscriptions: F[T]*): Modifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map[T](subs => Monoid[T].combineAll(subs))
    managed(composite)
  }

  @inline def managedFunction[T : CanCancel](subscription: () => T): Modifier = CancelableModifier(() => Cancelable.lift(subscription()))

  object managedElement {
    def apply[T : CanCancel](subscription: dom.Element => T): Modifier = Modifier.delay {
      var lastSub: js.UndefOr[T] = js.undefined
      Modifier(
        dsl.onDomMount foreach { elem => lastSub = subscription(elem) },
        dsl.onDomUnmount foreach { lastSub.foreach(CanCancel[T].cancel) }
      )
    }

    @inline def asHtml[T : CanCancel](subscription: dom.html.Element => T): Modifier = apply(elem => subscription(elem.asInstanceOf[dom.html.Element]))

    @inline def asSvg[T : CanCancel](subscription: dom.svg.Element => T): Modifier = apply(elem => subscription(elem.asInstanceOf[dom.svg.Element]))
  }
}

object RModifier extends RModifierOps {

  @inline def apply[Env, T : Render[Env, ?]](t: T): RModifier[Env] = Render[Env, T].render(t)

  @inline def apply[Env](modifier: RModifier[Env], modifier2: RModifier[Env]): RModifier[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2))

  @inline def apply[Env](modifier: RModifier[Env], modifier2: RModifier[Env], modifier3: RModifier[Env]): RModifier[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3))

  @inline def apply[Env](modifier: RModifier[Env], modifier2: RModifier[Env], modifier3: RModifier[Env], modifier4: RModifier[Env]): RModifier[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4))

  @inline def apply[Env](modifier: RModifier[Env], modifier2: RModifier[Env], modifier3: RModifier[Env], modifier4: RModifier[Env], modifier5: RModifier[Env]): RModifier[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5))

  @inline def apply[Env](modifier: RModifier[Env], modifier2: RModifier[Env], modifier3: RModifier[Env], modifier4: RModifier[Env], modifier5: RModifier[Env], modifier6: RModifier[Env]): RModifier[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6))

  @inline def apply[Env](modifier: RModifier[Env], modifier2: RModifier[Env], modifier3: RModifier[Env], modifier4: RModifier[Env], modifier5: RModifier[Env], modifier6: RModifier[Env], modifier7: RModifier[Env], modifiers: RModifier[Env]*): RModifier[Env] =
    CompositeModifier[Env](js.Array(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, CompositeModifier(modifiers)))

  @inline def composite[Env](modifiers: Iterable[RModifier[Env]]): RModifier[Env] = CompositeModifier[Env](modifiers.toJSArray)

  @inline def delay[Env, T : Render[Env, ?]](modifier: => T): RModifier[Env] = SyncEffectModifier[Env](() => RModifier(modifier))

  @inline def access[Env](modifier: Env => RModifier[Any]): RModifier[Env] = EnvModifier[Env](modifier)

  implicit object monoidk extends MonoidK[RModifier] {
    @inline def empty[Env]: RModifier[Env] = RModifier.empty
    @inline def combineK[Env](x: RModifier[Env], y: RModifier[Env]): RModifier[Env] = RModifier[Env](x, y)
  }

  implicit object contravariant extends Contravariant[RModifier] {
    def contramap[A, B](fa: RModifier[A])(f: B => A): RModifier[B] = fa.provideMap(f)
  }

  @inline implicit def renderToModifier[Env, T : Render[Env, ?]](value: T): RModifier[Env] = Render[Env, T].render(value)
}

object Modifier extends RModifierOps {
  @inline def apply[T : Render[Any, ?]](t: T): Modifier = Render[Any, T].render(t)

  @inline def apply(modifier: Modifier, modifier2: Modifier): Modifier =
    RModifier(modifier, modifier2)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier): Modifier =
    RModifier(modifier, modifier2, modifier3)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier, modifier4: Modifier): Modifier =
    RModifier(modifier, modifier2, modifier3, modifier4)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier, modifier4: Modifier, modifier5: Modifier): Modifier =
    RModifier(modifier, modifier2, modifier3, modifier4, modifier5)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier, modifier4: Modifier, modifier5: Modifier, modifier6: Modifier): Modifier =
    RModifier(modifier, modifier2, modifier3, modifier4, modifier5, modifier6)

  @inline def apply(modifier: Modifier, modifier2: Modifier, modifier3: Modifier, modifier4: Modifier, modifier5: Modifier, modifier6: Modifier, modifier7: Modifier, modifiers: Modifier*): Modifier =
    RModifier(modifier, modifier2, modifier3, modifier4, modifier5, modifier6, modifier7, modifiers)

  @inline def composite(modifiers: Iterable[Modifier]): Modifier = RModifier.composite(modifiers)

  @inline def delay[T : Render[Any, ?]](modifier: => T): Modifier = RModifier.delay(modifier)
}

sealed trait DefaultModifier[-Env] extends RModifier[Env] {
  type Self[-R] = RModifier[R]

  final def append[R](args: RModifier[R]*): RModifier[Env with R] = RModifier(this, RModifier.composite(args))
  final def prepend[R](args: RModifier[R]*): RModifier[Env with R] = RModifier(RModifier.composite(args), this)

  final def provide(env: Env): RModifier[Any] = ProvidedModifier(this, env)
  final def provideMap[R](map: R => Env): RModifier[R] = EnvModifier[R](env => provide(map(env)))
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
final case class ProvidedModifier[Env](modifier: RModifier[Env], env: Env) extends DefaultModifier[Any]
final case class EnvModifier[Env](modifier: Env => RModifier[Any]) extends DefaultModifier[Env]
final case class CompositeModifier[Env](modifiers: Iterable[RModifier[Env]]) extends DefaultModifier[Env]
final case class StreamModifier[Env](subscription: Observer[RModifier[Env]] => Cancelable) extends DefaultModifier[Env]
final case class SyncEffectModifier[Env](unsafeRun: () => RModifier[Env]) extends DefaultModifier[Env]

sealed trait RVNode[-Env] extends RModifier[Env] {
  type Self[-R] <: RVNode[R]

  def append[R](args: RModifier[R]*): Self[Env with R]
  def prepend[R](args: RModifier[R]*): Self[Env with R]
  @inline final def apply[R](args: RModifier[R]*): Self[Env with R] = append[R](args: _*)

  final def provide(env: Env): Self[Any] = ???
  final def provideMap[R](map: R => Env): Self[R] = ???
}

sealed trait RBasicVNode[-Env] extends RVNode[Env] {
  type Self[-R] <: RBasicVNode[R]

  def nodeType: String
  def modifiers: js.Array[_ <: RModifier[Env]]
}
object RBasicVNode {
  @inline implicit class RBasicVNodeOps[Env](val self: RBasicVNode[Env]) extends AnyVal {
    @inline def thunk(key: Key.Value)(arguments: Any*)(renderFn: => RModifier[Env]): RThunkVNode[Env] = RThunkVNode(self, key, arguments.toJSArray, () => renderFn)
    @inline def thunkConditional(key: Key.Value)(shouldRender: Boolean)(renderFn: => RModifier[Env]): RConditionalVNode[Env] = RConditionalVNode(self, key, shouldRender, () => renderFn)
    @inline def thunkStatic(key: Key.Value)(renderFn: => RModifier[Env]): RConditionalVNode[Env] = thunkConditional(key)(false)(renderFn)
  }
}

sealed trait RExtendVNode[-Env] extends RVNode[Env] {
  type Self[-R] <: RExtendVNode[R]
}

// final case class ProvidedVNode(runWith: ProvidedModifierConsumer[RVNode] => Unit) extends RVNode[Any]
// final case class EnvVNode[Env](modifier: Env => RVNode[Any]) extends RVNode[Env]

@inline final case class RThunkVNode[-Env](baseNode: RBasicVNode[Env], key: Key.Value, arguments: js.Array[Any], renderFn: () => RModifier[Env]) extends RExtendVNode[Env] {
  type Self[-R] = RThunkVNode[R]

  def append[R](args: RModifier[R]*): RThunkVNode[Env with R] = copy(baseNode = baseNode.append(args: _*))
  def prepend[R](args: RModifier[R]*): RThunkVNode[Env with R] = copy(baseNode = baseNode.prepend(args :_*))
}
@inline final case class RConditionalVNode[-Env](baseNode: RBasicVNode[Env], key: Key.Value, shouldRender: Boolean, renderFn: () => RModifier[Env]) extends RExtendVNode[Env] {
  type Self[-R] = RConditionalVNode[R]

  def append[R](args: RModifier[R]*): RConditionalVNode[Env with R] = copy(baseNode = baseNode.append(args: _*))
  def prepend[R](args: RModifier[R]*): RConditionalVNode[Env with R] = copy(baseNode = baseNode.prepend(args :_*))
}
@inline final case class RHtmlVNode[-Env](nodeType: String, modifiers: js.Array[_ <: RModifier[Env]]) extends RBasicVNode[Env] {
  type Self[-R] = RHtmlVNode[R]

  def append[R](args: RModifier[R]*): RHtmlVNode[Env with R] = copy(modifiers = appendSeq(modifiers, args))
  def prepend[R](args: RModifier[R]*): RHtmlVNode[Env with R] = copy(modifiers = prependSeq(modifiers, args))
}
@inline final case class RSvgVNode[-Env](nodeType: String, modifiers: js.Array[_ <: RModifier[Env]]) extends RBasicVNode[Env] {
  type Self[-R] = RSvgVNode[R]

  def append[R](args: RModifier[R]*): RSvgVNode[Env with R] = copy(modifiers = appendSeq(modifiers, args))
  def prepend[R](args: RModifier[R]*): RSvgVNode[Env with R] = copy(modifiers = prependSeq(modifiers, args))
}
