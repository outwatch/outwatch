package outwatch.http

import org.scalajs.dom.raw.XMLHttpRequest
import rxscalajs.Observable

import scala.scalajs.js
import org.scalajs.dom

object Http {
  sealed trait HttpRequestType
  case object Get extends HttpRequestType
  case object Post extends HttpRequestType
  case object Delete extends HttpRequestType
  case object Put extends HttpRequestType
  case object Option extends HttpRequestType
  case object Head extends HttpRequestType

  private def request(observable: Observable[HttpRequest], requestType: HttpRequestType): Observable[HttpResponse] = {
    observable.switchMap(data => Observable.ajax(data.url).map(mapToResponse)).share
  }

  private def requestWithUrl(urls: Observable[String], requestType: HttpRequestType) = {
    request(urls.map(url => HttpRequest(url)), requestType: HttpRequestType)
  }

  private def mapToResponse(d: js.Dynamic) = {
    HttpResponse(
      d.response.asInstanceOf[String],
      d.status.asInstanceOf[Int],
      d.responseType.asInstanceOf[String])
  }


  def get(urls: Observable[String]) = requestWithUrl(urls, Get)

  def post(requests: Observable[HttpRequest]): Observable[js.Dynamic] =  {
    requests.switchMap(http => Observable.ajax(http.url)).share
  }

  def delete(requests: Observable[HttpRequest]): Observable[HttpResponse] = {
    request(requests, Delete).share
  }

  case class HttpRequest(url: String, data: String = "", timeout: Int = 0,
                         headers: Map[String, String] = Map.empty,
                         withCredentials: Boolean = false, responseType: String = "")

  case class HttpResponse(body: String, status: Int, responseType: String)

}
