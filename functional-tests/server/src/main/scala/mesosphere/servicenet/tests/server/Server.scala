package mesosphere.servicenet.tests.server

import unfiltered.jetty.Http
import unfiltered.request.{ Path, Seg, GET }
import unfiltered.response.{ ResponseHeader, ResponseString }

object Server extends App {

  private val interface = "::1"
  private val port = 9797

  object Route extends unfiltered.filter.Plan {
    def intent = {
      case req @ Path(Seg(p :: Nil)) => req match {
        case GET(_) =>
          ResponseHeader("Server", Set(s"[$interface]:$port")) ~>
            ResponseString(p)
      }
    }
  }

  new Http(port, interface).filter(Route).run()
}
