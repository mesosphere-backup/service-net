package mesosphere.servicenet.dsl

import java.net.Inet6Address

/**
  * Network interface and its attached address. If the address is given as a
  * subnet, a route is created to push traffic in the subnet to the given
  * interface, as well as creating the interface with the given IP.
  *
  * @param name interface device name
  * @param addrs IPv6 IPs to assign to this interface
  */
case class Interface(name: String, addrs: Seq[Inet6Address])
  extends NetworkEntity

object Interface {
  def apply(name: String, addr: Inet6Address): Interface =
    Interface(name, Seq(addr))
}
