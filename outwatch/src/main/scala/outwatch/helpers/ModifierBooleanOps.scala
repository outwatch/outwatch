package outwatch.helpers

import outwatch._

@inline class VModBooleanOps(val condition: Boolean) extends AnyVal {
  @inline def apply[T](m: => VModM[T]): VModM[T] = if (condition) m else VModM.empty
  @inline def apply[T](m: => VModM[T], m2: => VModM[T]): VModM[T] =
    if (condition) VModM[T](m, m2) else VModM.empty
  @inline def apply[T](m: => VModM[T], m2: => VModM[T], m3: => VModM[T]): VModM[T] =
    if (condition) VModM[T](m, m2, m3) else VModM.empty
  @inline def apply[T](
    m: => VModM[T],
    m2: => VModM[T],
    m3: => VModM[T],
    m4: => VModM[T],
  ): VModM[T] = if (condition) VModM[T](m, m2, m3, m4) else VModM.empty
  @inline def apply[T](
    m: => VModM[T],
    m2: => VModM[T],
    m3: => VModM[T],
    m4: => VModM[T],
    m5: => VModM[T],
  ): VModM[T] = if (condition) VModM[T](m, m2, m3, m4, m5) else VModM.empty
  @inline def apply[T](
    m: => VModM[T],
    m2: => VModM[T],
    m3: => VModM[T],
    m4: => VModM[T],
    m5: => VModM[T],
    m6: => VModM[T],
  ): VModM[T] = if (condition) VModM[T](m, m2, m3, m4, m5, m6) else VModM.empty
  @inline def apply[T](
    m: => VModM[T],
    m2: => VModM[T],
    m3: => VModM[T],
    m4: => VModM[T],
    m5: => VModM[T],
    m6: => VModM[T],
    m7: => VModM[T],
  ): VModM[T] = if (condition) VModM[T](m, m2, m3, m4, m5, m6, m7) else VModM.empty
}
