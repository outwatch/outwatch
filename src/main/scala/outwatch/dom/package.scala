package outwatch

import cats.effect.IO

package object dom extends Attributes with Tags with HandlerFactories {

  type VNode = IO[VTree]
  type VDomModifier = IO[VDomModifier_]
  object VDomModifier {
    val empty = IO.pure(EmptyModifier)
  }

  type Observable[+A] = rxscalajs.Observable[A]
  val Observable = rxscalajs.Observable

  type Sink[-A] = outwatch.Sink[A]
  val Sink = outwatch.Sink

  type Pipe[-I, +O] = outwatch.Pipe[I, O]
  val Pipe = outwatch.Pipe

  type Handler[T] = outwatch.Handler[T]
  val Handler = outwatch.Handler

  implicit def valueModifier[T](value: T)(implicit mr: ValueModifier[T]): VDomModifier = mr.asModifier(value)

  implicit def optionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse VDomModifier.empty

  implicit def compositeModifier(modifiers: Seq[VDomModifier]): VDomModifier = IO.pure(CompositeModifier(modifiers))

  implicit class ioVTreeMerge(vnode: VNode) {
    def apply(args: VDomModifier*): VNode = {
      vnode.flatMap(vnode_ => vnode_(args:_*))
    }
  }
}
