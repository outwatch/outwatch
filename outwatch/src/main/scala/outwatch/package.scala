import com.raquo.domtypes.generic.keys
import outwatch.helpers.BasicStyleBuilder

import cats._
import colibri._
import colibri.effect.RunSyncEffect

package object outwatch {
  type EmitterBuilderExecution[+O, +R <: Modifier, +Exec <: EmitterBuilder.Execution] = REmitterBuilderExecution[Any, O, R, Exec]
  type REmitterBuilder[-Env, +O, +R <: RModifier[Env]] = REmitterBuilderExecution[Env, O, R, EmitterBuilder.Execution]
  type EmitterBuilder[+O, +R <: Modifier] = REmitterBuilder[Any, O, R]
  // type REmitterBuilderModifier[-Env, +O] = REmitterBuilder[Env, O, RModifier[Env]]
  // type EmitterBuilderModifier[+O] = REmitterBuilderModifier[Any, O]
  // type REmitterBuilderNode[-Env, +O] = REmitterBuilder[Env, O, RVNode[Env]]
  // type EmitterBuilderNode[+O] = REmitterBuilderNode[Any, O]

  type Modifier = RModifier[Any]
  type VNode = RVNode[Any]
  // val VNode = RVNode
  type BasicVNode = RBasicVNode[Any]
  // val BasicVNode = RBasicVNode
  type ConditionalVNode = RConditionalVNode[Any]
  val ConditionalVNode = RConditionalVNode
  type ThunkVNode = RThunkVNode[Any]
  val ThunkVNode = RThunkVNode
  type HtmlVNode = RHtmlVNode[Any]
  val HtmlVNode = RHtmlVNode
  type SvgVNode = RSvgVNode[Any]
  val SvgVNode = RSvgVNode
  type CompositeModifier = RCompositeModifier[Any]
  val CompositeModifier = RCompositeModifier
  type StreamModifier = RStreamModifier[Any]
  val StreamModifier = RStreamModifier
  type SyncEffectModifier = RSyncEffectModifier[Any]
  val SyncEffectModifier = RSyncEffectModifier

  @deprecated("use Modifier instead", "1.0.0")
  type VDomModifier = Modifier
  @deprecated("use Modifier instead", "1.0.0")
  val VDomModifier = Modifier

  @deprecated("use Modifier.managed instead", "1.0.0")
  @inline def managed[F[_] : RunSyncEffect, T : CanCancel](subscription: F[T]): Modifier = Modifier.managed(subscription)
  @deprecated("use Modifier.managed instead", "1.0.0")
  def managed[F[_] : RunSyncEffect : Applicative : Functor, T : CanCancel : Monoid](sub1: F[T], sub2: F[T], subscriptions: F[T]*): Modifier = Modifier.managed(sub1, sub2, subscriptions: _*)
  @deprecated("use Modifier.managedFunction instead", "1.0.0")
  @inline def managedFunction[T : CanCancel](subscription: () => T): Modifier = Modifier.managedFunction(subscription)
  @deprecated("use Modifier.managedElement instead", "1.0.0")
  val managedElement = Modifier.managedElement

  //TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.cssName)
}
