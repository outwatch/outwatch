package outwatch.helpers

import outwatch._

@inline class ModifierBooleanOps(val condition: Boolean) extends AnyVal {
  @inline def apply(m: => VModifier): VModifier                   = if (condition) VModifier(m) else VModifier.empty
  @inline def apply(m: => VModifier, m2: => VModifier): VModifier = if (condition) VModifier(m, m2) else VModifier.empty
  @inline def apply(m: => VModifier, m2: => VModifier, m3: => VModifier): VModifier =
    if (condition) VModifier(m, m2, m3) else VModifier.empty
  @inline def apply(m: => VModifier, m2: => VModifier, m3: => VModifier, m4: => VModifier): VModifier =
    if (condition) VModifier(m, m2, m3, m4) else VModifier.empty
  @inline def apply(
    m: => VModifier,
    m2: => VModifier,
    m3: => VModifier,
    m4: => VModifier,
    m5: => VModifier,
  ): VModifier = if (condition) VModifier(m, m2, m3, m4, m5) else VModifier.empty
  @inline def apply(
    m: => VModifier,
    m2: => VModifier,
    m3: => VModifier,
    m4: => VModifier,
    m5: => VModifier,
    m6: => VModifier,
  ): VModifier = if (condition) VModifier(m, m2, m3, m4, m5, m6) else VModifier.empty
  @inline def apply(
    m: => VModifier,
    m2: => VModifier,
    m3: => VModifier,
    m4: => VModifier,
    m5: => VModifier,
    m6: => VModifier,
    m7: => VModifier,
  ): VModifier = if (condition) VModifier(m, m2, m3, m4, m5, m6, m7) else VModifier.empty
}
