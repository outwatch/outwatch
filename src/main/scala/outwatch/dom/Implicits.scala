package outwatch.dom

import cats.effect.Effect
import cats.syntax.apply._
import com.raquo.domtypes.generic.keys
import outwatch.ValueModifier
import outwatch.dom.helpers.BasicStyleBuilder

trait Implicits {

  implicit def valueModifier[T,F[_]:Effect](value: T)(implicit mr: ValueModifier[T]): VDomModifier = mr.asModifier[F](value)

  implicit def optionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse VDomModifier.empty

  implicit def compositeModifier(modifiers: Seq[VDomModifier]): VDomModifier = VDomModifier.apply(modifiers : _*)

  implicit class ioVTreeMerge(vnode: VNode) {
    def apply(args: VDomModifier*): VNode = for {
      vnode <- vnode
      args <- args.sequence
    } yield vnode(args: _*)
  }

  implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.cssName)

  private[outwatch] implicit class SeqIOSequence[T,F[_]:Effect](args: Seq[F[T]]) {
    def sequence: F[Seq[T]] = args.foldRight(Effect[F].pure(List.empty[T]))((a, l) => a.map2(l)(_ :: _))
  }
}
