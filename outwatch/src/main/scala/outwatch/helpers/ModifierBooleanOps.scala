package outwatch.helpers

import outwatch._

@inline class ModifierBooleanOps(val condition: Boolean) extends AnyVal {
  @inline def apply[Env](m: => VModM[Env]): VModM[Env]                    = if (condition) VMod(m) else VMod.empty
  @inline def apply[Env](m: => VModM[Env], m2: => VModM[Env]): VModM[Env] = if (condition) VMod(m, m2) else VMod.empty
  @inline def apply[Env](m: => VModM[Env], m2: => VModM[Env], m3: => VModM[Env]): VModM[Env] =
    if (condition) VMod(m, m2, m3) else VMod.empty
  @inline def apply[Env](m: => VModM[Env], m2: => VModM[Env], m3: => VModM[Env], m4: => VModM[Env]): VModM[Env] =
    if (condition) VMod(m, m2, m3, m4) else VMod.empty
  @inline def apply[Env](
    m: => VModM[Env],
    m2: => VModM[Env],
    m3: => VModM[Env],
    m4: => VModM[Env],
    m5: => VModM[Env],
  ): VModM[Env] = if (condition) VMod(m, m2, m3, m4, m5) else VMod.empty
  @inline def apply[Env](
    m: => VModM[Env],
    m2: => VModM[Env],
    m3: => VModM[Env],
    m4: => VModM[Env],
    m5: => VModM[Env],
    m6: => VModM[Env],
  ): VModM[Env] = if (condition) VMod(m, m2, m3, m4, m5, m6) else VMod.empty
  @inline def apply[Env](
    m: => VModM[Env],
    m2: => VModM[Env],
    m3: => VModM[Env],
    m4: => VModM[Env],
    m5: => VModM[Env],
    m6: => VModM[Env],
    m7: => VModM[Env],
  ): VModM[Env] = if (condition) VMod(m, m2, m3, m4, m5, m6, m7) else VMod.empty
}
