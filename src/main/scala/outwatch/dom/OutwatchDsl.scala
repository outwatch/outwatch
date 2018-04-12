package outwatch.dom

import cats.effect.Effect
import com.raquo.domtypes.generic.builders.TagBuilder
import com.raquo.domtypes.generic.defs.styles.Styles

trait OutwatchDsl[F[+_]] extends DomTypesFactory[F] { thisDsl =>
  implicit val effectF: Effect[F]

  type VNode = VNodeF
  type VDomModifier = VDomModifierF

  object tags extends Tags with TagBuilder {
    object extra extends TagsExtra with TagBuilder
  }
  object attributes extends Attributes {
    object attrs extends Attrs
    object reflected extends ReflectedAttrs
    object props extends Props
    object events extends Events
    object outwatch extends OutwatchAttributes
    object lifecycle extends OutWatchLifeCycleAttributes
  }
  object events {
    object window extends WindowEvents
    object document extends DocumentEvents
  }
}


