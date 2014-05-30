package mesosphere.servicenet.tests

import mesosphere.servicenet.util.{ Logging, Properties }
import scala.util.Random
import unfiltered.jetty.Http
import unfiltered.request.{ GET, Seg, Path }
import unfiltered.response._

object Server extends Logging {
  lazy val addr = Properties.underlying.getOrElse("http.ip", "::1")
  lazy val port = Properties.underlying.getOrElse("http.port", "9797").toInt

  object Route extends unfiltered.filter.Plan {

    lazy val data = {
      val array = new Array[Byte](10 * 1024 * 1024)
      Random.nextBytes(array)
      array
    }

    def intent = {
      case req @ Path(Seg(Nil)) => req match {
        case GET(_) =>
          ResponseHeader("ServerIP", Set(s"$addr")) ~>
            ContentType("application/octet-stream") ~>
            ContentLength(s"${data.length}") ~>
            ResponseBytes(data)
      }
    }
  }

  def main(args: Array[String]) {
    log.info(s"Starting server $addr:$port")
    new Http(port, addr).filter(Route).run()
  }
}
