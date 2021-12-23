package outwatch.reactive

import colibri._

@deprecated("Use colibri.Subject.publish[T] or .replay[T] or .behavior[T](seed) instead", "")
object handler extends HandlerEnvironment[Observer, Observable, Subject, ProSubject]
