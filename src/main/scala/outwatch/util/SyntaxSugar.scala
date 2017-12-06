package outwatch.util

import cats.effect.IO
import outwatch.dom.{Attribute, AttributeStreamReceiver, EmptyAttribute}
import rxscalajs.Observable


object SyntaxSugar {

  implicit class BooleanSelector(val values: Observable[Boolean]) extends AnyVal {
    def ?=(attr: IO[Attribute]): IO[AttributeStreamReceiver] = {
      attr.map { attr =>
        val attributes = values.map(b => if (b) attr else EmptyAttribute)
        AttributeStreamReceiver(attr.title, attributes)
      }
    }
  }

}
