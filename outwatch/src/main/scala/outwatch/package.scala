import com.raquo.domtypes.generic.keys

package object outwatch extends definitions.ManagedHelpers {
  @deprecated("Use VModifier instead", "")
  type VDomModifier = VModifier
  @deprecated("Use VModifier instead", "")
  val VDomModifier = VModifier
  @deprecated("Use StaticVModifier instead", "")
  type StaticVDomModifier = StaticVModifier

  @deprecated("Use Outwatch instead", "")
  val OutWatch = Outwatch

  // TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): AttrBuilder.ToBasicStyle[T] =
    new AttrBuilder.ToBasicStyle[T](style.name)
}
