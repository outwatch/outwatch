package outwatch

import cats.effect.IO
import cats.syntax.apply._
import org.scalajs.dom.Element

package object dom extends Attributes with Tags with HandlerFactories {

  type VTree[Elem <: Element] = IO[VTree_[Elem]]
  type VNode = IO[VTree_[_ <: Element]]
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

  implicit def compositeModifier(modifiers: Seq[VDomModifier]): VDomModifier = modifiers.sequence.map(CompositeModifier)

  implicit class ioVTreeMerge[Elem <: Element](vnode: VTree[Elem]) {
    def apply(args: (TagContext[Elem] => VDomModifier)*): VTree[Elem] = for {
      vnode <- vnode
      args <- IO { args.map(_ andThen (_.unsafeRunSync())) }
    } yield vnode(args: _*)
  }

  private[outwatch] implicit class SeqIOSequence[T](args: Seq[IO[T]]) {
    def sequence: IO[Seq[T]] = args.foldRight(IO.pure(List.empty[T]))((a, l) => a.map2(l)(_ :: _))
  }

  //TODO type clase vdommodifierrender
  implicit def ModifierIsContextualModifier[Elem <: Element, T](mod: T)(implicit conv: T => VDomModifier): TagContext[Elem] => VDomModifier = _ => conv(mod)
}
