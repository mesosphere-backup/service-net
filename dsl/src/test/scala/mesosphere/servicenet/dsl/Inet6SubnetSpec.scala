package mesosphere.servicenet.dsl

import mesosphere.servicenet.util.{ InetAddressHelper, Spec }

class Inet6SubnetSpec extends Spec {

  object Fixture {
    val addressesInSubnet = Seq(
      "2001:0db8:a000:0000:0000:0000:0000:0000",
      "2001:0db8:a000:0000:0000:0000:0000:0001",
      "2001:0db8:a000:0000:0000:0000:0000:0002"
    ).map(InetAddressHelper.ipv6(_))

    val addressesNotInSubnet = Seq(
      "2001:0db8:b000:0000:0000:0000:0000:0000",
      "2001:0db8:b000:0000:0000:0000:0000:0001",
      "2001:0db8:b000:0000:0000:0000:0000:0002"
    ).map(InetAddressHelper.ipv6(_))

    val subnet = Inet6Subnet(addr = addressesInSubnet(0), prefixBits = 64)
  }

  "Inet6Subnet" should "contain its own address" in {
    import Fixture._
    subnet.contains(subnet.addr) should be (true)
  }

  it should "match addresses in its subnet" in {
    import Fixture._
    for (addr <- addressesInSubnet) subnet.contains(addr) should be (true)
  }

  it should "not match addresses not in its subnet" in {
    import Fixture._
    for (addr <- addressesNotInSubnet) subnet.contains(addr) should be (false)
  }
}
