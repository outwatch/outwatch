import com.raquo.domtypes.generic.keys
import outwatch.helpers.BasicStyleBuilder

package object outwatch extends ManagedSubscriptions {
  type EmitterBuilder[+O, +R] = EmitterBuilderExecution[O, R, EmitterBuilder.Execution]

  type Modifier = RModifier[Any]
  @inline def Modifier = RModifier
  type VNode = RVNode[Any]
  @inline def VNode = RVNode
  type BasicVNode = RBasicVNode[Any]
  // @inline def BasicVNode = RBasicVNode
  type ConditionalVNode = RConditionalVNode[Any]
  @inline def ConditionalVNode = RConditionalVNode
  type ThunkVNode = RThunkVNode[Any]
  @inline def ThunkVNode = RThunkVNode
  type HtmlVNode = RHtmlVNode[Any]
  @inline def HtmlVNode = RHtmlVNode
  type SvgVNode = RSvgVNode[Any]
  @inline def SvgVNode = RSvgVNode
  type CompositeModifier = RCompositeModifier[Any]
  @inline def CompositeModifier = RCompositeModifier
  type StreamModifier = RStreamModifier[Any]
  @inline def StreamModifier = RStreamModifier
  type SyncEffectModifier = RSyncEffectModifier[Any]
  @inline def SyncEffectModifier = RSyncEffectModifier

  @deprecated("use Modifier instead", "1.0.0")
  type VDomModifier = Modifier
  @deprecated("use Modifier instead", "1.0.0")
  @inline def VDomModifier = Modifier

  //TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.cssName)
}
