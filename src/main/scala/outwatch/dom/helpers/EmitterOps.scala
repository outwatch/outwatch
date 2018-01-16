package outwatch.dom.helpers

import org.scalajs.dom.{Element, Event, html, svg}
import outwatch.dom.{Emitter, Hook}

trait EmitterOps {

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

  implicit class TypedElements[E <: Element, H <: Hook[Element]](builder: EmitterBuilder[E, E, H]) {
    def asHtml: EmitterBuilder[E, html.Element, H] = builder.map(_.asInstanceOf[html.Element])

    def asSvg: EmitterBuilder[E, svg.Element, H] = builder.map(_.asInstanceOf[svg.Element])
  }

  implicit class TypedElementTuples[E <: Element, H <: Hook[(Element,Element)]](builder: EmitterBuilder[(E,E), (E,E), H]) {
    def asHtml: EmitterBuilder[(E,E), (html.Element, html.Element), H] = builder.map(_.asInstanceOf[(html.Element, html.Element)])

    def asSvg: EmitterBuilder[(E,E), (svg.Element, svg.Element), H] = builder.map(_.asInstanceOf[(svg.Element, svg.Element)])
  }
}
