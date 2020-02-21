package outwatch.ext

import _root_.monix.reactive.{Observable, Observer}

import colibri._
import colibri.ext.monix._
import outwatch.reactive._

package object monix {
  val handler = HandlerEnvironment[Observer, Observable, MonixSubject, MonixProSubject]
}
