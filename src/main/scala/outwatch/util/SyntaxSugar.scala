package outwatch.util

import cats.effect.Effect
import cats.syntax.functor._
import outwatch.dom._

trait SyntaxSugarFactory[F[+_]] extends VDomModifierFactory[F] {
  implicit val effectF:Effect[F]

  object SyntaxSugar {

    implicit class BooleanSelector(val values: Observable[Boolean]) {
      def ?=(attr: F[TitledAttribute]): F[AttributeStreamReceiver] = {
        attr.map { attr =>
          val attributes = values.map(b => if (b) attr else Attribute.empty)
          AttributeStreamReceiver(attr.title, attributes)
        }
      }
    }

  }

}
