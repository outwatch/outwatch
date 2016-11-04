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

  private def http[T](observable: Observable[T], url: String): Observable[js.Dynamic] = {
    observable.switchMap(t => Observable.ajax(url)).share
  }

  private def request(observable: Observable[HttpData], requestType: HttpRequestType): Observable[HttpResponse] = {
    observable.switchMap(data => Observable.ajax(data.url).map(mapToResponse _)).share
  }

  private def requestWithUrl(urls: Observable[String], requestType: HttpRequestType) = {
    request(urls.map(url => HttpData(url= url)), requestType: HttpRequestType)
  }

  private def mapToResponse(d: js.Dynamic) = {
    HttpResponse(d.response.asInstanceOf[String], d.status.asInstanceOf[Int])
  }


  def get(urlStream: Observable[String]) = requestWithUrl(urlStream, Get)

  def post(urlStream: Observable[HttpData]): Observable[js.Dynamic] =  {
    urlStream.switchMap(http => Observable.ajax(http.url)).share
  }

  def delete(stream: Observable[HttpData]): Observable[HttpResponse] = {
    request(stream, Delete).share
  }

  case class HttpData(url: String, data: String = "", timeout: Int = 0,
                      headers: Map[String, String] = Map.empty,
                      withCredentials: Boolean = false, responseType: String = "")

  case class HttpResponse(body: String, status: Int)

  /*
  val requests = createInputHandler()
    .debounceTime(300)

  val responses = Http.get(requests)
    .map(_.response.json)

  val progressSpinnerHidden = responses.mapTo(true)
    .merge(requests.mapTo(false))

  div(class:= "progress-spinner", hidden <= progressSpinnerHidden)
  */

}
