package outwatch.dom.helpers

import cats.effect.Effect

class STRef[A](private var unsafeGet: A) {
  def put(a: A): F[A] = Effect[F].delay { unsafeGet = a; a }

  def getOrThrow(t: Throwable): F[A] = IO(unsafeGet)
    .flatMap(s => if (s == null) IO.raiseError(t) else Effect[F].pure(s)) // scalastyle:ignore

  def get: F[A] = getOrThrow(new IllegalStateException())
  def update(f: A => A): F[A] = Effect[F].delay { unsafeGet = f(unsafeGet); unsafeGet }
}

object STRef {
  def apply[A](a: A): STRef[A] = new STRef(a)
  def empty[A]: STRef[A] = new STRef[A](null.asInstanceOf[A]) // scalastyle:ignore
}
