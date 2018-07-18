package outwatch.util

import cats.effect.IO
import monix.reactive.Observable
import outwatch.dom.{Attribute, ModifierStreamReceiver, EmptyModifier}


object SyntaxSugar {

  implicit class BooleanSelector(val values: Observable[Boolean]) extends AnyVal {
    def ?=(attr: IO[Attribute]): IO[ModifierStreamReceiver] = {
      attr.map { attr =>
        val attributes = values.map(b => if (b) IO.pure(attr) else IO.pure(EmptyModifier))
        ModifierStreamReceiver(attributes)
      }
    }
  }

}
