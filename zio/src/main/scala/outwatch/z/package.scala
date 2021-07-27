package outwatch

import zio._
import zio.internal.Platform
import zio.interop.catz._

package object z {
  type ZModifierEnv = Has[Platform]
  type ZModifierM[-Env] = ModifierM[ZModifierEnv with Env]
  type ZModifier = ZModifierM[Any]

  implicit def renderWithError[Env, RE, RT, E: Render[RE, *], T: Render[RT, *]]: Render[ZModifierEnv with Env with RT with RE, ZIO[Env, E, T]] = new Render[ZModifierEnv with Env with RT with RE, ZIO[Env, E, T]] {
    def render(effect: ZIO[Env, E, T]) = ModifierM.access[ZModifierEnv with Env with RT with RE] { env =>
      implicit val runtime = Runtime(env, env.get[Platform])
      Render.EffectRenderAs[RIO[Env, *], RE with RT, ModifierM[RE with RT]].render(effect.fold(ModifierM(_), ModifierM(_))).provide(env)
    }
  }

  implicit def renderWithoutError[Env, R, T: Render[R, *]]: Render[ZModifierEnv with Env with R, RIO[Env, T]] = new Render[ZModifierEnv with Env with R, RIO[Env, T]] {
    def render(effect: RIO[Env, T]) = ModifierM.access[ZModifierEnv with Env with R] { env =>
      implicit val runtime = Runtime(env, env.get[Platform])
      Render.EffectRenderAs[RIO[Env, *], R, T].render(effect).provide(env)
    }
  }

  @inline implicit def accessEnvironment[Result]: AccessEnvironment[RIO[-*, Result]] = new AccessEnvironment[RIO[-*, Result]] {
    @inline def access[Env](f: Env => RIO[Any, Result]): RIO[Env, Result] = RIO.accessM(f)
    @inline def provide[Env](t: RIO[Env, Result])(env: Env): RIO[Any, Result] = t.provide(env)
    @inline def provideSome[Env, R](t: RIO[Env, Result])(map: R => Env): RIO[R, Result] = t.provideSome(map)
  }

  @inline implicit class EmitterBuilderOpsAccessEnvironment[Env, O, Result[-_]](val self: EmitterBuilder[O, Result[Env]])(implicit acc: AccessEnvironment[Result]) {
    @inline def useZIO[R, T](effect: RIO[R, T]): EmitterBuilder[T, Result[ZModifierEnv with R with Env]] = mapZIO(_ => effect)
    @inline def useZIOSingleOrDrop[R, T](effect: RIO[R, T]): EmitterBuilder[T, Result[ZModifierEnv with R with Env]] = mapZIOSingleOrDrop(_ => effect)

    @inline def mapZIO[R, T](effect: O => RIO[R, T]): EmitterBuilder[T, Result[ZModifierEnv with R with Env]] =
      EmitterBuilder.accessM { env =>
        implicit val runtime = Runtime(env, env.get[Platform])
        self.mapAsync(effect).provide(env)
      }

    @inline def mapZIOSingleOrDrop[R, T](effect: O => RIO[R, T]): EmitterBuilder[T, Result[ZModifierEnv with R with Env]] =
      EmitterBuilder.accessM { env =>
        implicit val runtime = Runtime(env, env.get[Platform])
        self.mapAsyncSingleOrDrop(effect).provide(env)
      }

    @inline def foreachZIO[R](action: O => RIO[R, Unit]): Result[ZModifierEnv with R with Env] = mapZIO(action).discard
    @inline def doZIO[R](action: RIO[R, Unit]): Result[ZModifierEnv with R with Env] = foreachZIO(_ => action)
    @inline def foreachZIOSingleOrDrop[R](action: O => RIO[R, Unit]): Result[ZModifierEnv with R with Env] = mapZIOSingleOrDrop(action).discard
    @inline def doZIOSingleOrDrop[R](action: RIO[R, Unit]): Result[ZModifierEnv with R with Env] = foreachZIOSingleOrDrop(_ => action)
  }

  @inline implicit class EmitterBuilderOps[O, Result](val self: EmitterBuilder[O, Result]) extends AnyVal {
    @inline def useZIO[R, T](effect: RIO[R, T]): EmitterBuilder[T, RIO[ZModifierEnv with R, Result]] = mapZIO(_ => effect)
    @inline def useZIOSingleOrDrop[R, T](effect: RIO[R, T]): EmitterBuilder[T, RIO[ZModifierEnv with R, Result]] = mapZIOSingleOrDrop(_ => effect)

    @inline def mapZIO[R, T](effect: O => RIO[R, T]): EmitterBuilder[T, RIO[ZModifierEnv with R, Result]] =
      EmitterBuilder.accessM[ZModifierEnv with R].apply[Any, T, RIO[-*, Result], EmitterBuilderExec.Execution] { env =>
        implicit val runtime = Runtime(env, env.get[Platform])
        self.mapAsync(effect).mapResult(RIO.succeed(_))
      }

    @inline def mapZIOSingleOrDrop[R, T](effect: O => RIO[R, T]): EmitterBuilder[T, RIO[ZModifierEnv with R, Result]] =
      EmitterBuilder.accessM[ZModifierEnv with R].apply[Any, T, RIO[-*, Result], EmitterBuilderExec.Execution] { env =>
        implicit val runtime = Runtime(env, env.get[Platform])
        self.mapAsyncSingleOrDrop(effect).mapResult(RIO.succeed(_))
      }

    @inline def foreachZIO[R](action: O => RIO[R, Unit]): RIO[ZModifierEnv with R, Result] = mapZIO(action).discard
    @inline def doZIO[R](action: RIO[R, Unit]): RIO[ZModifierEnv with R, Result] = foreachZIO(_ => action)
    @inline def foreachZIOSingleOrDrop[R](action: O => RIO[R, Unit]): RIO[ZModifierEnv with R, Result] = mapZIOSingleOrDrop(action).discard
    @inline def doZIOSingleOrDrop[R](action: RIO[R, Unit]): RIO[ZModifierEnv with R, Result] = foreachZIOSingleOrDrop(_ => action)
  }

  @inline implicit final class AccessEnvironmentDispatchOperations[O : Tag, R[-_], Env](val builder: EmitterBuilder[O, R[Env]])(implicit acc: AccessEnvironment[R]) {
    @inline def dispatch: R[Env with Has[EventDispatcher[O]]] = AccessEnvironment[R].accessM(env => builder.dispatchWith(env.get))
  }
}
