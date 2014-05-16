package mesosphere.servicenet.http

import mesosphere.servicenet.dsl._
import mesosphere.servicenet.http.json.DocProtocol
import play.api.libs.json._
import unfiltered.jetty.Http
import unfiltered.request._
import unfiltered.response._

class HTTPServer(updated: Doc => Unit = (doc: Doc) => ()) extends DocProtocol {

  @volatile protected var doc: Doc = Doc(Nil, Nil, Nil, Nil)

  def update(docPrime: Doc) = synchronized {
    doc = docPrime
  }

  object RestRoutes extends unfiltered.filter.Plan {
    def intent = {
      case req @ Path(Seg("doc" :: Nil)) => req match {

        case GET(_) =>
          ResponseHeader("Content-Type", Set("application/json")) ~>
            ResponseString(Json.toJson(doc).toString)

        case PUT(_) =>
          Json.parse(Body.bytes(req)).validate[Doc] match {
            case success: JsSuccess[_] =>
              update(success.get)
              ResponseString("OK")
            case error: JsError =>
              BadRequest ~>
                ResponseHeader("Content-Type", Set("application/json")) ~>
                ResponseString(JsError.toFlatJson(error).toString)
          }

        case PATCH(_) =>
          MethodNotAllowed ~> ResponseString("Must be GET or PUT")
      }

      case _ => NotFound ~> ResponseString("Not found")
    }
  }

  def run(port: Int): Unit =
    Http(port).filter(RestRoutes).run

}

object HTTPServer extends App {
  val server = new HTTPServer
  val defaultPort = 9000
  server.run(defaultPort) // TODO: get port from configuration
}
