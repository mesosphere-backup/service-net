package mesosphere.servicenet.util

import scala.collection.BitSet

class BitsSpec extends Spec {

  import mesosphere.servicenet.util.Bits

  "Bits" should "create a big-endian BitSet for a Byte" in {
    // From the example in RFC 1700 http://tools.ietf.org/html/rfc1700
    Bits.toBitSet(170.toByte) should equal (BitSet(0, 2, 4, 6))
  }
}
