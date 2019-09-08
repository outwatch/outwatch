package outwatch.reactive

trait HandlerEnvironment[SinkT[-_], SourceT[+_], HandlerT[_], ProHandlerT[-_, +_]] {

  type HandlerSink[-T] = SinkT[T]
  type HandlerSource[+T] = SourceT[T]

  type Handler[T] = HandlerT[T]
  type ProHandler[-I, +O] = ProHandlerT[I,O]

  val Handler: HandlerFactory[Handler]
  val ProHandler: ProHandlerFactory[ProHandler]

  val HandlerSink: SinkFactory[HandlerSink]
  val HandlerSource: SourceFactory[HandlerSource]
}

object HandlerEnvironment {
  def apply[SinkT[-_] : LiftSink, SourceT[+__] : LiftSource, HandlerT[_] : CreateHandler, ProHandlerT[-_,+_] : CreateProHandler]: HandlerEnvironment[SinkT, SourceT, HandlerT, ProHandlerT] =
    new HandlerEnvironment[SinkT, SourceT, HandlerT, ProHandlerT] {
      val Handler = new HandlerFactory[Handler]
      val ProHandler = new ProHandlerFactory[ProHandler]

      val HandlerSink = new SinkFactory[HandlerSink]
      val HandlerSource = new SourceFactory[HandlerSource]
    }
}
