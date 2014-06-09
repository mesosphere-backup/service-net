package mesosphere.servicenet.tests

import java.net.InetSocketAddress

import com.github.theon.uri.Uri
import com.github.theon.uri.Uri._
import com.twitter.conversions.storage.longToStorageUnitableWholeNumber
import com.twitter.conversions.time.longToTimeableNumber
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.{ RequestBuilder, Http }
import com.twitter.finagle.{ Service, SimpleFilter }
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.{ HttpResponse, HttpRequest }
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import play.api.libs.json.Json

import mesosphere.servicenet.util.{ Logging, Properties }

case class TestRequestResponse(requestNumber: Int,
                               responseServerIp: String)

class Client(host: String, port: Int) extends Logging {

  /**
    * Convert HTTP 4xx and 5xx class responses into Exceptions.
    */
  class HandleErrors extends SimpleFilter[HttpRequest, HttpResponse] {
    def apply(
      request: HttpRequest,
      service: Service[HttpRequest, HttpResponse]) = {
      service(request) flatMap { response =>
        response.getStatus match {
          case OK => Future.value(response)
          case _ => Future.exception(
            new Exception(response.getStatus.getReasonPhrase)
          )
        }
      }
    }
  }

  val address = new InetSocketAddress(host, port)

  lazy val builder = ClientBuilder()
    .codec(Http().maxResponseSize(10.megabytes))
    .hosts(address)
    .tcpConnectTimeout(2.seconds)
    .requestTimeout(15.seconds)
    .hostConnectionLimit(30)

  lazy val client = {
    val newClient = new HandleErrors andThen builder.build()
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        newClient.close()
      }
    })
    newClient
  }

  private[this] def get(uri: Uri): HttpRequest = {
    val uriString = s"http://$host:$port/${uri.toString()}"
    RequestBuilder()
      .url(uriString.replaceAll("(?<!:)//", "/"))
      .buildGet()
  }

  def ping(requestNumber: Int = 0) = {
    client(get("/" ? ("requestNumber" -> requestNumber))) flatMap {
      case response =>
        Future.value(
          new TestRequestResponse(
            requestNumber,
            response.headers().get("ServerIP")
          )
        )
    }
  }
}

object Client extends Logging with BalanceFactorTestFormatters {
  def usage() = {
    System.err.println("Missing host arg.\n Usage: Client <hostname>[:port]")
    System.exit(2)
  }

  def main(args: Array[String]) = {
    val connection = {
      if (args.length == 0) None
      val HostAndPort = "^(.+)(?:[:]([0-9]+))$".r
      args(0) match {
        case HostAndPort(host, port) => Some(host, port.toInt)
        case host                    => Some(host, 80)
      }
    }

    val requestCount = Properties.underlying.getOrElse(
      "svcnet.test.requests",
      "100"
    ).toInt
    val balanceVariance = Properties.underlying.getOrElse(
      "svcnet.test.balance.variance",
      "0.05"
    ).toDouble

    connection match {
      case Some(t) =>
        val (host, port) = t
        log.info(s"svcnet.test.requests = $requestCount")
        log.info(s"svcnet.test.balance.variance = $balanceVariance")
        log.info(s"clientConnection = $host:$port")

        val client = new Client(host, port)
        val results = new BalanceFactorTest(client).runBalanceFactorTest(
          requestCount,
          balanceVariance
        )
        println(Json.prettyPrint(Json.toJson(results)))
        if (results.pass) System.exit(0) else System.exit(5)
      case _ => usage()
    }
  }
}
