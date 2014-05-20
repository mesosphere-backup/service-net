package mesosphere.servicenet.config

import java.net.Inet4Address

import mesosphere.servicenet.dsl.Inet6Subnet
import mesosphere.servicenet.util._

case class Config(localIPv4: Inet4Address,
                  instanceSubnet: Inet6Subnet,
                  serviceSubnet: Inet6Subnet)

object Config {
  val underlying: Map[String, String] =
    Properties.get("mesosphere.servicenet") ++ Properties.get("svcnet")

  /**
    * Obtain a config, using various defaulting rules to substitute for missing
    * properties. Even a completely disconnected node will be able to get a
    * valid config.
    *
    * @param properties a map of properties (by default, the system properties)
    * @return
    */
  def apply(properties: Map[String, String] = underlying): Config = {
    val (ipv4, ipv6) = Net.addresses()
    /*  2001:db8::/32 is reserved for use in documentation.

        http://tools.ietf.org/html/rfc3849

        "The document describes the use of the IPv6 address prefix 2001:DB8::/32
         as a reserved prefix for use in documentation."
    */
    val localIPv4 = properties.get("local.ipv4").map(InetAddressHelper.ipv4(_))
      .orElse(ipv4).getOrElse(InetAddressHelper.ipv4("127.0.0.1"))
    val forInstances = properties.get("local.instanceSubnet")
      .orElse(ipv6.map(_.getHostAddress ++ "/64"))
      .orElse(ipv4.map(InetAddressHelper.ipv6(_))
        .map(_.getHostAddress ++ "/64"))
      .getOrElse("2001:db8:1::/64")
    val forServices = properties.get("serviceSubnet")
      .getOrElse("2001:db8:2::/64")
    Config(
      localIPv4 = localIPv4,
      instanceSubnet = Inet6Subnet.parse(forInstances),
      serviceSubnet = Inet6Subnet.parse(forServices)
    )
  }

}
