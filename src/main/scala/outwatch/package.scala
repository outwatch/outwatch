import rxscalajs.Observable

package object outwatch {

  type Handler[-I, +O] = Observable[O] with Sink[I]

}
