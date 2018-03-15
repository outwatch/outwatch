package outwatch.dom

import cats.Applicative
import cats.effect.{Effect, IO}

trait dsl[F[+_]] extends Styles[F] with Tags[F] with Attributes { thisDsl =>
  implicit def effectF: Effect[F]
  implicit def applicativeF: Applicative[F] = effectF

  type VNode = VNodeF[F]
  type VDomModifier = VDomModifierF[F]

  object tags extends Tags[F] with TagBuilder[F] {
    implicit val effectF: Effect[F] = thisDsl.effectF
    object extra extends TagsExtra[F] with TagBuilder[F] {
      implicit val effectF: Effect[F] = thisDsl.effectF
    }
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



object dsl extends dsl[IO] with TagsCompat {
  implicit val effectF: Effect[IO] = IO.ioEffect
}

//TODO:
//object dslId extends dsl[Id] with TagsCompat {
//  implicit val effectF: Effect[Id] = Id.
//  implicit val applicativeF: Applicative[Id] = Applicative.apply
//}
