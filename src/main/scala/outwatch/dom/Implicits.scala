package outwatch.dom

import cats.effect.IO
import com.raquo.domtypes.generic.keys
import outwatch.ValueModifier
import cats.syntax.apply._
import outwatch.dom.helpers.StyleBuilder

trait Implicits {

  implicit def valueModifier[T](value: T)(implicit mr: ValueModifier[T]): VDomModifier = mr.asModifier(value)

  implicit def optionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse VDomModifier.empty

  implicit def compositeModifier(modifiers: Seq[VDomModifier]): VDomModifier = VDomModifier.apply(modifiers : _*)

  implicit class ioVTreeMerge(vnode: VNode) {
    def apply(args: VDomModifier*): VNode = for {
      vnode <- vnode
      args <- args.sequence
    } yield vnode(args: _*)
  }

  implicit def StyleIsBuilder[T](style: keys.Style[T]): StyleBuilder[T] = new StyleBuilder[T](style.cssName)

  private[outwatch] implicit class SeqIOSequence[T](args: Seq[IO[T]]) {
    def sequence: IO[Seq[T]] = args.foldRight(IO.pure(List.empty[T]))((a, l) => a.map2(l)(_ :: _))
  }
}
