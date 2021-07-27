package outwatch

trait AccessEnvironment[T[-_]] {
  def access[Env](f: Env => T[Any]): T[Env]
  def provide[Env](t: T[Env])(env: Env): T[Any]
  def provideSome[Env, R](t: T[Env])(map: R => Env): T[R]

  final def accessM[Env, Env2](f: Env => T[Env2]): T[Env with Env2] = access(env => provide(f(env))(env))
}
object AccessEnvironment {
  @inline def apply[T[-_]](implicit env: AccessEnvironment[T]): AccessEnvironment[T] = env

  implicit object modifier extends AccessEnvironment[ModifierM] {
    @inline def access[Env](f: Env => ModifierM[Any]): ModifierM[Env] = ModifierM.access(f)
    @inline def provide[Env](t: ModifierM[Env])(env: Env): ModifierM[Any] = t.provide(env)
    @inline def provideSome[Env, R](t: ModifierM[Env])(map: R => Env): ModifierM[R] = t.provideSome(map)
  }
  implicit object vnode extends AccessEnvironment[VNodeM] {
    @inline def access[Env](f: Env => VNodeM[Any]): VNodeM[Env] = VNodeM.access(f)
    @inline def provide[Env](t: VNodeM[Env])(env: Env): VNodeM[Any] = t.provide(env)
    @inline def provideSome[Env, R](t: VNodeM[Env])(map: R => Env): VNodeM[R] = t.provideSome(map)
  }
}
