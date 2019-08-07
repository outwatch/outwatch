package outwatch

import cats.effect.IO

package object util {
  @deprecated("Better to extend StoreOps[F], providing your own context such as IO, SyncIO, Task, etc", "")
  val Store = new StoreOps[IO]{}.Store
}
