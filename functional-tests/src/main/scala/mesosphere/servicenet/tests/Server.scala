package mesosphere.servicenet.tests

import unfiltered.jetty.Http
import unfiltered.request.{ GET, Seg, Path }
import unfiltered.response.{ ResponseString, ResponseHeader }

import mesosphere.servicenet.util.Properties

object Server extends App {

  private val addr = Properties.underlying.getOrElse("http.ip", "::1")
  private val port = Properties.underlying.getOrElse("http.port", "9797").toInt

  object Route extends unfiltered.filter.Plan {
    def intent = {
      case req @ Path(Seg(p :: Nil)) => req match {
        case GET(_) =>
          ResponseHeader("ServerIP", Set(s"$addr")) ~>
            ResponseString(p)
      }
    }
  }

  new Http(port, addr).filter(Route).run()
}
