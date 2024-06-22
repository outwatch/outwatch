import com.raquo.domtypes.generic.keys

package object outwatch {
  // TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): AttrBuilder.ToBasicStyle[T] =
    new AttrBuilder.ToBasicStyle[T](style.name)
}
