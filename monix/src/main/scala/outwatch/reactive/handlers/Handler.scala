package outwatch.reactive.handlers

import _root_.monix.reactive.{Observable, Observer}

import colibri.ext.monix._
import outwatch.reactive._

@deprecated("Use monix.reactive.PublishSubject[T] or .ReplaySubject[T] or .BehaviourSubject[T](seed) instead", "")
object monix extends HandlerEnvironment[Observer, Observable, MonixSubject, MonixProSubject]
