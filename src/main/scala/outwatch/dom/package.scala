package outwatch

import cats.effect.IO
import cats.syntax.apply._

package object dom extends Attributes with Tags {

  type VNode = IO[VTree]
  type VDomModifier = IO[VDomModifier_]
  object VDomModifier {
    val empty = IO.pure(EmptyModifier)
  }

  type Observable[+A] = monix.reactive.Observable[A]
  val Observable = monix.reactive.Observable

  type Sink[-A] = outwatch.Sink[A]
  val Sink = outwatch.Sink

  type Pipe[-I, +O] = outwatch.Pipe[I, O]
  val Pipe = outwatch.Pipe

  type Handler[T] = outwatch.Handler[T]
  val Handler = outwatch.Handler

  implicit def valueModifier[T](value: T)(implicit mr: ValueModifier[T]): VDomModifier = mr.asModifier(value)

  implicit def optionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse VDomModifier.empty

  implicit def compositeModifier(modifiers: Seq[VDomModifier]): VDomModifier = modifiers.sequence.map(CompositeModifier)

  implicit class ioVTreeMerge(vnode: VNode) {
    def apply(args: VDomModifier*): VNode = for {
      vnode <- vnode
      args <- args.sequence
    } yield vnode(args: _*)
  }

  private[outwatch] implicit class SeqIOSequence[T](args: Seq[IO[T]]) {
    def sequence: IO[Seq[T]] = args.foldRight(IO.pure(List.empty[T]))((a, l) => a.map2(l)(_ :: _))
  }
}
