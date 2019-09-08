package outwatch

import com.raquo.domtypes.generic.keys
import outwatch.dom.helpers.BasicStyleBuilder

package object dom extends ManagedSubscriptions {

  //TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.cssName)
}
