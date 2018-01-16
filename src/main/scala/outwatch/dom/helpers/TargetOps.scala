package outwatch.dom.helpers

import org.scalajs.dom.{Event, html}
import outwatch.dom.Emitter

trait TargetOps {

  implicit class TargetAsInput[E <: Event, O <: Event](builder: EmitterBuilder[E, O, Emitter]) {

    object target {
      def value: EmitterBuilder[E, String, Emitter] = builder.map(_.target.asInstanceOf[html.Input].value)

      def valueAsNumber: EmitterBuilder[E, Int, Emitter] = builder.map(_.target.asInstanceOf[html.Input].valueAsNumber)

      def checked: EmitterBuilder[E, Boolean, Emitter] = builder.map(_.target.asInstanceOf[html.Input].checked)
    }

    def value: EmitterBuilder[E, String, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].value)

    def valueAsNumber: EmitterBuilder[E, Int, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].valueAsNumber)

    def checked: EmitterBuilder[E, Boolean, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].checked)
  }

}
