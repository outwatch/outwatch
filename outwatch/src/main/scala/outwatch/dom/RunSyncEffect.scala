package outwatch.dom

import monix.eval.Coeval
import cats.Eval
import cats.effect.SyncIO

trait RunSyncEffect[-F[_]] {
  def unsafeRun[T](effect: F[T]): T
}

object RunSyncEffect {
  @inline def apply[F[_]](implicit run: RunSyncEffect[F]): RunSyncEffect[F] = run

  implicit object syncIO extends RunSyncEffect[SyncIO] {
    @inline def unsafeRun[T](effect: SyncIO[T]): T = effect.unsafeRunSync()
  }

  implicit object eval extends RunSyncEffect[Eval] {
    @inline def unsafeRun[T](effect: Eval[T]): T = effect.value
  }

  implicit object coeval extends RunSyncEffect[Coeval] {
    @inline def unsafeRun[T](effect: Coeval[T]): T = effect.apply()
  }
}

