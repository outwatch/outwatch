package outwatch.libs.clipboardjs

import outwatch.libs.clipboardjs.facade._
import outwatch._
import outwatch.dsl._

trait ClipboardJs {
  def onClickCopyToClipboard(text: String): VDomModifier = {
    VDomModifier(
      dataAttr("clipboard-text") := text,
      managedElement.asHtml { elem =>
        val clip = new ClipboardJS(elem)
        cancelable(() => clip.destroy())
      }
    )
  }

}

object ClipboardJs extends ClipboardJs
