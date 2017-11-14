import rxscalajs.Observable

package object outwatch {

  type Handler[-I, +O] = Observable[O] with Sink[I] with HandlerOps[I, O]

}
