package outwatch.reactive

trait CreateHandler[+F[_]] {
  def create[A]: F[A]
  def create[A](seed: A): F[A]
}
object CreateHandler {
  @inline def apply[F[_]](implicit handler: CreateHandler[F]): CreateHandler[F] = handler
}

trait CreateProHandler[+F[_,_]] {
  def create[I,O](f: I => O): F[I,O]
  def create[I,O](seed: I)(f: I => O): F[I,O]
  def from[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): F[I, O]
}
object CreateProHandler {
  @inline def apply[F[_,_]](implicit handler: CreateProHandler[F]): CreateProHandler[F] = handler
}
