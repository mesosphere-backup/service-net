package mesosphere.servicenet.tests

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import play.api.libs.json.Json

import mesosphere.servicenet.dsl._
import mesosphere.servicenet.http.json.DocProtocol
import mesosphere.servicenet.util.InetAddressHelper.{ ipv4, ipv6 }
import mesosphere.servicenet.util.IO

case class Host(
  interface: Interface,
  subnets: Seq[Subnet])
case class Subnet(
    interface: Interface,
    services: Seq[NetService]) {
  private val splits = interface.name.split("-")
  val host = splits(0)
  val name = splits(1)
}
case class NetService(
    interface: Interface,
    instances: Seq[Interface]) {
  private val splits = interface.name.split("-")
  val host = splits(0)
  val net = splits(1)
  val name = splits(2)
}

class TestDocGenerator extends DocProtocol {
  def generateDoc(hostCount: Int,
                  serviceCount: Int,
                  instanceCount: Int,
                  hostIpv4Addresses: Seq[String] = Seq()) = {

    val numberOfHosts = math.max(hostCount, hostIpv4Addresses.length)

    val hosts = for { h <- 1 to numberOfHosts } yield {
      val hName = f"H$h%x"
      val hAddr = f"2001:db8:$h%x"

      val subnetName = s"$hName-NET1"
      val subnetAddr = s"$hAddr:1"

      val services = for { service <- 1 to serviceCount } yield {
        val serviceName = f"$subnetName-SVC$service%x"
        val serviceAddr = f"$subnetAddr:$service%x"

        val instances = for {
          instance <- 1 to instanceCount
        } yield {
          val instanceName = f"$serviceName-I$instance%x"
          Interface(instanceName, ipv6(f"$serviceAddr::$instance%x"))
        }
        NetService(Interface(serviceName, ipv6(s"$serviceAddr::")), instances)
      }

      val cannedServiceInstances = {
        val revBytes = "eeee"

        val svcName = f"$subnetName-SVC$revBytes"
        val svcAddr = s"$subnetAddr:$revBytes"

        val instName = f"$svcName-I1"
        NetService(
          Interface(svcName, ipv6(s"$svcAddr::")), Seq(
            Interface(instName, ipv6(s"$svcAddr::1"))
          )
        )
      }

      val subnet = Subnet(
        Interface(subnetName, ipv6(s"$subnetAddr::")),
        services :+ cannedServiceInstances
      )

      Host(
        Interface(hName, ipv6(s"$hAddr::")),
        Seq(subnet)
      )
    }

    val interfaces = hosts.flatMap { host =>
      host.subnets.flatMap { subnet =>
        subnet.services.flatMap { service =>
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

    val natFans = {
      val services = for {
        host <- hosts
        subnet <- host.subnets
        service <- subnet.services
      } yield service

      val serviceGroups = services.groupBy(_.name)

      val fans = for {
        service <- services
      } yield {
        val allServices = serviceGroups(service.name)
        val endpoints = for {
          s <- allServices
          if s.host != service.host // we don't nat to our own host
          i <- s.instances
          a <- i.addrs
        } yield a
        NATFan(
          name = s"NAT-${service.interface.name}",
          entrypoint = service.interface.addrs.head,
          endpoints = endpoints
        )
      }
      fans
    }

    val doc = Doc(
      interfaces = interfaces,
      natFans = natFans.toSeq,
      tunnels = tunnels
    )

    val props = for {
      host <- hosts
      subnet <- host.subnets
    } yield {
      val subnetInterface = subnet.interface
      subnetInterface.name -> Inet6Subnet(subnetInterface.addrs.head, 64)
    }

    val generationTime =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())

    props.foreach {
      case (name, subnet) =>
        val fileContents =
          s"""
            |# Host and Subnet: $name
            |# $generationTime
            |svcnet.subnet.instance=${subnet.getCanonicalForm}
            |ns.port=53
          """.stripMargin.trim + "\n" // add back the trailing new line
        IO.overwrite(new File(s"$name.properties"), fileContents)
    }

    val json = Json.toJson(doc)
    val jsonString = Json.prettyPrint(json)
    IO.overwrite(new File("net.json"), jsonString)
  }
}

object TestDocGenerator {
  def main(args: Array[String]) {
    val generator = new TestDocGenerator

    val opts = parser.parse(args, Options()).get

    generator.generateDoc(opts.hosts, opts.services, opts.instances, opts.ips)
  }

  val parser = new scopt.OptionParser[Options]("TestDocGenerator") {
    opt[Int]("hosts") action { (x, c) =>
      c.copy(hosts = x)
    } text "hosts is an integer property"
    opt[Int]("services") action { (x, c) =>
      c.copy(services = x)
    } text "services is an integer property"
    opt[Int]("instances") action { (x, c) =>
      c.copy(instances = x)
    } text "instances is an integer property"

    help("help") text "prints this usage text"
    arg[String]("<IPv4 IP>...") unbounded () optional () action { (x, c) =>
      c.copy(ips = c.ips :+ x)
    } text "if tunneling, IPv4s for each host"
  }
}

case class Options(hosts: Int = 10,
                   services: Int = 10,
                   instances: Int = 10,
                   ips: Seq[String] = Seq())
