package outwatch.reactive

trait Sink[-F[_]] {
  def onNext[A](sink: F[A])(value: A): Unit
  def onError[A](sink: F[A])(error: Throwable): Unit
}
object Sink {
  @inline def apply[F[_]](implicit sink: Sink[F]): Sink[F] = sink
}

trait LiftSink[+F[_]] {
  def lift[G[_] : Sink, A](sink: G[A]): F[A]
}
object LiftSink {
  @inline def apply[F[_]](implicit sink: LiftSink[F]): LiftSink[F] = sink
}

