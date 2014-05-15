package mesosphere.servicenet.http.json

import mesosphere.servicenet.dsl._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.net.{ InetAddress, Inet4Address, Inet6Address }

trait DocProtocol {

  // InetAddress, Inet4Address, Inet6Address, Inet6Subnet

  implicit val inet4AddressFormat = new Format[Inet4Address] {
    def writes(addr: Inet4Address): JsValue = ???
    def reads(json: JsValue): JsResult[Inet4Address] = ???
  }

  implicit val inet6AddressFormat = new Format[Inet6Address] {
    def writes(addr: Inet6Address): JsValue = ???
    def reads(json: JsValue): JsResult[Inet6Address] = ???
  }

  implicit val inetAddressFormat = new Format[InetAddress] {
    def writes(addr: InetAddress): JsValue = addr match {
      case a: Inet4Address => inet4AddressFormat.writes(a)
      case a: Inet6Address => inet6AddressFormat.writes(a)
      case _               => ???
    }

    def reads(json: JsValue): JsResult[InetAddress] = ???
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
