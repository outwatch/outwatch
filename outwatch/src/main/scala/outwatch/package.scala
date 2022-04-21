import com.raquo.domtypes.generic.keys
import outwatch.helpers.BasicStyleBuilder

package object outwatch extends definitions.ManagedHelpers {
  type EmitterBuilder[+O, +R] = EmitterBuilderExecution[O, R, EmitterBuilder.Execution]

  @deprecated("Use VModifier instead", "")
  type VDomModifier = VModifier
  @deprecated("Use VModifier instead", "")
  val VDomModifier = VModifier
  @deprecated("Use StaticVModifier instead", "")
  type StaticVDomModifier = StaticVModifier

  @deprecated("Use Outwatch instead", "")
  val OutWatch = Outwatch

  //TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.name)
}
