package outwatch.dom

import cats.effect.{Effect, Sync}
import com.raquo.domtypes.generic.keys
import outwatch.AsVDomModifier
import outwatch.dom.helpers.BasicStyleBuilder

trait Implicits {

  implicit def asVDomModifier[F[+_]: Effect, T](value: T)(implicit vm: AsVDomModifier[F, T]): VDomModifierF[F] =
    vm.asVDomModifier(value)

  implicit class ioVTreeMerge[F[+_]: Sync](vnode: VNodeF[F]) {
    import cats.implicits._
    def apply(args: VDomModifierF[F]*): VNodeF[F] = vnode.flatMap(_.apply(args: _*))
  }

  implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.cssName)

}
