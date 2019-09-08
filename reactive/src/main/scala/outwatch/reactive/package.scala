package outwatch

package object reactive {
  val handler = HandlerEnvironment[SinkObserver, SourceStream, SinkSourceHandler.Simple, SinkSourceHandler](SinkObserver.liftSink, SourceStream.liftSource, SinkSourceHandler.createHandler, SinkSourceHandler.createProHandler)
}
