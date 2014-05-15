package mesosphere.servicenet.http.json

import mesosphere.servicenet.dsl._
import mesosphere.servicenet.util.InetAddressHelper
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.{ Try, Success, Failure }
import java.net.{ InetAddress, Inet4Address, Inet6Address }

trait DocProtocol {

  // InetAddress, Inet4Address, Inet6Address, Inet6Subnet

  implicit val inet4AddressFormat = new Format[Inet4Address] {
    def writes(addr: Inet4Address): JsValue = JsString(addr.getHostAddress)
    def reads(json: JsValue): JsResult[Inet4Address] = json match {
      case JsString(addr) =>
        Try(InetAddressHelper.ipv4(addr)) match {
          case Success(ip)    => JsSuccess(ip)
          case Failure(cause) => JsError("Malformed address.")
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
          case Failure(cause) => JsError("Malformed address.")
        }
      case _ => JsError("Address must be a string")
    }
  }

  implicit val inet6SubnetFormat = Json.format[Inet6Subnet]

  // Interface

  implicit val interfaceFormat = new Format[Interface] {
    def writes(interface: Interface): JsValue = ???
    def reads(json: JsValue): JsResult[Interface] = ???
  }

  // DNS

  implicit val dnsFormat = new Format[DNS] {
    def writes(dns: DNS): JsValue = ???
    def reads(json: JsValue): JsResult[DNS] = ???
  }

  // NAT

  implicit val natFormat = new Format[NAT] {
    def writes(nat: NAT): JsValue = ???
    def reads(json: JsValue): JsResult[NAT] = ???
  }

  // Tunnel

  implicit val tunnelFormat = new Format[Tunnel] {
    def writes(tunnel: Tunnel): JsValue = ???
    def reads(json: JsValue): JsResult[Tunnel] = ???
  }

  // Add, Remove, Change, Diff

  /*
  implicit val changeFormat = new Format[Change[_]] {
    def writes(change: Change[_]): JsValue = ???
    def reads(json: JsValue): JsResult[Change[_]] = ???
  }

  implicit val diffFormat = Json.format[Diff]  
*/

  // Doc

  implicit val docFormat = Json.format[Doc]

}

object DocProtocol extends DocProtocol
