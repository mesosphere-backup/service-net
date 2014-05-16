package mesosphere.servicenet.dsl

import java.net.{ Inet4Address, Inet6Address }

sealed trait Tunnel extends NetworkEntity {
  def name(): String
}

/**
  * A 6in4 tunnel allows one to pass IPv6 traffic over an IPv4 network, using
  * IP protocol number 41.
  *
  * See: http://en.wikipedia.org/wiki/6in4
  *
  * @param name Device name to be used for the tunnel
  * @param localEnd Local IPv4 end of the tunnel
  * @param remoteEnd Remote IPv4 end of the tunnel (gateway to net we want to
  *                  reach)
  * @param addr Local IPv6 address to attach to tunnel (otherwise the interface
  *             is never used, since it is not routable)
  * @param remoteIPv6Net The remote IPv6 network to which we're tunneling
  *                      traffic
  */
case class Tunnel6in4(name: String,
                      localEnd: Inet4Address,
                      remoteEnd: Inet4Address,
                      addr: Inet6Address,
                      remoteIPv6Net: Inet6Subnet) extends Tunnel

