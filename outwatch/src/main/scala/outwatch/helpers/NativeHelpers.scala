package outwatch.helpers

import org.scalajs.dom.Element
import org.scalajs.dom.CSSStyleDeclaration

import scala.scalajs.js
import scala.scalajs.js.annotation.JSBracketAccess

@js.native
private[outwatch] trait DictionaryRawApply[A] extends js.Object {
  @JSBracketAccess
  def apply(@annotation.unused key: String): js.UndefOr[A] = js.native
}

private[outwatch] object NativeHelpers {
  @inline implicit class WithRaw[A](val dict: js.Dictionary[A]) extends AnyVal {
    @inline def raw: DictionaryRawApply[A] = dict.asInstanceOf[DictionaryRawApply[A]]
  }

  @inline implicit class RichElement(val elem: Element) extends AnyVal {
    @inline def style: CSSStyleDeclaration = elem.asInstanceOf[js.Dynamic].style.asInstanceOf[CSSStyleDeclaration] // HTMLElement already has .style, but SVGElement doesn't
    @inline def dataset: js.Dictionary[String] = elem.asInstanceOf[js.Dynamic].dataset.asInstanceOf[js.Dictionary[String]] //TODO: https://github.com/scala-js/scala-js-dom/pull/337
  }

  @noinline def appendSeq[T](source: js.Array[T], other: collection.Seq[T]): js.Array[T] = if (other.isEmpty) source else other match {
    case wrappedOther:js.WrappedArray[_] =>
      if (source.isEmpty) wrappedOther.asInstanceOf[js.WrappedArray[T]] else source.concat(wrappedOther)
    case _ =>
      val arr = new js.Array[T]()
      source.foreach(arr.push(_))
      other.foreach(arr.push(_))
      arr
  }

  @noinline def prependSeq[T](source: js.Array[T], other: collection.Seq[T]): js.Array[T] = if (other.isEmpty) source else other match {
    case wrappedOther:js.WrappedArray[_] =>
      if (source.isEmpty) wrappedOther.asInstanceOf[js.WrappedArray[T]] else (wrappedOther.asInstanceOf[js.WrappedArray[T]]: js.Array[T]).concat(source)
    case _ =>
      val arr = new js.Array[T]()
      other.foreach(arr.push(_))
      source.foreach(arr.push(_))
      arr
  }
}
