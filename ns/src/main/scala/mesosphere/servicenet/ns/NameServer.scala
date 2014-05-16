package mesosphere.servicenet.ns

import mesosphere.servicenet
import mesosphere.servicenet.dsl.{ AAAA, Doc, Diff }
import mesosphere.servicenet.util.{ InetAddressHelper, Logging }
import akka.actor._
import akka.pattern.ask
import akka.io.IO
import akka.util.Timeout
import com.github.mkroli.dns4s
import com.github.mkroli.dns4s.akka.Dns
import com.github.mkroli.dns4s.dsl._
import com.github.mkroli.dns4s.section.{ QuestionSection, ResourceRecord => RR }
import com.github.mkroli.dns4s.section.resource.AAAAResource
import org.xbill.DNS
import org.xbill.DNS.SimpleResolver
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import java.net.{ InetAddress, Inet6Address }

/**
  * A simple DNS server
  *
  * See: http://www.ietf.org/rfc/rfc1035.txt
  *
  * Serves AAAA records present in the underlying
  * `mesosphere.servicenet.dsl.Doc`, and delegates to the host for all other
  * queries.
  */
class NameServer extends Logging {

  implicit val system = ActorSystem("NameServer")
  implicit val timeout = Timeout(5.seconds)
  implicit val executionContext = system.dispatcher

  protected[this] var networkDoc: Doc = Doc(
    interfaces = Nil,
    dns = Nil,
    nat = Nil,
    tunnels = Nil
  )

  // See: http://www.dnsjava.org/doc/org/xbill/DNS/ResolverConfig.html
  protected val underlying: SimpleResolver = new SimpleResolver()

  def resolve(query: dns4s.Message): Option[dns4s.Message] =
    query match {
      case Query(_) ~ Questions(QName(name) ~ TypeAAAA() :: Nil) =>
        log debug s"Received 'AAAA' query for [$name]"
        val answers: Seq[RR] = resolveFromDoc(name, network()).collect {
          case r: AAAA => r.addresses.map { address: Inet6Address =>
            RR(
              `class` = RR.`classIN`,
              name = r.label,
              rdata = AAAAResource(address),
              ttl = r.ttl,
              `type` = RR.`typeAAAA`
            )
          }
        }.flatten
        if (answers.nonEmpty)
          Some(Response ~ Questions(query.question: _*) ~ Answers(answers: _*))
        else
          None

      case _ => None
    }

  def delegate(query: dns4s.Message): Option[dns4s.Message] =
    Try {
      log debug s"Delegating query to host resolver: [$query]"
      val buffer: dns4s.MessageBuffer = query.apply().flipped
      val msg = new DNS.Message(buffer.getBytes(buffer.remaining).toArray)
      log debug s"Sending query to upstream name server: [$msg]"
      val response = underlying send msg
      log debug s"Received response from upstream name server: [$response]"
      toDns4s(response)
    }.toOption

  // TODO: this could be more efficient
  def resolveFromDoc(label: String, doc: Doc): Seq[servicenet.dsl.DNS] =
    doc.dns.filter { _.label == label }

  /**
    * A description of the underlying network topology.
    */
  def network(): Doc = networkDoc

  /**
    * Updates this name server with a new description of the underlying network.
    */
  def update(doc: Doc): Unit = synchronized { networkDoc = doc }

  /**
    * Updates this name server with changes to the underlying network.
    */
  def update(diff: Diff): Unit = synchronized { update(diff(networkDoc)) }

  class NameServerActor extends Actor {
    override def receive = {
      case msg: dns4s.Message =>
        resolve(msg) orElse delegate(msg) match {
          case Some(answer) => sender ! answer
          case None         => log debug s"No result for $msg"
        }
    }
  }

  def run(port: Int): Unit = {
    val nsHandler = system actorOf Props(new NameServerActor)
    IO(Dns) ? Dns.Bind(nsHandler, port) onComplete {
      case Success(bound) => log.info(s"Bound port [$port]")
      case Failure(cause) => {
        log.error(s"Failed to bind port [$port]", cause)
        system.shutdown
        sys.exit(1)
      }
    }
  }

  protected def toDns4s(msg: DNS.Message): dns4s.Message =
    dns4s.Message(dns4s.MessageBuffer().put(msg.toWire).flipped)

}

/**
  * Companion object for [[NameServer]].
  */
object NameServer extends App {
  val ns = new NameServer

  val testDoc = {
    val loopbackAddress =
      InetAddressHelper.ipv6("0000 0000 0000 0000 0000 0000 0000 0001")

    val anotherAddress =
      InetAddressHelper.ipv6("fc75:0000:0000:0000:0000:9fb2:0000:0804")

    Doc(
      interfaces = Nil,
      dns = Seq(
        AAAA("foo.bar", Seq(loopbackAddress)),
        AAAA("foo.bar", Seq(anotherAddress))
      ),
      nat = Nil,
      tunnels = Nil
    )
  }

  ns update testDoc
  ns.run(port = 8888) // TODO: get value for port from config
}
