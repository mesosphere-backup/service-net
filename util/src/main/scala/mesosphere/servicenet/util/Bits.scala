package mesosphere.servicenet.util

import java.net.{ Inet6Address, InetAddress }
import scala.collection.BitSet

object Bits {
  /**
    * Big-endian [[BitSet]] for a [[Byte]].
    */
  def toBitSet(b: Byte): BitSet = BitSet((0 to 7).filter(isSet(b, _)): _*)

  /**
    * Convert an array of [[Byte]]s to a big-endian [[BitSet]]. The first
    * [[Byte]] is treated as the most significant.
    */
  def toBitSet(bytes: Seq[Byte]): BitSet = {
    val bits: Seq[Boolean] = bytes.map(b => (0 to 7).map(isSet(b, _))).flatten
    BitSet((for ((bit, i) <- bits.zipWithIndex if bit) yield i): _*)
  }

  def toBitSet(bytes: Array[Byte]): BitSet = toBitSet(bytes.toSeq)

  def toBitSet(inet: InetAddress): BitSet = toBitSet(inet.getAddress)

  def fromBitSet(bits: BitSet): Array[Byte] =
    fromBitSet(bits, Math.min(1, Math.ceil(bits.last / 8.0).toInt))

  def fromBitSet(bits: BitSet, length: Int): Array[Byte] = {
    val indices = 0 to ((length * 8) - 1)
    for (set <- indices.grouped(8)) yield {
      for ((bit, i) <- set.reverse.zipWithIndex if bits.contains(bit))
        yield Math.pow(2, i)
    }.sum.toByte
  }.toArray

  /**
    * Determine whether a bit is set in a [[Byte]], according to a big-endian
    * interpretation. Bit 0 is the most significant bit.
    */
  def isSet(byte: Byte, bit: Int): Boolean = ((byte >> (7 - bit)) & 1) == 1
}
