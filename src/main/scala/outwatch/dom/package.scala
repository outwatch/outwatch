package outwatch

import cats.Applicative

package object dom extends Implicits with ManagedSubscriptions {

  type VNodeF[F[+_]] = F[VTree[F]]
  type VDomModifierF[F[+_]] = F[Modifier]

  object VDomModifierF {
    import cats.instances.list._
    import cats.syntax.all._

    def empty[F[+_]: Applicative]: VDomModifierF[F] = Applicative[F].pure(EmptyModifier)

    def apply[F[+_]: Applicative](modifiers: VDomModifierF[F]*): VDomModifierF[F] =
      modifiers.toList.sequence.map(CompositeModifier)
  }

  type Observable[+A] = monix.reactive.Observable[A]
  val Observable = monix.reactive.Observable

  type Sink[-A] = outwatch.Sink[A]
  val Sink = outwatch.Sink

  type Pipe[-I, +O] = outwatch.Pipe[I, O]
  val Pipe = outwatch.Pipe

  type Handler[T] = outwatch.Handler[T]
  val Handler = outwatch.Handler
}
