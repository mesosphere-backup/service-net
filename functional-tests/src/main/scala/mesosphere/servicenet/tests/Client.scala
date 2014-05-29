package mesosphere.servicenet.tests

import java.net.InetSocketAddress

import com.github.theon.uri.Uri._
import com.github.theon.uri.Uri
import com.twitter.conversions.time.longToTimeableNumber
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.{ RequestBuilder, Http }
import com.twitter.finagle.{ Service, SimpleFilter }
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.{ HttpResponse, HttpRequest }
import org.jboss.netty.handler.codec.http.HttpResponseStatus._

import mesosphere.servicenet.util.{ Logging, Properties }

case class TestRequestResponse(requestNumber: Int,
                               responseServerIp: String)

class Client(hostname: String, port: Int) extends Logging {

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

  val address = new InetSocketAddress(hostname, port)

  lazy val builder = ClientBuilder()
    .codec(Http())
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
    val uriString = s"http://$hostname:$port/${uri.toString()}"
    RequestBuilder()
      .url(uriString.replaceAll("(?<!:)//", "/"))
      .buildGet()
  }

  def ping(requestNumber: Int = 0) = {
    client(get("/ping" ? ("requestNumber" -> requestNumber))) flatMap {
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

object Client extends App with Logging {

  val client: Client = args(0).split(":") match {
    case Array(hostname, port) => new Client(hostname, port.toInt)
    case Array(hostname)       => new Client(hostname, 80)
    case _ => throw new IllegalArgumentException(
      "Missing host arg.\n Usage: Client <hostname>[:port]"
    )
  }

  val requestCount = Properties.underlying.getOrElse(
    "test.client.balanceTest.request.count",
    "10000"
  ).toInt
  val expectBalanceFactor = Properties.underlying.getOrElse(
    "test.client.balanceTest.expect.balanceFactor",
    "100.0"
  ).toDouble
  val expectBalanceFactorDelta = Properties.underlying.getOrElse(
    "test.client.balanceTest.expect.balanceFactor.delta",
    "0.1"
  ).toDouble

  val testResults = new BalanceFactorTest(client).runBalanceFactorTest(
    requestCount,
    expectBalanceFactor,
    expectBalanceFactorDelta
  )

  val dash80 =
    "----------------------------------------" +
      "----------------------------------------"

  log.info("Results:")
  log.info(dash80)
  testResults.allResults.foreach {
    case result =>
      log.info(s" *  ${result.serverIp} -> ${result.percentage}")
  }
  log.info(dash80)

  val expectReportString = s"$expectBalanceFactor(+/-$expectBalanceFactorDelta)"
  if (testResults.unbalancedResults.nonEmpty) {
    log.error("Unbalanced:")
    log.error(dash80)
    testResults.unbalancedResults.foreach {
      case result =>
        val message =
          s" *  ${result.serverIp} -> " +
            s"expected: $expectReportString, " +
            s"actual: ${result.percentage}"
        log.error(message)
    }
    log.error(dash80)
  }
  else {
    log.info(s"Balanced at $expectReportString")
  }

  println(JacksonWrapper.serialize(testResults))
  if (testResults.pass) {
    System.exit(0)
  }
  else {
    System.exit(5)
  }
}
