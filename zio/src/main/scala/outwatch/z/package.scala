package outwatch

import zio._
import zio.internal.Platform
import zio.interop.catz._

package object z {
  type ZModifierEnv = Has[Platform]
  type ZRModifier[-Env] = RModifier[ZModifierEnv with Env]
  type ZModifier = ZRModifier[Any]

  type ZREmitterBuilder[-Env, +O, +R <: RModifier[Env]] = REmitterBuilder[ZModifierEnv with Env, O, R]
  type ZEmitterBuilder[+O, +R <: Modifier] = ZREmitterBuilder[Any, O, R]

  object ZEmitterBuilder {
    type RSync[-Env, +O, +R <: RModifier[Env]] = EmitterBuilder.RSync[ZModifierEnv with Env, O, R]
    type Sync[+O, +R <: Modifier] = RSync[Any, O, R]
  }

  implicit def render[Env, T: Render[Any, ?]]: Render[ZModifierEnv with Env, RIO[Env, T]] = new Render[ZModifierEnv with Env, RIO[Env, T]] {
    def render(effect: RIO[Env, T]) = Modifier.access[ZModifierEnv with Env] { env =>
      implicit val runtime = Runtime(env, env.get[Platform])
      RModifier(effect)
    }
  }

  //TODO observables? colibri
  //TODO emitterbuilder useAsync?

  @inline implicit class EmitterBuilderOpsModifier[Env, O, Exec <: EmitterBuilder.Execution](val self: REmitterBuilderExecution[Env, O, RModifier[Env], Exec]) extends AnyVal {
    @inline def useZIO[R, T](effect: RIO[R, T]): REmitterBuilder[ZModifierEnv with R with Env, T, RModifier[ZModifierEnv with R with Env]] =
      concatMapZIO(_ => effect)

    @inline def concatMapZIO[R, T](effect: O => RIO[R, T]): REmitterBuilder[ZModifierEnv with R with Env, T, RModifier[ZModifierEnv with R with Env]] =
      EmitterBuilder.access { env =>
        implicit val runtime = Runtime(env, env.get[Platform])
        self.concatMapAsync(effect).provide(env)
      }

    @inline def foreachZIO[R](action: O => RIO[R, Unit]): RModifier[ZModifierEnv with R with Env] = concatMapZIO(action).discard
    @inline def doZIO[R](action: RIO[R, Unit]): RModifier[ZModifierEnv with R with Env] = foreachZIO(_ => action)
  }
}
