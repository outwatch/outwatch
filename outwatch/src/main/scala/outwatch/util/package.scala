package outwatch

import cats.effect.IO

package object util {
  @deprecated("Better to extend StoreOps[F], providing your own context such as IO, SyncIO, Task, etc. If you want to use IO, you can import from outwatch.util.io", "")
  val Store = new StoreOps[IO]{}.Store

  object io extends StoreOps[IO]
}
