package mesosphere.servicenet.util

import java.net.{ Inet6Address, Inet4Address, NetworkInterface }
import scala.collection.JavaConversions._

object Net {
  def interfaces(): Seq[NetworkInterface] =
    NetworkInterface.getNetworkInterfaces.toSeq

  def active(interface: NetworkInterface): Boolean =
    interface.isUp && !(interface.isLoopback || interface.isVirtual)

  def addresses(): (Option[Inet4Address], Option[Inet6Address]) = {
    var ipv4: Option[Inet4Address] = None
    var ipv6: Option[Inet6Address] = None
    for (interface <- interfaces if active(interface)) {
      for (addr <- interface.getInetAddresses) addr match {
        case i: Inet4Address if !i.isLoopbackAddress => ipv4 = Some(i)
        case i: Inet6Address =>
          if (!i.isLoopbackAddress && !i.isLinkLocalAddress) Some(i) else None
      }
      return (ipv4, ipv6)
    }
    return (ipv4, ipv6)
  }
}
