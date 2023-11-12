import com.raquo.domtypes.generic.keys
import outwatch.helpers.BasicStyleBuilder

package object outwatch extends definitions.ManagedHelpers {
  type EmitterBuilder[+O, +R] = EmitterBuilderExecution[O, R, EmitterBuilderExecution.Execution]

  type VModifier = VModifierM[Any]
  val VModifier = VModifierM
  type VNode = VNodeM[Any]
  val VNode = VNodeM
  type BasicNamespaceVNode[N <: VNodeNamespace] = BasicNamespaceVNodeM[N, Any]
  type BasicVNodeM[-Env]                        = BasicNamespaceVNodeM[_ <: VNodeNamespace, Env]
  type BasicVNode                               = BasicVNodeM[Any]
  type HtmlVNodeM[-Env]                         = BasicNamespaceVNodeM[VNodeNamespace.Html.type, Env]
  type SvgVNodeM[-Env]                          = BasicNamespaceVNodeM[VNodeNamespace.Svg.type, Env]
  type HtmlVNode                                = HtmlVNodeM[Any]
  type SvgVNode                                 = SvgVNodeM[Any]
  type AccessEnvVNode                           = AccessEnvVNodeM[Any]
  type ThunkVNode                               = ThunkVNodeM[Any]

  @deprecated("Use VModifier instead", "")
  type VDomModifier = VModifier
  @deprecated("Use VModifier instead", "")
  val VDomModifier = VModifier
  @deprecated("Use StaticVModifier instead", "")
  type StaticVDomModifier = StaticVModifier

  @deprecated("Use Outwatch instead", "")
  val OutWatch = Outwatch

  // TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] =
    new BasicStyleBuilder[T](style.name)
}
