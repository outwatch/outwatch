package outwatch.dom

import cats.effect.Sync
import cats.implicits._

trait ProHandlerOps[F[_]] extends HandlerOps[F] {

  object ProHandler {
    def create[I,O](f: I => O)(implicit F: Sync[F]): F[ProHandler[I,O]] = for {
      handler <- Handler.create[I]
    } yield handler.mapObservable[O](f)
  }
}
