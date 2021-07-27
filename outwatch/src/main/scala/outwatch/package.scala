import com.raquo.domtypes.generic.keys
import outwatch.helpers.BasicStyleBuilder

package object outwatch extends definitions.ManagedHelpers {
  type EmitterBuilder[+O, +R] = EmitterBuilderExec[O, R, EmitterBuilderExec.Execution]

  type Modifier = ModifierM[Any]
  type VNode = VNodeM[Any]
  type BasicNamespaceVNode[N <: VNodeNamespace] = BasicNamespaceVNodeM[N, Any]
  type BasicVNodeM[-Env] = BasicNamespaceVNodeM[_ <: VNodeNamespace, Env]
  type BasicVNode = BasicVNodeM[Any]
  type HtmlVNodeM[-Env] = BasicNamespaceVNodeM[VNodeNamespace.Html.type, Env]
  type SvgVNodeM[-Env] = BasicNamespaceVNodeM[VNodeNamespace.Svg.type, Env]
  type HtmlVNode = HtmlVNodeM[Any]
  type SvgVNode = SvgVNodeM[Any]
  type AccessEnvVNode = AccessEnvVNodeM[Any]
  type ThunkVNode = ThunkVNodeM[Any]

  @deprecated("use Modifier instead", "1.0.0")
  type VDomModifier = Modifier
  @deprecated("use Modifier instead", "1.0.0")
  val VDomModifier = Modifier

  @deprecated("use Outwatch instead", "1.0.0")
  val OutWatch = Outwatch

  //TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.name)
}
