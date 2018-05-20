package outwatch.dom

import cats.effect.IO
import cats.syntax.apply._
import com.raquo.domtypes.generic.keys
import outwatch.AsVDomModifier
import outwatch.dom.helpers.BasicStyleBuilder
import monix.reactive.Observable
import monix.execution.Scheduler

trait Implicits {

  implicit def asVDomModifier[T](value: T)(implicit vm: AsVDomModifier[T]): VDomModifier = vm.asVDomModifier(value)

  implicit class ioVTreeMerge(vnode: VNode) {
    def apply(args: VDomModifier*): VNode = vnode.flatMap(_.apply(args: _*))
    def thunk[T](renderFn: T => VDomModifier, argumentStream: Observable[T])(implicit scheduler: Scheduler): VDomModifier = vnode.flatMap(_.thunk(renderFn, argumentStream))
  }

  implicit def StyleIsBuilder[T](style: keys.Style[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.cssName)

  private[outwatch] implicit class SeqIOSequence[T](args: Seq[IO[T]]) {
    def sequence: IO[Seq[T]] = args.foldRight(IO.pure(List.empty[T]))((a, l) => a.map2(l)(_ :: _))
  }
}
