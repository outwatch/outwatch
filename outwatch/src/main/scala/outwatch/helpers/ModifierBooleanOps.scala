package outwatch.helpers

import outwatch._

@inline class ModifierBooleanOps(val condition: Boolean) extends AnyVal {
  @inline def apply(m: => VMod): VMod              = if (condition) VMod(m) else VMod.empty
  @inline def apply(m: => VMod, m2: => VMod): VMod = if (condition) VMod(m, m2) else VMod.empty
  @inline def apply(m: => VMod, m2: => VMod, m3: => VMod): VMod =
    if (condition) VMod(m, m2, m3) else VMod.empty
  @inline def apply(m: => VMod, m2: => VMod, m3: => VMod, m4: => VMod): VMod =
    if (condition) VMod(m, m2, m3, m4) else VMod.empty
  @inline def apply(
    m: => VMod,
    m2: => VMod,
    m3: => VMod,
    m4: => VMod,
    m5: => VMod,
  ): VMod = if (condition) VMod(m, m2, m3, m4, m5) else VMod.empty
  @inline def apply(
    m: => VMod,
    m2: => VMod,
    m3: => VMod,
    m4: => VMod,
    m5: => VMod,
    m6: => VMod,
  ): VMod = if (condition) VMod(m, m2, m3, m4, m5, m6) else VMod.empty
  @inline def apply(
    m: => VMod,
    m2: => VMod,
    m3: => VMod,
    m4: => VMod,
    m5: => VMod,
    m6: => VMod,
    m7: => VMod,
  ): VMod = if (condition) VMod(m, m2, m3, m4, m5, m6, m7) else VMod.empty
}
