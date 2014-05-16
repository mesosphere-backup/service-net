package mesosphere.servicenet.util

import java.net.{ InetAddress, Inet4Address, Inet6Address }

object InetAddressHelper {

  /**
    * Returns a canonical `java.net.Inet6Address` for the supplied address
    * string.
    *
    * @param addr A well-formed IPv6 Address
    */
  @throws[java.net.UnknownHostException]
  def ipv6(addr: String): Inet6Address =
    InetAddress.getByName(addr) match {
      case inet: Inet4Address =>
        throw new IllegalArgumentException("An IPv4 address was supplied")
      case inet: Inet6Address => inet
    }

  /**
    * Returns a canonical `java.net.Inet4Address` for the supplied address
    * string.
    *
    * @param addr A well-formed IPv4 Address
    */
  @throws[java.net.UnknownHostException]
  def ipv4(addr: String): Inet4Address =
    InetAddress.getByName(addr) match {
      case inet: Inet4Address => inet
      case inet: Inet6Address =>
        throw new IllegalArgumentException("An IPv6 address was supplied")
    }

}
