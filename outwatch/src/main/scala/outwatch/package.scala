import cats.effect.IO

package object outwatch extends ObserverOps {
  @deprecated("Use ObserverBuilder instead", "")
  val Sink = ObserverBuilder

  object io extends MonixOps[IO]
}
