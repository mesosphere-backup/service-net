package mesosphere.servicenet.dsl

import java.net.Inet6Address

case class Inet6Subnet(addr: Inet6Address, prefixBits: Int) {
  require(prefixBits >= 0 && prefixBits <= 128,
    "IPv6 subnets can have prefixes only between 0 and 128 bits")
}

