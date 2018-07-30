package outwatch.util

import cats.effect.IO
import monix.reactive.Observable
import outwatch.dom.{Attribute, AttributeStreamReceiver, TitledAttribute}


object SyntaxSugar {

  implicit class BooleanSelector(val values: Observable[Boolean]) extends AnyVal {
    def ?=(attr: IO[TitledAttribute]): IO[AttributeStreamReceiver] = {
      attr.map { attr =>
        val attributes = values.map(b => if (b) attr else Attribute.empty)
        AttributeStreamReceiver(attr.title, attributes)
      }
    }
  }

}
