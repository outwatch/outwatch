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

  case class Request(url: String, data: String = "", timeout: Int = 0,
                     headers: Map[String, String] = Map.empty,
                     withCredentials: Boolean = false, responseType: String = "")

  case class Response(body: String, status: Int, responseType: String)

  private def request(observable: Observable[Request], requestType: HttpRequestType): Observable[Response] = {
    observable.switchMap(data => Observable.ajax(data.url).map(mapToResponse)).share
  }

  private def requestWithUrl(urls: Observable[String], requestType: HttpRequestType) = {
    request(urls.map(url => Request(url)), requestType: HttpRequestType)
  }

  def get(urls: Observable[String]) = requestWithUrl(urls, Get)

  def getWithBody(requests: Observable[Request]) = request(requests, Get)

  def post(requests: Observable[Request]) = request(requests, Post)

  def delete(requests: Observable[Request]) = request(requests, Delete)

  def put(requests: Observable[Request]) = request(requests, Put)

  def option(requests: Observable[Request]) = request(requests, Option)

  def head(requests: Observable[Request]) = request(requests, Head)


  private def mapToResponse(d: js.Dynamic) = {
    Response(
      d.response.asInstanceOf[String],
      d.status.asInstanceOf[Int],
      d.responseType.asInstanceOf[String])
  }

}
