import rxscalajs.Observable

package object outwatch {
  type Pipe[-I, +O] = Observable[O] with Sink[I] with PipeOps[I, O]
  type Handler[T] = Pipe[T,T]
}
