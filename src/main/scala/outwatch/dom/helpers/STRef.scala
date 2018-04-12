package outwatch.dom.helpers

import cats.effect.{Effect, Sync}
import cats.implicits._
import org.scalajs.dom.svg.A

class STRef[F[+_], A](private var unsafeGet: A)(implicit effectF:Effect[F]) {
  def put(a: A): F[A] = effectF.delay { unsafeGet = a; a }

  def getOrThrow(t: Throwable): F[A] = effectF.delay(unsafeGet)
    .flatMap(s => if (s == null) effectF.raiseError(t) else effectF.pure(s)) // scalastyle:ignore

  def get: F[A] = getOrThrow(new IllegalStateException())
  def update(f: A => A): F[A] = effectF.delay { unsafeGet = f(unsafeGet); unsafeGet }
}

object STRef {
  def apply[F[+_]:Effect, A](a: A): STRef[F, A] = new STRef(a)
  def empty[F[+_]:Effect, A]: STRef[F, A] = new STRef[F, A](null.asInstanceOf[A]) // scalastyle:ignore
}
