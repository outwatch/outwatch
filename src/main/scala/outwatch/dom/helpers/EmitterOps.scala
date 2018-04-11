package outwatch.dom.helpers

import cats.effect.Effect
import org.scalajs.dom.{Element, Event, html, svg}
import outwatch.dom.{Emitter, Hook}

trait EmitterOps[F[+_]] {
  implicit val effectF:Effect[F]

  implicit class TargetAsInput[E <: Event, O <: Event](builder: EmitterBuilder[F, E, O, Emitter]) {

    object target {
      def value: EmitterBuilder[F, E, String, Emitter] = builder.map(_.target.asInstanceOf[html.Input].value)

      def valueAsNumber: EmitterBuilder[F, E, Double, Emitter] = builder.map(_.target.asInstanceOf[html.Input].valueAsNumber)

      def checked: EmitterBuilder[F, E, Boolean, Emitter] = builder.map(_.target.asInstanceOf[html.Input].checked)
    }

    def value: EmitterBuilder[F, E, String, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].value)

    def valueAsNumber: EmitterBuilder[F, E, Double, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].valueAsNumber)

    def checked: EmitterBuilder[F, E, Boolean, Emitter] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].checked)

  }

  implicit class TypedElements[E <: Element, H <: Hook[Element]](builder: EmitterBuilder[F, E, E, H]) {
    def asHtml: EmitterBuilder[F, E, html.Element, H] = builder.map(_.asInstanceOf[html.Element])

    def asSvg: EmitterBuilder[F, E, svg.Element, H] = builder.map(_.asInstanceOf[svg.Element])
  }

  implicit class TypedElementTuples[E <: Element, H <: Hook[(Element,Element)]](builder: EmitterBuilder[F, (E,E), (E,E), H]) {
    def asHtml: EmitterBuilder[F, (E,E), (html.Element, html.Element), H] = builder.map(_.asInstanceOf[(html.Element, html.Element)])

    def asSvg: EmitterBuilder[F, (E,E), (svg.Element, svg.Element), H] = builder.map(_.asInstanceOf[(svg.Element, svg.Element)])
  }
}
