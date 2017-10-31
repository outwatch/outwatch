package outwatch.util

import cats.effect.IO
import outwatch.dom.{Attribute, AttributeStreamReceiver}
import rxscalajs.Observable


object SyntaxSugar {

  // Maybe find a better way to represent empty attributes
  private val emptyAttribute = Attribute("hidden", "")

  implicit class BooleanSelector(val values: Observable[Boolean]) extends AnyVal {
    def ?=(attr: IO[Attribute]): IO[AttributeStreamReceiver] = {
      attr.map { attr =>
        val attributes = values.map(b => if (b) attr else emptyAttribute)
        AttributeStreamReceiver(attr.title, attributes)
      }
    }
  }

}
