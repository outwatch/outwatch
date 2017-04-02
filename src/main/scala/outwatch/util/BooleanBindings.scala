package outwatch.util

import outwatch.dom.{Attribute, AttributeStreamReceiver}
import rxscalajs.Observable


object BooleanBindings {

  private val emptyAttribute = Attribute("hidden","")

  implicit class ToBooleanBinder(val values: Observable[Boolean]) extends AnyVal {
    def ?=(attr: Attribute): AttributeStreamReceiver = {
      val attributes = values.map {
        case true => attr
        case false => emptyAttribute
      }

      AttributeStreamReceiver(attr.title, attributes)
    }
  }
}
