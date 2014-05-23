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

  /**
    * Calculate the 6to4 address of an IPv4 address.
    */
  def ipv6(ipv4: Inet4Address): Inet6Address = {
    val Array(a, b, c, d) = ipv4.getAddress
    ipv6(f"2002:$a%02x$b%02x:$c%02x$d%02x::")
  }
}
