package outwatch.dom.helpers

import cats.effect.IO

class STRef[A](private var unsafeGet: A) {
  def put(a: A): IO[A] = IO { unsafeGet = a; a }
  def get: IO[A] = IO(unsafeGet)
  def update(f: A => A): IO[A] = IO { unsafeGet = f(unsafeGet); unsafeGet }
}

object STRef {
  def apply[A](a: A) = new STRef(a)
  def empty[A] = new STRef[A](null.asInstanceOf[A])
}
