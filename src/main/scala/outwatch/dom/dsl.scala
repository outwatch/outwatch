package outwatch.dom

abstract class dslEffect[F[_]:Effect] extends Attributes with Tags[F] with Styles[F] {
  object tags extends Tags {
    object extra extends TagsExtra
  }
  object attributes extends Attributes {
    object attrs extends Attrs
    object reflected extends ReflectedAttrs
    object props extends Props
    object events extends Events
    object outwatch extends OutwatchAttributes
    object lifecycle extends OutWatchLifeCycleAttributes
  }
  object styles extends Styles {
    object extra extends StylesExtra
  }
  object events {
    object window extends WindowEvents
    object document extends DocumentEvents
  }
}

object dsl extends dslEffect[cats.effect.IO]
