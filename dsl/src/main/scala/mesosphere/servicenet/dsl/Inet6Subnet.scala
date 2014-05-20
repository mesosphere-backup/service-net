package mesosphere.servicenet.dsl

import java.lang.IllegalArgumentException
import java.net.Inet6Address
import scala.collection.BitSet
import scala.util.Try

import mesosphere.servicenet.util.{ Bits, InetAddressHelper }

case class Inet6Subnet(addr: Inet6Address, prefixBits: Int) {
  require(prefixBits >= 0 && prefixBits <= 128,
    "IPv6 subnets can have prefixes only between 0 and 128 bits")

  def getCanonicalForm: String = s"${addr.getHostAddress}/$prefixBits"

  /**
    * A big-endian bit mask that matches this subnet. A /64 thus has ones in
    * bits 0..63, a /32 in bits 0..32.
    */
  val mask: BitSet = BitSet((0 to (prefixBits - 1)): _*)
  // NB:    0 to 0 -> Range(0)    0 to -1 -> Range()

  def contains(inet: Inet6Address) =
    (mask & Bits.toBitSet(addr)) == (mask & Bits.toBitSet(inet))

  def contains(other: Inet6Subnet) = {
    (mask & Bits.toBitSet(addr)) == (mask & Bits.toBitSet(other.addr)) &&
      prefixBits >= other.prefixBits
  }
}

object Inet6Subnet {
  @throws[java.lang.IllegalArgumentException]
  def parse(s: String): Inet6Subnet = {
    val components = s.split('/')
    if (components.size != 2) throw
      new IllegalArgumentException("Subnet must be of form: <addr>/<length>")
    val Seq(addr, prefix): Seq[String] = components
    Try(InetAddressHelper.ipv6(addr)).flatMap({ addr =>
      Try(Inet6Subnet(addr, prefix.toInt))
    }).get
  }
}
