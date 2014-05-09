package mesosphere.servicenet.dsl

import java.net.Inet6Address

/**
  * A description of a
  * This part of the specification might be too low-level.
  */
case class NAT(
  name: String,
  service: Inet6Subnet,
  instances: Seq[Inet6Address]) extends NetworkEntity
