package mesosphere.servicenet.http.json

import mesosphere.servicenet.dsl._
import mesosphere.servicenet.util.InetAddressHelper
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.{ Try, Success, Failure }
import java.net.{ InetAddress, Inet4Address, Inet6Address }

/**
  * Custom JSON (de)serializer logic for Service Net DSL types.
  */
trait DocProtocol {

  // InetAddress, Inet4Address, Inet6Address, Inet6Subnet

  implicit val inet4AddressFormat = new Format[Inet4Address] {
    def writes(addr: Inet4Address): JsValue = JsString(addr.getHostAddress)
    def reads(json: JsValue): JsResult[Inet4Address] = json match {
      case JsString(addr) =>
        Try(InetAddressHelper.ipv4(addr)) match {
          case Success(ip)    => JsSuccess(ip)
          case Failure(cause) => JsError("Malformed address")
        }
      case _ => JsError("Address must be a string")
    }
  }

  implicit val inet6AddressFormat = new Format[Inet6Address] {
    def writes(addr: Inet6Address): JsValue = JsString(addr.getHostAddress)
    def reads(json: JsValue): JsResult[Inet6Address] = json match {
      case JsString(addr) =>
        Try(InetAddressHelper.ipv6(addr)) match {
          case Success(ip)    => JsSuccess(ip)
          case Failure(cause) => JsError("Malformed address")
        }
      case _ => JsError("Address must be a string")
    }
  }

  implicit val inet6SubnetFormat = new Format[Inet6Subnet] {
    def writes(net: Inet6Subnet): JsValue = JsString(net.getCanonicalForm)
    def reads(json: JsValue): JsResult[Inet6Subnet] = json match {
      case JsString(s) => {
        val components = s.split('/')
        if (components.size != 2)
          return JsError("Subnet must be of form: <addr>/<length>")
        val Seq(addr, prefix): Seq[String] = components
        Try(InetAddressHelper.ipv6(addr)) flatMap { addr =>
          Try(Inet6Subnet(addr, prefix.toInt))
        } match {
          case Success(net)   => JsSuccess(net)
          case Failure(cause) => JsError("Malformed subnet")
        }
      }
      case _ => JsError("Address must be a string")
    }
  }

  // Interface

  implicit val addressOrSubnetFormat =
    new Format[Either[Inet6Address, Inet6Subnet]] {
      def writes(v: Either[Inet6Address, Inet6Subnet]): JsValue =
        v match {
          case Left(addr)    => inet6AddressFormat writes addr
          case Right(subnet) => inet6SubnetFormat writes subnet
        }
      def reads(json: JsValue): JsResult[Either[Inet6Address, Inet6Subnet]] =
        json match {
          case _: JsString =>
            inet6AddressFormat.reads(json).map(Left(_)) orElse
              inet6SubnetFormat.reads(json).map(Right(_))
          case _ => JsError("Address or subnet must be a string")
        }
    }

  implicit val interfaceFormat = Json.format[Interface]

  // DNS

  val aaaaFormat = Json.format[AAAA]

  implicit val dnsFormat = new Format[DNS] {
    def writes(dns: DNS): JsValue = dns match {
      case aaaa: AAAA => aaaaFormat.writes(aaaa)
    }
    def reads(json: JsValue): JsResult[DNS] =
      aaaaFormat.reads(json)
  }

  // NAT

  implicit val natFormat = Json.format[NAT]

  // Tunnel

  val tunnel6in4Format = Json.format[Tunnel6in4]

  implicit val tunnelFormat = new Format[Tunnel] {
    def writes(tunnel: Tunnel): JsValue = tunnel match {
      case t: Tunnel6in4 => tunnel6in4Format.writes(t)
    }
    def reads(json: JsValue): JsResult[Tunnel] =
      tunnel6in4Format.reads(json)
  }

  // Add, Remove, Change, Diff

  // See: http://tools.ietf.org/html/rfc6902
  //
  // For example:
  //
  // {
  //   "op": "add",
  //   "path": "interfaces",
  //   "value": { "label": "my-service", "addr": "fc75:0:0:0:0:9fb2:0:804" }
  // }
  //
  // {
  //   "op": "remove", "path": "interfaces", "value": "my-service"
  // }

  // implicit val networkEntityFormat = new Format[NetworkEntity] {
  //   def writes(entity: NetworkEntity): JsValue = ???
  //   def reads(json: JsValue): JsResult[NetworkEntity] = ???
  // }

  // case class Patch(
  //   op: String,
  //   path: String,
  //   value: Change[NetworkEntity])

  // implicit val patchFormat = Json.format[Patch]

  implicit val changeInterfaceFormat = new Format[Change[Interface]] {
    def writes(change: Change[Interface]): JsValue = ???
    def reads(json: JsValue): JsResult[Change[Interface]] = ???
  }

  implicit val changeDnsFormat = new Format[Change[DNS]] {
    def writes(change: Change[DNS]): JsValue = ???
    def reads(json: JsValue): JsResult[Change[DNS]] = ???
  }

  implicit val changeNatFormat = new Format[Change[NAT]] {
    def writes(change: Change[NAT]): JsValue = ???
    def reads(json: JsValue): JsResult[Change[NAT]] = ???
  }

  implicit val changeTunnelFormat = new Format[Change[Tunnel]] {
    def writes(change: Change[Tunnel]): JsValue = ???
    def reads(json: JsValue): JsResult[Change[Tunnel]] = ???
  }

  implicit val diffWrites: Writes[Diff] = (
    (__ \ "interfaces").write[Seq[Change[Interface]]] and
    (__ \ "dns").write[Seq[Change[DNS]]] and
    (__ \ "nat").write[Seq[Change[NAT]]] and
    (__ \ "tunnels").write[Seq[Change[Tunnel]]]
  )(unapply(Diff.unapply))

  implicit val diffReads: Reads[Diff] = (
    (__ \ "interfaces").read[Seq[Change[Interface]]] and
    (__ \ "dns").read[Seq[Change[DNS]]] and
    (__ \ "nat").read[Seq[Change[NAT]]] and
    (__ \ "tunnels").read[Seq[Change[Tunnel]]]
  )(Diff.apply(_, _, _, _))

  // Doc

  implicit val docFormat = Json.format[Doc]

}

/**
  * Custom JSON (de)serializer logic for Service Net DSL types.
  */
object DocProtocol extends DocProtocol
