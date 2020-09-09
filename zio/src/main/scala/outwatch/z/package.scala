package outwatch

import zio._
import zio.internal.Platform
import zio.interop.catz._

package object z {
  type ZModifierEnv = Has[Platform]
  type ZRModifier[Env] = RModifier[ZModifierEnv with Has[Env]]
  type ZModifier = ZRModifier[ZModifierEnv]

  implicit def render[Env, T: Render[Any, ?]]: Render[ZModifierEnv with Has[Env], RIO[Has[Env], T]] = new Render[ZModifierEnv with Has[Env], RIO[Has[Env], T]] {
    def render(effect: RIO[Has[Env], T]) = Modifier.access[ZModifierEnv with Has[Env]] { env =>
      implicit val runtime: Runtime[Has[Env]] = Runtime[Has[Env]](env, env.get[Platform])
      RModifier(effect)
    }
  }

  //TODO observables? colibri
  //TODO emitterbuilder useAsync?
}
