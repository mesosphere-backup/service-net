package mesosphere.servicenet.tests

import unfiltered.request.{ GET, Seg, Path }
import unfiltered.response.{ ResponseString, ResponseHeader }
import unfiltered.jetty.Http

object Server extends App {

  private val addr = "::1"
  private val port = 9797

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
