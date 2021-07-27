package outwatch.helpers

import outwatch._

@inline class ModifierBooleanOps(val condition: Boolean) extends AnyVal {
  @inline def apply[T](m: => ModifierM[T]):ModifierM[T] = if(condition) m else ModifierM.empty
  @inline def apply[T](m: => ModifierM[T], m2: => ModifierM[T]):ModifierM[T] = if(condition) ModifierM[T](m,m2) else ModifierM.empty
  @inline def apply[T](m: => ModifierM[T], m2: => ModifierM[T], m3: => ModifierM[T]):ModifierM[T] = if(condition) ModifierM[T](m,m2,m3) else ModifierM.empty
  @inline def apply[T](m: => ModifierM[T], m2: => ModifierM[T], m3: => ModifierM[T], m4: => ModifierM[T]):ModifierM[T] = if(condition) ModifierM[T](m,m2,m3,m4) else ModifierM.empty
  @inline def apply[T](m: => ModifierM[T], m2: => ModifierM[T], m3: => ModifierM[T], m4: => ModifierM[T], m5: => ModifierM[T]):ModifierM[T] = if(condition) ModifierM[T](m,m2,m3,m4,m5) else ModifierM.empty
  @inline def apply[T](m: => ModifierM[T], m2: => ModifierM[T], m3: => ModifierM[T], m4: => ModifierM[T], m5: => ModifierM[T], m6: => ModifierM[T]):ModifierM[T] = if(condition) ModifierM[T](m,m2,m3,m4,m5,m6) else ModifierM.empty
  @inline def apply[T](m: => ModifierM[T], m2: => ModifierM[T], m3: => ModifierM[T], m4: => ModifierM[T], m5: => ModifierM[T], m6: => ModifierM[T], m7: => ModifierM[T]):ModifierM[T] = if(condition) ModifierM[T](m,m2,m3,m4,m5,m6,m7) else ModifierM.empty
}
