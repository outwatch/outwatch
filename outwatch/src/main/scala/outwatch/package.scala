import com.raquo.domtypes.generic.keys

package object outwatch extends definitions.ManagedHelpers {
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
