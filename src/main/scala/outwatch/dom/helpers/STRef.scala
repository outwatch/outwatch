package outwatch.dom.helpers

import cats.effect.{Effect, Sync}
import cats.implicits._
import org.scalajs.dom.svg.A

class STRef[F[+_]: Effect, A](private var unsafeGet: A) {
  def put(a: A): F[A] = Sync[F].delay { unsafeGet = a; a }

  def getOrThrow(t: Throwable): F[A] = Sync[F].delay(unsafeGet)
    .flatMap(s => if (s == null) Sync[F].raiseError(t) else Sync[F].pure(s)) // scalastyle:ignore

  def get: F[A] = getOrThrow(new IllegalStateException())
  def update(f: A => A): F[A] = Sync[F].delay { unsafeGet = f(unsafeGet); unsafeGet }
}

object STRef {
  def apply[F[+_]:Effect, A](a: A): STRef[F, A] = new STRef(a)
  def empty[F[+_]:Effect, A]: STRef[F, A] = new STRef[F, A](null.asInstanceOf[A]) // scalastyle:ignore
}
