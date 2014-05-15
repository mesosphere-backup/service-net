package mesosphere.servicenet.http

import mesosphere.servicenet.http.json.DocProtocol
import play.api.libs.json.Json
import unfiltered.jetty.Http
import unfiltered.request._
import unfiltered.response._

class HTTPServer extends DocProtocol {

  def run(port: Int): Unit = {
    object RestRoutes extends unfiltered.filter.Plan {
      def intent = {
        case Path(Seg(p :: Nil)) => ResponseString(p)
      }
    }

    Http(port).filter(RestRoutes).run
  }
}

object HTTPServer extends App {
  val server = new HTTPServer
  val defaultPort = 9000
  server.run(defaultPort) // TODO: get port from configuration
}
