package outwatch.reactive

import scala.scalajs.js

trait SinkSourceHandler[-I, +O] extends SinkObserver[I] with SourceStream[O]

class SinkSourceVariable[I, O](private var current: Option[O], convert: I => O) extends SinkSourceHandler[I, O] {

  private var subscribers = new js.Array[SinkObserver[O]]
  private var isRunning = false

  def onNext(value: I): Unit = {
    isRunning = true
    val converted = convert(value)
    current = Some(converted)
    subscribers.foreach(_.onNext(converted))
    isRunning = false
  }

  def onError(error: Throwable): Unit = {
    isRunning = true
    subscribers.foreach(_.onError(error))
    isRunning = false
  }

  def subscribe[G[_] : Sink](sink: G[_ >: O]): Subscription = {
    val observer = SinkObserver.lift(sink)
    subscribers.push(observer)
    current.foreach(observer.onNext)
    Subscription { () =>
      if (isRunning) subscribers = subscribers.filter(_ != observer)
      else JSArrayHelper.removeElement(subscribers)(observer)
    }
  }
}

class SinkSourcePublisher[I, O](convert: I => O) extends SinkSourceHandler[I, O] {

  private var subscribers = new js.Array[SinkObserver[O]]
  private var isRunning = false

  def onNext(value: I): Unit = {
    isRunning = true
    val converted = convert(value)
    subscribers.foreach(_.onNext(converted))
    isRunning = false
  }

  def onError(error: Throwable): Unit = {
    isRunning = true
    subscribers.foreach(_.onError(error))
    isRunning = false
  }

  def subscribe[G[_] : Sink](sink: G[_ >: O]): Subscription = {
    val observer = SinkObserver.lift(sink)
    subscribers.push(observer)
    Subscription { () =>
      if (isRunning) subscribers = subscribers.filter(_ != observer)
      else JSArrayHelper.removeElement(subscribers)(observer)
    }
  }
}

@inline class SinkSourceCombinator[SI[_] : Sink, SO[_] : Source, I, O](sink: SI[I], source: SO[O]) extends SinkSourceHandler[I, O] {

  @inline def onNext(value: I): Unit = Sink[SI].onNext(sink)(value)

  @inline def onError(error: Throwable): Unit = Sink[SI].onError(sink)(error)

  @inline def subscribe[G[_] : Sink](sink: G[_ >: O]): Subscription = Source[SO].subscribe(source)(sink)
}

object SinkSourceHandler {
  type Simple[T] = SinkSourceHandler[T,T]

  def apply[O]: Simple[O] = new SinkSourceVariable[O, O](None, identity)
  def apply[O](seed: O): Simple[O] = new SinkSourceVariable[O, O](Some(seed), identity)

  def map[I, O](convert: I => O): SinkSourceHandler[I, O] = new SinkSourceVariable[I, O](None, convert)
  def map[I, O](seed: I)(convert: I => O): SinkSourceHandler[I, O] = new SinkSourceVariable[I, O](Some(convert(seed)), convert)

  object publish {
    def apply[O]: Simple[O] = new SinkSourcePublisher[O, O](identity)

    def map[I, O](convert: I => O): SinkSourceHandler[I, O] = new SinkSourcePublisher[I, O](convert)
  }

  @inline def from[SI[_] : Sink, SO[_] : Source, I, O](sink: SI[I], source: SO[O]): SinkSourceHandler[I, O] = new SinkSourceCombinator[SI, SO, I, O](sink, source)

  @inline implicit class Operations[I,O](val handler: SinkSourceHandler[I,O]) extends AnyVal {
    @inline def transformSource[S[_] : Source, O2](g: SourceStream[O] => S[O2]): SinkSourceHandler[I, O2] = from[SinkObserver, S, I, O2](handler, g(handler))
    @inline def transformSink[G[_] : Sink, I2](f: SinkObserver[I] => G[I2]): SinkSourceHandler[I2, O] = from[G, SourceStream, I2, O](f(handler), handler)
    @inline def transformHandler[G[_] : Sink, S[_] : Source, I2, O2](f: SinkObserver[I] => G[I2])(g: SourceStream[O] => S[O2]): SinkSourceHandler[I2, O2] = from(f(handler), g(handler))
  }

  object createHandler extends CreateHandler[Simple] {
    @inline def publisher[A]: SinkSourceHandler[A, A] = SinkSourceHandler.publish[A]
    @inline def variable[A]: SinkSourceHandler[A, A] = SinkSourceHandler.apply[A]
    @inline def variable[A](seed: A): SinkSourceHandler[A, A] = SinkSourceHandler.apply[A](seed)
  }
  object createProHandler extends CreateProHandler[SinkSourceHandler] {
    @inline def apply[I,O](f: I => O): SinkSourceHandler[I,O] = SinkSourceHandler.map(f)
    @inline def apply[I,O](seed: I)(f: I => O): SinkSourceHandler[I,O] = SinkSourceHandler.map(seed)(f)
    @inline def from[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): SinkSourceHandler[I, O] = SinkSourceHandler.from(sink, source)
  }
}
