package outwatch.dom.helpers

import cats.effect.IO

class STRef[A](private var unsafeGet: A) {
  def put(a: A): IO[A] = IO { unsafeGet = a; a }

  def getOrThrow(t: Throwable): IO[A] = IO(unsafeGet)
    .flatMap(s => if (s == null) IO.raiseError(t) else IO.pure(s)) // scalastyle:ignore

  def get: IO[A] = getOrThrow(new IllegalStateException())
  def update(f: A => A): IO[A] = IO { unsafeGet = f(unsafeGet); unsafeGet }
}

object STRef {
  def apply[A](a: A): STRef[A] = new STRef(a)
  def empty[A]: STRef[A] = new STRef[A](null.asInstanceOf[A]) // scalastyle:ignore
}
