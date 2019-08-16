package outwatch
package dom

import cats.effect.Sync
import monix.reactive.subjects.{BehaviorSubject, ReplaySubject}

trait HandlerOps[F[_]] {
  object Handler {
    def empty[T](implicit F: Sync[F]): F[Handler[T]] = create[T]

    def create[T](implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T])
    def create[T](seed:T)(implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T](seed))

    def unsafe[T]: Handler[T] = ReplaySubject.createLimited(1)
    def unsafe[T](seed:T): Handler[T] = BehaviorSubject[T](seed)
  }
}
