package mesosphere.servicenet.tests

import mesosphere.servicenet.util.{ Logging, Properties }
import scala.util.Random
import unfiltered.jetty.Http
import unfiltered.request.{ GET, Seg, Path, Range }
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

    lazy val serverIP = ResponseHeader("ServerIP", Set(s"$addr"))

    val RangeBytes = "^bytes=([0-9]+)-([0-9]+)$".r

    def intent = {
      case req @ Path(Seg(Nil)) => req match {
        case GET(Range(List(s))) =>
          log info s"Range: $s"
          s match {
            case RangeBytes(begin, end) =>
              log info s"size: ${1 + end.toInt - begin.toInt}"
              response(1 + end.toInt - begin.toInt)
            case _ => serverIP ~> BadRequest ~>
              ResponseString("Malformed or multiple Range headers")
          }
        case GET(_) => response(data.size)
      }
    }

    def response(size: Int) = if (size <= data.size) {
      serverIP ~>
        ContentType("application/octet-stream") ~>
        ContentLength(s"$size") ~>
        ResponseBytes(data.slice(0, size))
    }
    else {
      serverIP ~> RequestedRangeNotSatisfiable ~>
        ResponseString(s"Please request at most ${data.size} bytes")
    }
  }

  def main(args: Array[String]) {
    log.info(s"Starting server $addr:$port")
    new Http(port, addr).filter(Route).run()
  }
}
