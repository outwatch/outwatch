package outwatch

trait MonixOps[F[_]] extends dom.ProHandlerOps[F] with util.StoreOps[F]
