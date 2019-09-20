package outwatch.reactive

object UnhandledErrorReporter {
  private[reactive] val errorSubject = SinkSourceHandler.publish[Nothing]
  @inline def error: SourceStream[Nothing] = errorSubject
}
