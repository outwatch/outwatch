package outwatch.dom.helpers

import com.github.ghik.silencer.silent
import scala.scalajs.js
import scala.scalajs.js.annotation.JSBracketAccess

@js.native
private[outwatch] trait DictionaryRawApply[A] extends js.Object {
  @silent("never used|dead code")
  @JSBracketAccess
  def apply(key: String): js.UndefOr[A] = js.native
}

private[outwatch] object NativeHelpers {
  implicit class WithRaw[A](val dict: js.Dictionary[A]) extends AnyVal {
    @inline def raw: DictionaryRawApply[A] = dict.asInstanceOf[DictionaryRawApply[A]]
  }

  @inline def assign[T](value: T)(f: T => Unit): T = { f(value); value }

  @inline def appendSeq[T](source: js.Array[T], other: Seq[T]): js.Array[T] = other match {
    case wrappedOther:js.WrappedArray[T] => source.concat(wrappedOther.array)
    case _                               => source ++ other
  }

  @inline def prependSeq[T](source: js.Array[T], other: Seq[T]): js.Array[T] = other match {
    case wrappedOther:js.WrappedArray[T] => wrappedOther.array.concat(source)
    case _                               => other.++(source)(collection.breakOut)
  }
}
