package outwatch.util

import cats.Functor
import cats.syntax.functor._
import outwatch.dom.{Attribute, AttributeStreamReceiver, Observable, TitledAttribute}


object SyntaxSugar {

  implicit class BooleanSelector(val values: Observable[Boolean]) extends AnyVal {
    def ?=[F[+_]: Functor](attr: F[TitledAttribute]): F[AttributeStreamReceiver] = {
      attr.map { attr =>
        val attributes = values.map(b => if (b) attr else Attribute.empty)
        AttributeStreamReceiver(attr.title, attributes)
      }
    }
  }

}
