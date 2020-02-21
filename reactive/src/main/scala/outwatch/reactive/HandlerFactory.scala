package outwatch.reactive

import cats.effect.{Sync, SyncIO}
import colibri._

@inline final class HandlerFactory[H[_] : CreateSubject] {
  // Create a Handler that keeps the last emitted value as State, typically a BehaviourSubject or ReplaySubject

  @inline def create[T]: SyncIO[H[T]] = SyncIO(unsafe[T])
  @inline def create[T](seed: T): SyncIO[H[T]] = SyncIO(unsafe[T](seed))

  @inline def createF[F[_]] = new CreatePartiallyApplied[F]
  @inline def createF[F[_], T](implicit F: Sync[F]): F[H[T]] = F.delay(unsafe[T])
  @inline def createF[F[_], T](seed: T)(implicit F: Sync[F]): F[H[T]] = F.delay(unsafe[T](seed))

  @inline def unsafe[T]: H[T] = CreateSubject[H].replay[T]
  @inline def unsafe[T](seed: T): H[T] = CreateSubject[H].behavior[T](seed)

  object publish {
    // Create a Handler that just publish to all subscribers but does not keep the latest value as State, typically a PublishSubject

    @inline def create[T]: SyncIO[H[T]] = SyncIO(unsafe[T])
    @inline def createF[F[_], T](implicit F: Sync[F]): F[H[T]] = F.delay(unsafe[T])
    @inline def unsafe[T]: H[T] = CreateSubject[H].publish[T]
  }

  @inline final class CreatePartiallyApplied[F[_]] {
    @inline def apply[T](seed: T)(implicit F: Sync[F]): F[H[T]] = createF[F, T](seed)
  }
}

@inline final class ProHandlerFactory[H[_,_] : CreateProSubject] {
  // Create a ProHandler that has different type parameters for the Observer[I] part and the Observable[O] part

  @inline def apply[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): H[I, O] = CreateProSubject[H].from(sink, source)
}
