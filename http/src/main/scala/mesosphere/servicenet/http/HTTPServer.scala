package mesosphere.servicenet.http

import play.api.libs.json._
import unfiltered.jetty.Http
import unfiltered.request._
import unfiltered.response._

import mesosphere.servicenet.dsl._
import mesosphere.servicenet.http.json.DocProtocol
import mesosphere.servicenet.util.Logging

class HTTPServer(updated: (Diff, Doc) => Unit = (diff: Diff, doc: Doc) => ())
    extends DocProtocol with Logging {

  @volatile protected var doc: Doc = Doc(Nil, Nil, Nil, Nil)

  def update(docPrime: Doc) = synchronized {
    val diff = Diff(doc, docPrime)
    log info Seq(
      "change counts //",
      s"interfaces=${diff.interfaces.size}",
      s"dns=${diff.dns.size}",
      s"natFans=${diff.natFans.size}",
      s"tunnels=${diff.tunnels.size}"
    ).mkString(" ")
    doc = docPrime
    updated(diff, doc)
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
