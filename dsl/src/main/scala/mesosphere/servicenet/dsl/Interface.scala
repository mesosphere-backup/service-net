package mesosphere.servicenet.dsl

import java.net.Inet6Address

/**
  * Network interface and its attached address. If the address is given as a
  * subnet, a route is created to push traffic in the subnet to the given
  * interface, as well as creating the interface with the given IP.
  *
  * @param name interface device name
  * @param addr an IPv6 IP or subnet
  */
case class Interface(
  name: String,
  addr: Either[Inet6Address, Inet6Subnet]) extends NetworkEntity

object Interface {
  def apply(name: String, addr: Inet6Address): Interface =
    Interface(name, Left(addr))
  def apply(name: String, addr: Inet6Subnet): Interface =
    Interface(name, Right(addr))
}
