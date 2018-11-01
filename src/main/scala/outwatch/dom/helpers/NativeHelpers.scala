package outwatch.dom.helpers

import scala.scalajs.js
import scala.scalajs.js.annotation.JSBracketAccess

@js.native
private[outwatch] trait DictionaryRawApply[A] extends js.Object {
  @JSBracketAccess
  def apply(key: String): js.UndefOr[A] = js.native
}
private[outwatch] object NativeHelpers {
  implicit class WithRaw[A](val dict: js.Dictionary[A]) extends AnyVal {
    @inline def raw: DictionaryRawApply[A] = dict.asInstanceOf[DictionaryRawApply[A]]
  }

  @inline def arrayConcat[A](a: js.Array[A], b: Seq[A]): js.Array[A] = {
    val result = new js.Array[A](a.length + b.length)

    var i = 0
    while (i < a.length) {
      result(i) = a(i)
      i += 1
    }
    i = 0
    while (i < b.length) {
      result(i + a.length) = b(i)
      i += 1
    }

    result
  }


  @inline def assign[T](value: T)(f: T => Unit): T = { f(value); value }
}
