package outwatch
package dom

import cats.effect.Sync
import monix.reactive.subjects.{BehaviorSubject, ReplaySubject}

object Handler {
  def empty[F[_]: Sync, T]: F[Handler[T]] = create[F, T]

  def create[F[_]] = new CreatePartiallyApplied[F]
  def create[F[_], T](implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T])
  def create[F[_], T](seed: T)(implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T](seed))


  /* Partial application trick, "kinda-curried type parameters"
   * https://typelevel.org/cats/guidelines.html
   */
  final class CreatePartiallyApplied[F[_]](val dummy: Boolean = false) extends AnyVal {
    def apply[T](seed: T)(implicit F: Sync[F]): F[Handler[T]] = F.delay(unsafe[T](seed))
  }

  def unsafe[T]: Handler[T] = ReplaySubject.createLimited(1)
  def unsafe[T](seed:T): Handler[T] = BehaviorSubject[T](seed)
}
