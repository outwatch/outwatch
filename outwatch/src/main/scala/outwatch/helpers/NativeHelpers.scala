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
    @inline def style: CSSStyleDeclaration = elem
      .asInstanceOf[js.Dynamic]
      .style
      .asInstanceOf[CSSStyleDeclaration] // HTMLElement already has .style, but SVGElement doesn't
    @inline def dataset: js.Dictionary[String] = elem
      .asInstanceOf[js.Dynamic]
      .dataset
      .asInstanceOf[js.Dictionary[String]] // TODO: https://github.com/scala-js/scala-js-dom/pull/337
  }

  @noinline def appendSeq[F[-_], T, T2](
    source: js.Array[_ <: F[T]],
    other: collection.Seq[F[T2]],
  ): js.Array[_ <: F[T with T2]] = if (other.isEmpty) source
  else
    other match {
      case wrappedOther: js.WrappedArray[F[T2] @unchecked] =>
        if (source.isEmpty) wrappedOther else source.concat(wrappedOther)
      case _ =>
        val arr = new js.Array[F[T with T2]]()
        source.foreach(arr.push(_))
        other.foreach(arr.push(_))
        arr
    }

  @noinline def prependSeq[F[-_], T, T2](
    source: js.Array[_ <: F[T]],
    other: collection.Seq[F[T2]],
  ): js.Array[_ <: F[T with T2]] = if (other.isEmpty) source
  else
    other match {
      case wrappedOther: js.WrappedArray[F[T2] @unchecked] =>
        if (source.isEmpty) wrappedOther else wrappedOther.concat(source)
      case _ =>
        val arr = new js.Array[F[T with T2]]()
        other.foreach(arr.push(_))
        source.foreach(arr.push(_))
        arr
    }
}
