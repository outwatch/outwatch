package outwatch.reactive

import scala.scalajs.js

trait SinkSourceHandler[-I, +O] extends SinkObserver[I] with SourceStream[O]

class SinkSourceVariable[I, O](private var current: js.UndefOr[O], convert: I => O) extends SinkSourceHandler[I, O] {

  private val subscribers = new js.Array[SinkObserver[O]]

  def onNext(value: I): Unit = {
    val converted = convert(value)
    current = converted
    subscribers.foreach(_.onNext(converted))
  }

  def onError(error: Throwable): Unit = subscribers.foreach(_.onError(error))

  def subscribe[G[_] : Sink](sink: G[_ >: O]): Subscription = {
    val observer = SinkObserver.lift(sink)
    current.foreach(observer.onNext)
    subscribers.push(observer)
    Subscription(() => JSArrayHelper.removeElement(subscribers)(observer))
  }
}

class SinkSourcePublisher[I, O](convert: I => O) extends SinkSourceHandler[I, O] {

  private val subscribers = new js.Array[SinkObserver[O]]

  def onNext(value: I): Unit = {
    val converted = convert(value)
    subscribers.foreach(_.onNext(converted))
  }

  def onError(error: Throwable): Unit = subscribers.foreach(_.onError(error))

  def subscribe[G[_] : Sink](sink: G[_ >: O]): Subscription = {
    val observer = SinkObserver.lift(sink)
    subscribers.push(observer)
    Subscription(() => JSArrayHelper.removeElement(subscribers)(observer))
  }
}

@inline class SinkSourceCombinator[SI[_] : Sink, SO[_] : Source, I, O](sink: SI[I], source: SO[O]) extends SinkSourceHandler[I, O] {

  @inline def onNext(value: I): Unit = Sink[SI].onNext(sink)(value)

  @inline def onError(error: Throwable): Unit = Sink[SI].onError(sink)(error)

  @inline def subscribe[G[_] : Sink](sink: G[_ >: O]): Subscription = Source[SO].subscribe(source)(sink)
}

object SinkSourceHandler {
  type Simple[T] = SinkSourceHandler[T,T]

  def apply[O]: SinkSourceHandler.Simple[O] = new SinkSourceVariable[O, O](js.undefined, identity)
  def apply[O](seed: O): SinkSourceHandler.Simple[O] = new SinkSourceVariable[O, O](seed, identity)

  def map[I, O](convert: I => O): SinkSourceHandler[I, O] = new SinkSourceVariable[I, O](js.undefined, convert)
  def map[I, O](seed: I)(convert: I => O): SinkSourceHandler[I, O] = new SinkSourceVariable[I, O](convert(seed), convert)

  object publish {
    def apply[O]: SinkSourceHandler.Simple[O] = new SinkSourcePublisher[O, O](identity)

    def map[I, O](convert: I => O): SinkSourceHandler[I, O] = new SinkSourcePublisher[I, O](convert)
  }

  @inline def from[SI[_] : Sink, SO[_] : Source, I, O](sink: SI[I], source: SO[O]): SinkSourceHandler[I, O] = new SinkSourceCombinator[SI, SO, I, O](sink, source)

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
