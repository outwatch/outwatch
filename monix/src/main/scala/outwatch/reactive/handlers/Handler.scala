package outwatch.reactive.handlers

import _root_.monix.reactive.{Observable, Observer}

import colibri.ext.monix._
import outwatch.reactive._

object monix extends HandlerEnvironment[Observer, Observable, MonixSubject, MonixProSubject]
