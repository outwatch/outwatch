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

  @noinline def appendSeq[T](source: js.Array[T], other: collection.Seq[T]): js.Array[T] = if (other.isEmpty) source else other match {
    case wrappedOther:js.WrappedArray[T] =>
      if (source.isEmpty) wrappedOther.array else source.concat(wrappedOther.array)
    case _ =>
      val arr = new js.Array[T]()
      source.foreach(arr.push(_))
      other.foreach(arr.push(_))
      arr
  }

  @noinline def prependSeq[T](source: js.Array[T], other: collection.Seq[T]): js.Array[T] = if (other.isEmpty) source else other match {
    case wrappedOther:js.WrappedArray[T] =>
      if (source.isEmpty) wrappedOther.array else wrappedOther.array.concat(source)
    case _ =>
      val arr = new js.Array[T]()
      other.foreach(arr.push(_))
      source.foreach(arr.push(_))
      arr
  }
}
