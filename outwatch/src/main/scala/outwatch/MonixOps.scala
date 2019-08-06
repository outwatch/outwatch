package outwatch

trait MonixOps[F[_]] extends ProHandlerOps[F] with util.StoreOps[F]
