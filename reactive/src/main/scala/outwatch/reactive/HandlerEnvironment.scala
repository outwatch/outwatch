package outwatch.reactive

import colibri._

class HandlerEnvironment[SinkT[-_] : LiftSink, SourceT[+__] : LiftSource, HandlerT[_] : CreateSubject, ProHandlerT[-_,+_] : CreateProSubject] {

  type HandlerSink[-T] = SinkT[T]
  type HandlerSource[+T] = SourceT[T]

  type Handler[T] = HandlerT[T]
  type ProHandler[-I, +O] = ProHandlerT[I,O]

  val Handler: HandlerFactory[Handler] = new HandlerFactory[Handler]
  val ProHandler: ProHandlerFactory[ProHandler] = new ProHandlerFactory[ProHandler]

  val HandlerSink: SinkFactory[HandlerSink] = new SinkFactory[HandlerSink]
  val HandlerSource: SourceFactory[HandlerSource] = new SourceFactory[HandlerSource]
}
