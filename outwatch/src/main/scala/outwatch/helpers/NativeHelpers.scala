package outwatch.helpers

import com.github.ghik.silencer.silent
import org.scalajs.dom.Element
import org.scalajs.dom.raw.CSSStyleDeclaration

import scala.scalajs.js
import scala.scalajs.js.annotation.JSBracketAccess

private[outwatch] object JSDefined {
  // provides an extractor for js.UndefOr
  // https://gitter.im/scala-js/scala-js?at=5c3e221135350772cf375515
  def apply[A](a: A): js.UndefOr[A] = a
  def unapply[A](a: js.UndefOr[A]): UnapplyResult[A] = new UnapplyResult(a)

  final class UnapplyResult[+A](val self: js.UndefOr[A])
  extends AnyVal {
    @inline def isEmpty: Boolean = self eq js.undefined
    /** Calling `get` when `isEmpty` is true is undefined behavior. */
    @inline def get: A = self.asInstanceOf[A]
  }
}

@js.native
@silent("never used|dead code")
private[outwatch] trait DictionaryRawApply[A] extends js.Object {
  @JSBracketAccess
  def apply(key: String): js.UndefOr[A] = js.native
}

private[outwatch] object NativeHelpers {
  implicit class WithRaw[A](val dict: js.Dictionary[A]) extends AnyVal {
    @inline def raw: DictionaryRawApply[A] = dict.asInstanceOf[DictionaryRawApply[A]]
  }

  implicit class RichElement(val elem: Element) extends AnyVal {
    @inline def style: CSSStyleDeclaration = elem.asInstanceOf[js.Dynamic].style.asInstanceOf[CSSStyleDeclaration] // HTMLElement already has .style, but SVGElement doesn't
    @inline def dataset: js.Dictionary[String] = elem.asInstanceOf[js.Dynamic].dataset.asInstanceOf[js.Dictionary[String]] //TODO: https://github.com/scala-js/scala-js-dom/pull/337
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
