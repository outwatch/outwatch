package outwatch.dom

import cats.Applicative
import cats.effect.Effect
import cats.syntax.all._
import com.raquo.domtypes.generic.keys
import outwatch.AsVDomModifier
import outwatch.dom.helpers.BasicStyleBuilder

trait Implicits[F[+_]] {
  implicit val effectF: Effect[F]
  implicit def applicativeF: Applicative[F] = effectF

  implicit def asVDomModifier[T](value: T)(implicit vm: AsVDomModifier[F, T]): VDomModifierF[F] =
    vm.asVDomModifier(value)

  implicit class ioVTreeMerge(vnode: VNodeF[F]) {
    def apply(args: VDomModifierF[F]*): VNodeF[F] = vnode.flatMap(_.apply(args: _*))
  }

  implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[F, T] =
    new BasicStyleBuilder[F, T](style.cssName)

}
