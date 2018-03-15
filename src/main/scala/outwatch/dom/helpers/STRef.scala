package outwatch.dom.helpers

import cats.effect.{Sync}
import cats.implicits._

class STRef[A](private var unsafeGet: A) {
  def put[F[+_]: Sync](a: A): F[A] = Sync[F].delay { unsafeGet = a; a }

  def getOrThrow[F[+_]: Sync](t: Throwable): F[A] = Sync[F].delay(unsafeGet)
    .flatMap(s => if (s == null) Sync[F].raiseError(t) else Sync[F].pure(s)) // scalastyle:ignore

  def get[F[+_]: Sync]: F[A] = getOrThrow(new IllegalStateException())
  def update[F[+_]: Sync](f: A => A): F[A] = Sync[F].delay { unsafeGet = f(unsafeGet); unsafeGet }
}

object STRef {
  def apply[A](a: A): STRef[A] = new STRef(a)
  def empty[A]: STRef[A] = new STRef[A](null.asInstanceOf[A]) // scalastyle:ignore
}
