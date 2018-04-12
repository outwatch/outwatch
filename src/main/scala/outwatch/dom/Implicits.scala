package outwatch.dom

import cats.effect.Effect
import cats.syntax.all._
import com.raquo.domtypes.generic.keys
import outwatch.AsVDomModifierInstances
import outwatch.dom.helpers.BuilderFactory

trait Implicits[F[+_]] extends AsVDomModifierInstances[F] with BuilderFactory[F] {
  implicit val effectF: Effect[F]

  implicit def asVDomModifier[T](value: T)(implicit vm: AsVDomModifier[T]): VDomModifierF =
    vm.asVDomModifier(value)

  implicit class ioVTreeMerge(vnode: VNodeF) {
    def apply(args: VDomModifierF*): VNodeF = vnode.flatMap(_.apply(args: _*))
  }

  implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] =
    new BasicStyleBuilder[T](style.cssName)

}
