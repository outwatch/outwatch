import com.raquo.domtypes.generic.keys
import outwatch.helpers.BasicStyleBuilder

package object outwatch extends ManagedSubscriptions {
  // type EmitterBuilder[+O, +R] = EmitterBuilderExecution[O, R, EmitterBuilder.Execution]

  //TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.name)
}
