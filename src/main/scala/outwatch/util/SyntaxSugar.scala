package outwatch.util

import cats.effect.IO
import outwatch.dom.{Attribute, AttributeStreamReceiver}
import rxscalajs.Observable


object SyntaxSugar {

  // Maybe find a better way to represent empty attributes
  private val emptyAttribute = Attribute("hidden", "")

  implicit class BooleanSelector(val values: Observable[Boolean]) extends AnyVal {
    def ?=(attr: IO[Attribute]): IO[AttributeStreamReceiver] = {
      val attr_ = attr.unsafeRunSync()
      val attributes =
        values.map(b => if (b) attr_ else emptyAttribute)
      IO.pure(AttributeStreamReceiver(attr_.title, attributes))
    }
  }
}
