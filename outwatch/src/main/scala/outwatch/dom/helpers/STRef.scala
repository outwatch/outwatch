package outwatch.dom.helpers

import cats.effect.Sync
import cats.implicits._

class STRef[F[_]: Sync, A](private var unsafeGet: A) {
  def put(a: A): F[A] = Sync[F].delay { unsafeGet = a; a }

  def getOrThrow(t: Throwable): F[A] = 
    unsafeGet.pure[F].flatMap(s => if (s == null) Sync[F].raiseError(t) else s.pure[F]) // scalastyle:ignore

  def get: F[A] = getOrThrow(new IllegalStateException())
  def update(f: A => A): F[A] = Sync[F].delay { unsafeGet = f(unsafeGet); unsafeGet }
}

object STRef {
  def apply[F[_]: Sync, A](a: A): STRef[F, A] = new STRef[F, A](a)
  def empty[F[_]: Sync, A >: Null]: STRef[F, A] = new STRef[F, A](null)
}
