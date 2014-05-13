package mesosphere.servicenet.ns

import mesosphere.servicenet.dsl.{ Doc, Diff }
import mesosphere.servicenet.util.Logging
import akka.actor._
import akka.pattern.ask
import akka.io.IO
import akka.util.Timeout
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.akka.Dns
import com.github.mkroli.dns4s.dsl._
import com.github.mkroli.dns4s.section.{ QuestionSection, ResourceRecord => RR }
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import java.net.{ InetAddress, Inet6Address }

/**
  * A simple DNS server
  *
  * See: http://www.ietf.org/rfc/rfc1035.txt
  *
  * Serves A, AAAA and CNAME records present in the underlying
  * `mesosphere.servicenet.dsl.Doc`, and delegates to the host for all other
  * queries.
  */
class NameServer extends Logging {

  implicit val system = ActorSystem("NameServer")
  implicit val timeout = Timeout(5.seconds)
  implicit val executionContext = system.dispatcher

  type Resolver = PartialFunction[Message, ComposableMessage]

  def resolve: Resolver = {
    case Query(_) ~ Questions(QName(name) ~ TypeA() :: Nil) =>
      log debug s"Received 'A' query for [$name]"
      Response ~ Questions(name) ~ Answers(ARecord("1.2.3.4"))

    case query @ Query(_) ~ Questions(QName(name) ~ TypeAAAA() :: Nil) =>
      log debug s"Received 'AAAA' query for [$name]"
      val address = ipv6Address("0000 0000 0000 0000 0000 0000 0000 0001")
      val qs = QuestionSection(name, RR.`typeAAAA`, RR.`classIN`)
      Response ~ Questions(qs) ~ Answers(AAAARecord(address))

    case Query(_) ~ Questions(QName(name) ~ TypeCNAME() :: Nil) =>
      log debug s"Received 'CNAME' query for [$name]"
      val cname = "mail"
      val qs = QuestionSection(name, RR.`typeCNAME`, RR.`classIN`)
      Response ~ Questions(qs) ~ Answers(CNameRecord(cname))
  }

  def delegate: Resolver = {
    case _ => ??? // TODO: resolve by some other means
  }

  /**
    * A description of the underlying network topology.
    */
  def network(): Doc = ???

  /**
    * Updates this name server with a new description of the underlying network.
    */
  def update(doc: Doc): Unit = update(network diff doc)

  /**
    * Updates this name server with a changes to the underlying network.
    */
  def update(diff: Diff): Unit = ???

  class NameServerActor extends Actor {
    override def receive = {
      case msg: Message => (resolve orElse delegate).lift.apply(msg) match {
        case Some(answer) => sender ! answer
        case None         => log debug s"No result for $msg, delegating query"
      }
    }
  }

  lazy val nsHandler = system actorOf Props(new NameServerActor)

  def start(port: Int): Unit = {
    IO(Dns) ? Dns.Bind(nsHandler, port) onComplete {
      case Success(bound) => log.info(s"Bound port [$port]")
      case Failure(cause) => {
        log.error(s"Failed to bind port [$port]", cause)
        system.shutdown
        sys.exit(1)
      }
    }
  }

  /**
    * Returns a canonical `java.net.Inet6Address` for the supplied address
    * string.
    *
    * @param addr An IPv6 Address (32 hexadecimal digits)
    */
  @throws[java.net.UnknownHostException]
  def ipv6Address(addr: String): Inet6Address =
    InetAddress.getByAddress(bytes(addr)).asInstanceOf[Inet6Address]

  protected[this] val hexChars: Set[Char] =
    (('0' to '9') ++ ('A' to 'Z') ++ ('a' to 'z')).toSet

  protected[this] def bytes(s: String): Array[Byte] =
    s.filter(hexChars).sliding(2, 2).map(BigInt(_, 16).toByte).toArray

}

/**
  * Companion object for [[NameServer]].
  */
object NameServer extends App {
  val ns = new NameServer
  ns.start(port = 8888) // TODO: get value for port from config
}
