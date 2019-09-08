package outwatch.reactive

trait Source[-F[_]] {
  def subscribe[G[_] : Sink, A](source: F[A])(sink: G[_ >: A]): Subscription
}
object Source {
  @inline def apply[F[_]](implicit source: Source[F]): Source[F] = source
}

trait LiftSource[+F[_]] {
  def lift[G[_] : Source, A](source: G[A]): F[A]
}
object LiftSource {
  @inline def apply[F[_]](implicit source: LiftSource[F]): LiftSource[F] = source
}
