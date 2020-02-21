package outwatch

import colibri._

package object reactive {
  val handler = HandlerEnvironment[Observer, Observable, Subject, ProSubject]
}
