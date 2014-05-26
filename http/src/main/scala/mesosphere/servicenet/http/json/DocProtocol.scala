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
      case JsString(s) => Try(Inet6Subnet.parse(s)) match {
        case Success(net)   => JsSuccess(net)
        case Failure(cause) => JsError("Malformed subnet")
      }
      case _ => JsError("Address must be a string")
    }
  }

  // Interface

  implicit val interfaceFormat: Format[Interface] = (
    (__ \ "name").format[String] and
    (__ \ "addrs").format[Seq[Inet6Address]]
  )(Interface.apply(_, _), unlift(Interface.unapply))

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

  implicit val natFormat = Json.format[NATFan]

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

  case class Patch(
    op: String,
    path: String,
    value: JsValue)

  implicit val patchFormat = Json.format[Patch]

  implicit val diffFormat = new Format[Diff] {
    def writes(diff: Diff): JsValue = {
      val iPatches = diff.interfaces.map {
        case Add(iface)    => Patch("add", "interfaces", Json.toJson(iface))
        case Remove(label) => Patch("remove", "interfaces", JsString(label))
      }

      val dPatches = diff.dns.map {
        case Add(record)   => Patch("add", "dns", Json.toJson(record))
        case Remove(label) => Patch("remove", "dns", JsString(label))
      }

      val nPatches = diff.natFans.map {
        case Add(nat)      => Patch("add", "natFans", Json.toJson(nat))
        case Remove(label) => Patch("remove", "natFans", JsString(label))
      }

      val tPatches = diff.tunnels.map {
        case Add(tunnel)   => Patch("add", "tunnels", Json.toJson(tunnel))
        case Remove(label) => Patch("remove", "tunnels", JsString(label))
      }

      val allPatches = iPatches ++ dPatches ++ nPatches ++ tPatches

      JsArray(allPatches.map(Json.toJson(_)))
    }

    def reads(json: JsValue): JsResult[Diff] =
      json.validate[Seq[Patch]].flatMap { patches: Seq[Patch] =>
        try {
          JsSuccess(
            Diff(
              patches.collect {
                case Patch("add", "interfaces", js) =>
                  js.validate[Interface].map(Add(_)).get
                case Patch("remove", "interfaces", js) =>
                  js.validate[String].map(Remove(_)).get
              },
              patches.collect {
                case Patch("add", "dns", js) =>
                  js.validate[DNS].map(Add(_)).get
                case Patch("remove", "dns", js) =>
                  js.validate[String].map(Remove(_)).get
              },
              patches.collect {
                case Patch("add", "natFans", js) =>
                  js.validate[NATFan].map(Add(_)).get
                case Patch("remove", "natFans", js) =>
                  js.validate[String].map(Remove(_)).get
              },
              patches.collect {
                case Patch("add", "tunnels", js) =>
                  js.validate[Tunnel].map(Add(_)).get
                case Patch("remove", "tunnels", js) =>
                  js.validate[String].map(Remove(_)).get
              }
            )
          )
        }
        catch {
          case JsResultException(errors) => JsError(errors)
        }
      }

  }

  // Doc

  implicit val docFormat = Json.format[Doc]

}

/**
  * Custom JSON (de)serializer logic for Service Net DSL types.
  */
object DocProtocol extends DocProtocol
