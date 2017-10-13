package outwatch.http

import cats.effect.IO
import outwatch.dom.VDomHttp
import rxscalajs.Observable
import rxscalajs.dom.{Request, Response}

object Http {
  sealed trait HttpRequestType
  case object Get extends HttpRequestType
  case object Post extends HttpRequestType
  case object Delete extends HttpRequestType
  case object Put extends HttpRequestType
  case object Options extends HttpRequestType
  case object Head extends HttpRequestType

  def single(request: Request, method: HttpRequestType): IO[Response] = IO.async { cb =>
    Observable.ajax(request.copy(method = method.toString))
      .subscribe(response => cb(Right(response)), err => cb(Left(new Exception(err.toString))))
  }

  def singleWith[A](a: A, method: HttpRequestType)(f: Observable[A] => Observable[Request]): IO[Response] =
    IO.async { cb =>
      f(Observable.just(a))
        .flatMap(data => Observable.ajax(data.copy(method = method.toString)))
        .subscribe(response => cb(Right(response)), err => cb(Left(new Exception(err.toString))))
    }

  private def request(observable: Observable[Request], requestType: HttpRequestType): VDomHttp =
    VDomHttp(IO(observable.switchMap(data => Observable.ajax(data.copy(method = requestType.toString))).share))

  private def requestWithUrl(urls: Observable[String], requestType: HttpRequestType): VDomHttp =
    request(urls.map(url => Request(url)), requestType: HttpRequestType)

  def getWithUrl(urls: Observable[String]): VDomHttp = requestWithUrl(urls, Get)

  def get(requests: Observable[Request]): VDomHttp = request(requests, Get)

  def post(requests: Observable[Request]): VDomHttp = request(requests, Post)

  def delete(requests: Observable[Request]): VDomHttp = request(requests, Delete)

  def put(requests: Observable[Request]): VDomHttp = request(requests, Put)

  def options(requests: Observable[Request]): VDomHttp = request(requests, Options)

  def head(requests: Observable[Request]): VDomHttp = request(requests, Head)


}
