import com.raquo.domtypes.generic.keys
import outwatch.helpers.BasicStyleBuilder

package object outwatch extends ManagedSubscriptions {
  type EmitterBuilderExecution[+O, +R <: Modifier, +Exec <: EmitterBuilder.Execution] = REmitterBuilderExecution[Any, O, R, Exec]
  type REmitterBuilder[-Env, +O, +R <: RModifier[Env]] = REmitterBuilderExecution[Env, O, R, EmitterBuilder.Execution]
  type EmitterBuilder[+O, +R <: Modifier] = REmitterBuilder[Any, O, R]
  // type REmitterBuilderModifier[-Env, +O] = REmitterBuilder[Env, O, RModifier[Env]]
  // type EmitterBuilderModifier[+O] = REmitterBuilderModifier[Any, O]
  // type REmitterBuilderNode[-Env, +O] = REmitterBuilder[Env, O, RVNode[Env]]
  // type EmitterBuilderNode[+O] = REmitterBuilderNode[Any, O]

  type Modifier = RModifier[Any]
  val Modifier = RModifier
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

  //TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.cssName)
}
