package outwatch.http

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

  private def request(observable: Observable[Request], requestType: HttpRequestType): Observable[Response] = {
    observable.switchMap(data => Observable.ajax(data.copy(method = requestType.toString))).share
  }

  private def requestWithUrl(urls: Observable[String], requestType: HttpRequestType) = {
    request(urls.map(url => Request(url)), requestType: HttpRequestType)
  }

  def get(urls: Observable[String]) = requestWithUrl(urls, Get)

  def getWithBody(requests: Observable[Request]) = request(requests, Get)

  def post(requests: Observable[Request]) = request(requests, Post)

  def delete(requests: Observable[Request]) = request(requests, Delete)

  def put(requests: Observable[Request]) = request(requests, Put)

  def options(requests: Observable[Request]) = request(requests, Options)

  def head(requests: Observable[Request]) = request(requests, Head)


}
