package outwatch.reactive

object UnhandledErrorReporter {
  private[outwatch] val errorSubject = SinkSourceHandler.publish[Throwable]
  @inline def error: SourceStream[Throwable] = errorSubject
}
