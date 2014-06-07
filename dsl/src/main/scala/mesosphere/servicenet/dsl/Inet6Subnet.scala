package mesosphere.servicenet.dsl

import java.lang.IllegalArgumentException
import java.net.{ InetAddress, Inet6Address }
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
      prefixBits <= other.prefixBits
  }

  /**
    * Find the highest subnet below this one with the given number of bits as a
    * prefix. For example, the highest `/96` (32 bit subnet) in
    * `2001:db8:f:f::/64` is `2001:db8:f:f:ffff:ffff::/96`
    */
  def highest(bits: Int): Inet6Subnet = {
    require(bits > prefixBits,
      "Child subnets must have more prefix bits than their parents")
    val inverseMask = BitSet((prefixBits to 127): _*)
    val targetMask = BitSet((0 to (bits - 1)): _*)
    val targetAddr = targetMask & (Bits.toBitSet(addr) | inverseMask)
    val bytes = Bits.fromBitSet(targetAddr, length = 16)
    val fromBytes = InetAddress.getByAddress(bytes).asInstanceOf[Inet6Address]
    Inet6Subnet(fromBytes, bits)
  }
}

object Inet6Subnet {
  @throws[java.lang.IllegalArgumentException]
  def apply(s: String): Inet6Subnet = {
    val components = s.split('/')
    if (components.size != 2) throw
      new IllegalArgumentException("Subnet must be of form: <addr>/<length>")
    val Seq(addr, prefix): Seq[String] = components
    Try(InetAddressHelper.ipv6(addr)).flatMap({ addr =>
      Try(Inet6Subnet(addr, prefix.toInt))
    }).get
  }
}
