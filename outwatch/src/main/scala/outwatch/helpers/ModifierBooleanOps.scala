package outwatch.helpers

import outwatch._


@inline class ModifierBooleanOps(val condition: Boolean) extends AnyVal {
  @inline def apply[T](m: => RModifier[T]):RModifier[T] = if(condition) m else RModifier.empty
  @inline def apply[T](m: => RModifier[T], m2: => RModifier[T]):RModifier[T] = if(condition) RModifier[T](m,m2) else RModifier.empty
  @inline def apply[T](m: => RModifier[T], m2: => RModifier[T], m3: => RModifier[T]):RModifier[T] = if(condition) RModifier[T](m,m2,m3) else RModifier.empty
  @inline def apply[T](m: => RModifier[T], m2: => RModifier[T], m3: => RModifier[T], m4: => RModifier[T]):RModifier[T] = if(condition) RModifier[T](m,m2,m3,m4) else RModifier.empty
  @inline def apply[T](m: => RModifier[T], m2: => RModifier[T], m3: => RModifier[T], m4: => RModifier[T], m5: => RModifier[T]):RModifier[T] = if(condition) RModifier[T](m,m2,m3,m4,m5) else RModifier.empty
  @inline def apply[T](m: => RModifier[T], m2: => RModifier[T], m3: => RModifier[T], m4: => RModifier[T], m5: => RModifier[T], m6: => RModifier[T]):RModifier[T] = if(condition) RModifier[T](m,m2,m3,m4,m5,m6) else RModifier.empty
  @inline def apply[T](m: => RModifier[T], m2: => RModifier[T], m3: => RModifier[T], m4: => RModifier[T], m5: => RModifier[T], m6: => RModifier[T], m7: => RModifier[T]):RModifier[T] = if(condition) RModifier[T](m,m2,m3,m4,m5,m6,m7) else RModifier.empty
}

