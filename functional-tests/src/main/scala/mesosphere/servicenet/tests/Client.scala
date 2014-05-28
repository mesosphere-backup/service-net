package mesosphere.servicenet.tests

import com.github.theon.uri.Uri._
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.{ RequestBuilder, Http }
import java.net.InetSocketAddress
import com.twitter.conversions.time.longToTimeableNumber
import com.twitter.finagle.{ Service, SimpleFilter }
import org.jboss.netty.handler.codec.http.{ HttpResponse, HttpRequest }
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import com.twitter.util.{ Stopwatch, Await, Future }
import com.github.theon.uri.Uri
import mesosphere.servicenet.util.{ Logging, Properties }

case class TestRequestResponse(
  requestNumber: Int,
  responseServerIp: String)

class Client {

  /**
    * Convert HTTP 4xx and 5xx class responses into Exceptions.
    */
  class HandleErrors extends SimpleFilter[HttpRequest, HttpResponse] {
    def apply(request: HttpRequest, service: Service[HttpRequest, HttpResponse]) = {
      service(request) flatMap { response =>
        response.getStatus match {
          case OK => Future.value(response)
          case _  => Future.exception(new Exception(response.getStatus.getReasonPhrase))
        }
      }
    }
  }

  private val hostname = Properties.underlying.getOrElse("test.client.connect.hostname", "::1")
  private val port = Properties.underlying.getOrElse("test.client.connect.port", "9797").toInt
  lazy val builder = ClientBuilder()
    .codec(Http())
    .hosts(new InetSocketAddress(hostname, port))
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
    RequestBuilder()
      .url(s"http://[$hostname]:$port/${uri.toString()}".replaceAll("(?<!:)//", "/"))
      .buildGet()
  }

  def ping(requestNumber: Int = 0) = {
    client(get("/ping" ? ("requestNumber" -> requestNumber))) flatMap {
      case response =>
        Future.value(new TestRequestResponse(requestNumber, response.headers().get("ServerIP")))
    }
  }
}

object Client extends App with Logging {
  private val client: Client = new Client()

  val requestCount = 10000

  log.info("submitting {} requests", requestCount)
  val stopwatch = Stopwatch.start()
  val f = {
    Future.collect(
      0 to requestCount map {
        i => client.ping(i)
      }
    ) onFailure {
        case t: Throwable =>
          println(s"t = $t")
      }
  }

  val resp = Await.result(f)
  log.info("all {} requests complete in: {}", requestCount, stopwatch())

  resp.foreach {
    case testResponse =>
      assert(testResponse.responseServerIp == "::1")
  }

  System.exit(0)
}
