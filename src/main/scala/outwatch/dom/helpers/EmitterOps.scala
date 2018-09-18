package outwatch.dom.helpers

import org.scalajs.dom.{Element, Event, html, svg}
import outwatch.dom.{Emitter, Hook}

trait EmitterOps {

  implicit class EventActions[O <: Event, R](builder: EmitterBuilder[O, R]) {
    def preventDefault: EmitterBuilder[O, R] = builder.map { e => e.preventDefault; e }
    def stopPropagation: EmitterBuilder[O, R] = builder.map { e => e.stopPropagation; e }
    def stopImmediatePropagation: EmitterBuilder[O, R] = builder.map { e => e.stopImmediatePropagation; e }
  }

  implicit class TargetAsInput[O <: Event](builder: EmitterBuilder[O, Emitter]) {

    object target {
      def value: EmitterBuilder[String, Emitter] = builder.map(_.target.asInstanceOf[html.Input].value)

      def valueAsNumber: EmitterBuilder[Double, Emitter] = builder.map(_.target.asInstanceOf[html.Input].valueAsNumber)

      def checked: EmitterBuilder[Boolean, Emitter] = builder.map(_.target.asInstanceOf[html.Input].checked)
    }

    def value: EmitterBuilder[String, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].value)

    def valueAsNumber: EmitterBuilder[Double, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].valueAsNumber)

    def checked: EmitterBuilder[Boolean, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].checked)

  }

  implicit class TypedElements[O <: Element, H](builder: EmitterBuilder[O, H]) {
    def asHtml: EmitterBuilder[html.Element, H] = builder.map(_.asInstanceOf[html.Element])

    def asSvg: EmitterBuilder[svg.Element, H] = builder.map(_.asInstanceOf[svg.Element])
  }

  implicit class TypedElementTuples[E <: Element, H](builder: EmitterBuilder[(E,E), H]) {
    def asHtml: EmitterBuilder[(html.Element, html.Element), H] = builder.map(_.asInstanceOf[(html.Element, html.Element)])

    def asSvg: EmitterBuilder[(svg.Element, svg.Element), H] = builder.map(_.asInstanceOf[(svg.Element, svg.Element)])
  }
}
