package outwatch

import zio._
import zio.internal.Platform
import zio.interop.catz._

package object z {
  type ZModifierEnv = Has[Platform]
  type ZRModifier[-Env] = RModifier[ZModifierEnv with Env]
  type ZModifier = ZRModifier[ZModifierEnv]

  type ZREmitterBuilder[-Env, +O, +R[-_]] = REmitterBuilder[ZModifierEnv with Env, O, R]
  type ZEmitterBuilder[+O, +R[-_]] = REmitterBuilder[ZModifierEnv, O, R]
  type ZREmitterBuilderSync[-Env, +O, +R[-_]] = EmitterBuilder.RSync[ZModifierEnv with Env, O, R]
  type ZEmitterBuilderSync[+O, +R[-_]] = EmitterBuilder.RSync[ZModifierEnv, O, R]

  implicit def render[Env, T: Render[Any, ?]]: Render[ZModifierEnv with Env, RIO[Env, T]] = new Render[ZModifierEnv with Env, RIO[Env, T]] {
    def render(effect: RIO[Env, T]) = Modifier.access[ZModifierEnv with Env] { env =>
      implicit val runtime = Runtime(env, env.get[Platform])
      RModifier(effect)
    }
  }

  //TODO observables? colibri
  //TODO emitterbuilder useAsync?

  @inline implicit class EmitterBuilderOpsModifier[Env, O, R[-_], Exec <: EmitterBuilder.Execution](val self: EmitterBuilderExecution[Env, O, R, Exec]) extends AnyVal {
    @inline def useZIO[REnv, T](effect: RIO[REnv, T]): REmitterBuilder[ZModifierEnv with REnv with Env, T, R] = concatMapZIO(_ => effect)
    @inline def concatMapZIO[REnv, T](effect: O => RIO[REnv, T]): REmitterBuilder[ZModifierEnv with REnv with Env, T, R] = EmitterBuilder.access { env =>
      implicit val runtime = Runtime(env, env.get[Platform])
      self.concatMapAsync(effect).provide(env)
    }
    @inline def foreachZIO[REnv](action: O => RIO[REnv, Unit]): R[ZModifierEnv with REnv with Env] = concatMapZIO(action).discard
    @inline def doZIO[REnv](action: RIO[REnv, Unit]): R[ZModifierEnv with REnv with Env] = foreachZIO(_ => action)
  }
}
