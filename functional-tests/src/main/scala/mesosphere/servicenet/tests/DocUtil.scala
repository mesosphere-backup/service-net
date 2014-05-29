package mesosphere.servicenet.tests

import mesosphere.servicenet.dsl._
import mesosphere.servicenet.util.InetAddressHelper.{ipv4, ipv6}
import play.api.libs.json.Json
import mesosphere.servicenet.http.json.DocProtocol

class DocUtil {

  def createDoc(ipA: String, ipB: String, baseAddr: String = "2001:db8") = {
    new Doc(
      dns = Seq(
        AAAA(
          label = "all12.svc.dcc",
          localize = true,
          addrs = Seq(
            ipv6(s"$baseAddr:a::f:1"),
            ipv6(s"$baseAddr:b::f:1")
          )
        ),
        AAAA(
          label = "all34.svc.dcc",
          localize = true,
          addrs = Seq(
            ipv6(s"$baseAddr:a::f:2"),
            ipv6(s"$baseAddr:b::f:2")
          )
        )
      ),
      interfaces = Seq(
        Interface("task_a1", ipv6(s"$baseAddr:a::1")),
        Interface("task_a2", ipv6(s"$baseAddr:a::2")),
        Interface("task_a3", ipv6(s"$baseAddr:a::3")),
        Interface("task_a4", ipv6(s"$baseAddr:a::4")),

        Interface("task_b1", ipv6(s"$baseAddr:b::1")),
        Interface("task_b2", ipv6(s"$baseAddr:b::2")),
        Interface("task_b3", ipv6(s"$baseAddr:b::3")),
        Interface("task_b4", ipv6(s"$baseAddr:b::4")),

        Interface("all12_a", ipv6(s"$baseAddr:b::f:1")),
        Interface("all12_b", ipv6(s"$baseAddr:b::f:1")),
        Interface("all34_a", ipv6(s"$baseAddr:b::f:2")),
        Interface("all34_b", ipv6(s"$baseAddr:b::f:2"))
      ),
      natFans = Seq(
        NATFan(
          name = "all12_a",
          entrypoint = ipv6(s"$baseAddr:a::f:1"),
          endpoints = Seq(
            ipv6(s"$baseAddr:a::1"),
            ipv6(s"$baseAddr:a::2")
          )
        ),
        NATFan(
          name = "all12_b",
          entrypoint = ipv6(s"$baseAddr:b::f:1"),
          endpoints = Seq(
            ipv6(s"$baseAddr:b::1"),
            ipv6(s"$baseAddr:b::2")
          )
        ),
        NATFan(
          name = "all34_a",
          entrypoint = ipv6(s"$baseAddr:a::f:2"),
          endpoints = Seq(
            ipv6(s"$baseAddr:a::3"),
            ipv6(s"$baseAddr:a::4"),
            ipv6(s"$baseAddr:b::3"),
            ipv6(s"$baseAddr:b::4")
          )
        ),
        NATFan(
          name = "all34_b",
          entrypoint = ipv6(s"$baseAddr:b::f:2"),
          endpoints = Seq(
            ipv6(s"$baseAddr:a::3"),
            ipv6(s"$baseAddr:a::4"),
            ipv6(s"$baseAddr:b::3"),
            ipv6(s"$baseAddr:b::4")
          )
        )
      ),
      tunnels = Seq(
        Tunnel6in4(
          name = "tunnel_ab",
          addr = ipv6(s"$baseAddr:a::1000"),
          localEnd = ipv4(ipA),
          remoteEnd = ipv4(ipB),
          remoteIPv6Net = Inet6Subnet.parse(s"$baseAddr:b::/64")
        ),
        Tunnel6in4(
          name = "tunnel_ba",
          addr = ipv6(s"$baseAddr:b::1000"),
          localEnd = ipv4(ipB),
          remoteEnd = ipv4(ipA),
          remoteIPv6Net = Inet6Subnet.parse(s"$baseAddr:a::/64")
        )
      )
    )
  }

}

object DocUtil extends DocProtocol {
  def main(args: Array[String]) = {
    val doc = new DocUtil().createDoc("10.1.3.165", "10.1.3.166")
    val json = Json.toJson(doc).toString()

    println(s"json = $json")
  }
}
