import com.raquo.domtypes.generic.keys

package object outwatch extends definitions.ManagedHelpers {

  type VMod = VModM[Any]
  val VMod = VModM
  type VNode = VNodeM[Any]
  val VNode = VNodeM
  type BasicVNode       = BasicVNodeM[Any]
  type HtmlVNode        = HtmlVNodeM[Any]
  type SvgVNode         = SvgVNodeM[Any]
  type AccessEnvVNode   = AccessEnvVNodeM[Any]
  type ThunkVNode       = ThunkVNodeM[Any]
  type ConditionalVNode = ConditionalVNodeM[Any]
  type SyncEffectVNode  = SyncEffectVNodeM[Any]

  @deprecated("Use VMod instead", "")
  type VDomModifier = VMod
  @deprecated("Use VMod instead", "")
  val VDomModifier = VMod
  @deprecated("Use StaticVMod instead", "")
  type StaticVDomModifier = StaticVMod

  @deprecated("Use VMod instead", "")
  type VModifier = VMod
  @deprecated("Use VMod instead", "")
  val VModifier = VMod
  @deprecated("Use StaticVMod instead", "")
  type StaticVModifier = StaticVMod

  @deprecated("Use Outwatch instead", "")
  val OutWatch = Outwatch

  // TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): AttrBuilder.ToBasicStyle[T] =
    new AttrBuilder.ToBasicStyle[T](style.name)
}
