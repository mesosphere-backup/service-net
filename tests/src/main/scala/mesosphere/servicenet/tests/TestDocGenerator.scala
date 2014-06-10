package mesosphere.servicenet.tests

import mesosphere.servicenet.dsl.{Doc, Inet6Subnet, Interface, Tunnel6in4}
import mesosphere.servicenet.http.json.DocProtocol
import mesosphere.servicenet.util.InetAddressHelper.{ipv4, ipv6}
import play.api.libs.json.Json

case class Host(
  interface: Interface,
  subnets: Seq[Subnet])
case class Subnet(
  interface: Interface,
  services: Seq[NetService])
case class NetService(
  interface: Interface,
  instances: Seq[Interface])

class TestDocGenerator {
  def generateDoc(
    hostCount: Int,
    subnetCount: Int,
    serviceCount: Int,
    hostIpv4Addresses: Seq[String] = Seq()) = {

    val numberOfHosts = math.max(hostCount, hostIpv4Addresses.length)

    val hosts = for { h <- 1 to numberOfHosts } yield {
      val hName = f"H$h%x"
      val hAddr = f"2001:db8:$h%x"

      val subnets = for { subnet <- 1 to subnetCount } yield {
        val subnetName = f"$hName-SN$subnet%x"
        val subnetAddr = f"$hAddr:$subnet%x"

        val services = for { service <- 1 to serviceCount } yield {
          val serviceName = f"$subnetName-S$service%x"
          val serviceAddr = f"$subnetAddr:$service%x"

          val instances = for {
            instance <- 1 to subnetCount
            if instance != subnet
          } yield {
            val instanceName = f"$serviceName-I$instance%x"
            val instanceAddr = f"$serviceAddr:$instance%x"
            Interface(instanceName, ipv6(s"$instanceAddr::"))
          }
          NetService(Interface(serviceName, ipv6(s"$serviceAddr::")), instances)
        }
        Subnet(
          Interface(subnetName, ipv6(s"$subnetAddr::")),
          services
        )
      }
      Host(
        Interface(hName, ipv6(s"$hAddr::")),
        subnets
      )
    }

    val interfaces = hosts.flatMap { host =>
      host.interface +: host.subnets.flatMap { subnet =>
        subnet.interface +: subnet.services.flatMap { service =>
          service.interface +: service.instances
        }
      }
    }

    val numHostsNeedingTunnels = hostIpv4Addresses.length - 1
    val tunnels = for {
      h1 <- 0 to numHostsNeedingTunnels
      h2 <- 0 to numHostsNeedingTunnels
      if h1 != h2

      h41 = ipv4(hostIpv4Addresses(h1))
      h42 = ipv4(hostIpv4Addresses(h2))
      h61 = hosts(h1)
      h62 = hosts(h2)
      subnet <- h62.subnets
    } yield {
      Tunnel6in4(
        name = f"${h61.interface.name}_${subnet.interface.name}",
        localEnd = h41,
        remoteEnd = h42,
        addr = h61.interface.addrs.head,
        remoteIPv6Net = Inet6Subnet(subnet.interface.addrs.head, 64)
      )
    }

    Doc(
      interfaces = interfaces,
      //      natFans = natFans,
      tunnels = tunnels
    )
  }
}

object TestDocGenerator extends DocProtocol {
  def main(args: Array[String]) {
    val generator = new TestDocGenerator
    val doc = generator.generateDoc(10, 10, 10, Seq(
      "10.1.2.162",
      "10.1.2.164",
      "10.1.2.161",
      "10.1.2.160",
      "10.1.2.155",
      "10.1.2.156",
      "10.1.2.157",
      "10.1.2.158",
      "10.1.2.159",
      "10.1.2.163"
    ))
    val json = Json.toJson(doc)
    val jsonString = Json.prettyPrint(json)
    println(jsonString)
  }
}