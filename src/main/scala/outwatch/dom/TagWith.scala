package outwatch.dom

import org.scalajs.dom

trait TagWithString[Elem <: dom.EventTarget] {
  def value(elem: Elem): String
}
object TagWithString {
  implicit object InputWithString extends TagWithString[dom.html.Input] {
    def value(elem: dom.html.Input) = elem.value
  }
  implicit object TextAreaWithString extends TagWithString[dom.html.TextArea] {
    def value(elem: dom.html.TextArea) = elem.value
  }
}

trait TagWithNumber[Elem <: dom.EventTarget] {
  def valueAsNumber(elem: Elem): Double
}
object TagWithNumber {
  implicit object InputWithNumber extends TagWithNumber[dom.html.Input] {
    def valueAsNumber(elem: dom.html.Input) = elem.valueAsNumber
  }
}

trait TagWithChecked[Elem <: dom.EventTarget] {
  def checked(elem: Elem): Boolean
}
object TagWithChecked {
  implicit object InputWithBoolean extends TagWithChecked[dom.html.Input] {
    def checked(elem: dom.html.Input) = elem.checked
  }
}
