package outwatch.dom

import cats.Applicative
import cats.effect.Effect

trait OutwatchDsl[F[+_]] extends Styles[F] with Tags[F] with Attributes[F] { thisDsl =>
  implicit val effectF: Effect[F]

  type VNode = VNodeF[F]
  type VDomModifier = VDomModifierF[F]

  object tags extends Tags[F] with TagBuilder[F] {
    implicit val effectF: Effect[F] = thisDsl.effectF
    object extra extends TagsExtra[F] with TagBuilder[F] {
      implicit val effectF: Effect[F] = thisDsl.effectF
    }
  }
  object attributes extends Attributes[F] {
    implicit val effectF: Effect[F] = thisDsl.effectF
    object attrs extends Attrs[F] {
      implicit val effectF: Effect[F] = thisDsl.effectF
    }
    object reflected extends ReflectedAttrs[F] {
      implicit val effectF: Effect[F] = thisDsl.effectF
    }
    object props extends Props[F] {
      implicit val effectF: Effect[F] = thisDsl.effectF
    }
    object events extends Events {
      implicit val effectF: Effect[F] = thisDsl.effectF
    }
    object outwatch extends OutwatchAttributes[F] {
      implicit val effectF: Effect[F] = thisDsl.effectF
    }
    object lifecycle extends OutWatchLifeCycleAttributes {
      implicit val effectF: Effect[F] = thisDsl.effectF
    }
  }
  object events {
    object window extends WindowEvents
    object document extends DocumentEvents
  }
}


