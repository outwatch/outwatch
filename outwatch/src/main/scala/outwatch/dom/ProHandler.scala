package outwatch.dom

import cats.effect.Sync
import cats.implicits._


object ProHandler {
  def create[F[_]: Sync, I,O](f: I => O): F[ProHandler[I,O]] = for {
    handler <- Handler.create[F, I]
  } yield handler.mapObservable[O](f)
}
