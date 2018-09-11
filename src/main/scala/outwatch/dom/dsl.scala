package outwatch.dom

object dsl extends Attributes with Tags with Styles {
  object tags extends Tags {
    object extra extends TagsExtra
    object svg extends TagsSvg
  }
  object attributes extends Attributes {
    object attrs extends HtmlAttrs
    object events extends Events
    object outwatch extends OutwatchAttributes
    object lifecycle extends OutWatchLifeCycleAttributes
    object svg extends SvgAttrs
  }
  object svg extends SvgAttrs with TagsSvg
  object styles extends Styles {
    object extra extends StylesExtra
  }
  object events {
    object window extends WindowEvents
    object document extends DocumentEvents
  }
}
