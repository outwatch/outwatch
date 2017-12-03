package outwatch.dom

import org.scalajs.dom

trait TagWithString[Elem <: dom.EventTarget] {
  def string(elem: Elem): String
}
object TagWithString {
  implicit object InputWithString extends TagWithString[dom.html.Input] {
    def string(elem: dom.html.Input) = elem.value
  }
  implicit object TextAreaWithString extends TagWithString[dom.html.TextArea] {
    def string(elem: dom.html.TextArea) = elem.value
  }
  implicit object ButtonWithString extends TagWithString[dom.html.Button] {
    def string(elem: dom.html.Button) = elem.value
  }
  implicit object OptionWithString extends TagWithString[dom.html.Option] {
    def string(elem: dom.html.Option) = elem.value
  }
  implicit object ParamWithString extends TagWithString[dom.html.Param] {
    def string(elem: dom.html.Param) = elem.value
  }
}

trait TagWithNumber[Elem <: dom.EventTarget] {
  def number(elem: Elem): Double
}
object TagWithNumber {
  implicit object InputWithNumber extends TagWithNumber[dom.html.Input] {
    def number(elem: dom.html.Input) = elem.valueAsNumber
  }
  implicit object LiWithNumber extends TagWithNumber[dom.html.LI] {
    def number(elem: dom.html.LI) = elem.value
  }
  implicit object ProgressWithNumber extends TagWithNumber[dom.html.Progress] {
    def number(elem: dom.html.Progress) = elem.value
  }
}

trait TagWithBoolean[Elem <: dom.EventTarget] {
  def boolean(elem: Elem): Boolean
}
object TagWithBoolean {
  implicit object InputWithBoolean extends TagWithBoolean[dom.html.Input] {
    def boolean(elem: dom.html.Input) = elem.checked
  }
  implicit object OptionWithBoolean extends TagWithBoolean[dom.html.Option] {
    def boolean(elem: dom.html.Option) = elem.selected
  }
}
