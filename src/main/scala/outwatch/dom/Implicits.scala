package outwatch.dom

import com.raquo.domtypes.generic.keys
import monix.reactive.Observer
import outwatch.AsVDomModifier
import outwatch.dom.helpers.BasicStyleBuilder

trait Implicits {

  implicit def asVDomModifier[T](value: T)(implicit vm: AsVDomModifier[T]): VDomModifier = vm.asVDomModifier(value)

  //TODO: would be better to have typeclass AsObserver on all functions using observer.
  // but if we do this: emitter --> obs will not compile with sideeffects without type parameters
  implicit def asObserver[T, F[_]](value: F[T])(implicit ao: AsObserver[F]): Observer[T] = ao.as(value)

  implicit class ioVTreeMerge(vnode: VNode) {
    def apply(args: VDomModifier*): VNode = vnode.flatMap(_.apply(args: _*))
  }

  implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.cssName)
}
