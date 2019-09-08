package outwatch.reactive

import cats.effect.{Sync, SyncIO}

@inline final class HandlerFactory[H[_] : CreateHandler] {

  @inline def create[T]: SyncIO[H[T]] = SyncIO(unsafe[T])
  @inline def create[T](seed: T): SyncIO[H[T]] = SyncIO(unsafe[T](seed))

  @inline def createF[F[_]] = new CreatePartiallyApplied[F]
  @inline def createF[F[_], T](implicit F: Sync[F]): F[H[T]] = F.delay(unsafe[T])
  @inline def createF[F[_], T](seed: T)(implicit F: Sync[F]): F[H[T]] = F.delay(unsafe[T](seed))

  @inline def unsafe[T]: H[T] = CreateHandler[H].create[T]
  @inline def unsafe[T](seed: T): H[T] = CreateHandler[H].create[T](seed)

  @inline final class CreatePartiallyApplied[F[_]] {
    @inline def apply[T](seed: T)(implicit F: Sync[F]): F[H[T]] = createF[F, T](seed)
  }
}

@inline final class ProHandlerFactory[H[_,_] : CreateProHandler] {

  @inline def create[I,O](f: I => O): SyncIO[H[I,O]] = SyncIO(unsafe[I, O](f))
  @inline def create[I,O](seed: I)(f: I => O): SyncIO[H[I,O]] = SyncIO(unsafe[I, O](seed)(f))

  @inline def createF[F[_], I,O](f: I => O)(implicit F: Sync[F]): F[H[I,O]] = F.delay(unsafe[I, O](f))
  @inline def createF[F[_], I,O](seed: I)(f: I => O)(implicit F: Sync[F]): F[H[I,O]] = F.delay(unsafe[I, O](seed)(f))

  @inline def unsafe[I, O](f: I => O): H[I,O] = CreateProHandler[H].create[I,O](f)
  @inline def unsafe[I, O](seed: I)(f: I => O): H[I,O] = CreateProHandler[H].create[I,O](seed)(f)

  @inline def apply[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): H[I, O] = CreateProHandler[H].from(sink, source)
}
