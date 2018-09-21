package outwatch.util

import monix.reactive.Observable
import outwatch.dom.{Attribute, EmptyModifier, ModifierStreamReceiver, ValueObservable}


object SyntaxSugar {

  implicit class BooleanSelector(val values: Observable[Boolean]) extends AnyVal {
    def ?=(attr: Attribute): ModifierStreamReceiver = {
      val attributes = values.map(b => if (b) attr else EmptyModifier)
      ModifierStreamReceiver(ValueObservable(attributes))
    }
  }

}
