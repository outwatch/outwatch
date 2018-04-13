package outwatch.dom

import cats.effect.Effect
import cats.syntax.all._
import com.raquo.domtypes.generic.keys
import outwatch.AsVDomModifierFactory
import outwatch.dom.helpers.BuilderFactory

trait Implicits[F[+_]] extends AsVDomModifierFactory[F] with BuilderFactory[F] {
  implicit val effectF: Effect[F]

  implicit def asVDomModifier[T](value: T)(implicit vm: AsVDomModifier[T]): VDomModifier =
    vm.asVDomModifier(value)

  implicit class ioVTreeMerge(vnode: VNode) {
    def apply(args: VDomModifier*): VNode = vnode.flatMap(_.apply(args: _*))
  }

  implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] =
    new BasicStyleBuilder[T](style.cssName)

}
