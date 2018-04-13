package outwatch

import cats.effect.Effect

//import monix.execution.Ack.Continue
//import monix.execution.Scheduler


trait SideEffectsFactory[F[+_]] extends SinkFactory[F] {
  implicit val effectF: Effect[F]

//  trait SideEffects {
//    def sideEffect[T](f: T => Unit)(implicit s: Scheduler): Sink[T] = Sink.create[T] { e => f(e); Continue }(s). unsafeRunSync()
//
//    def sideEffect[S, T](f: (S, T) => Unit)(implicit s: Scheduler): Sink[(S, T)] = Sink.create[(S, T)] { e => f(e._1, e._2); Continue }(s).unsafeRunSync()
//
//    def sideEffect(f: => Unit)(implicit s: Scheduler): Sink[Any] = Sink.create[Any] { e => f; Continue }(s).unsafeRunSync()
//  }

}
