package outwatch.helpers

import outwatch._

@inline class VModifierBooleanOps(val condition: Boolean) extends AnyVal {
  @inline def apply[T](m: => VModifierM[T]):VModifierM[T] = if(condition) m else VModifierM.empty
  @inline def apply[T](m: => VModifierM[T], m2: => VModifierM[T]):VModifierM[T] = if(condition) VModifierM[T](m,m2) else VModifierM.empty
  @inline def apply[T](m: => VModifierM[T], m2: => VModifierM[T], m3: => VModifierM[T]):VModifierM[T] = if(condition) VModifierM[T](m,m2,m3) else VModifierM.empty
  @inline def apply[T](m: => VModifierM[T], m2: => VModifierM[T], m3: => VModifierM[T], m4: => VModifierM[T]):VModifierM[T] = if(condition) VModifierM[T](m,m2,m3,m4) else VModifierM.empty
  @inline def apply[T](m: => VModifierM[T], m2: => VModifierM[T], m3: => VModifierM[T], m4: => VModifierM[T], m5: => VModifierM[T]):VModifierM[T] = if(condition) VModifierM[T](m,m2,m3,m4,m5) else VModifierM.empty
  @inline def apply[T](m: => VModifierM[T], m2: => VModifierM[T], m3: => VModifierM[T], m4: => VModifierM[T], m5: => VModifierM[T], m6: => VModifierM[T]):VModifierM[T] = if(condition) VModifierM[T](m,m2,m3,m4,m5,m6) else VModifierM.empty
  @inline def apply[T](m: => VModifierM[T], m2: => VModifierM[T], m3: => VModifierM[T], m4: => VModifierM[T], m5: => VModifierM[T], m6: => VModifierM[T], m7: => VModifierM[T]):VModifierM[T] = if(condition) VModifierM[T](m,m2,m3,m4,m5,m6,m7) else VModifierM.empty
}
