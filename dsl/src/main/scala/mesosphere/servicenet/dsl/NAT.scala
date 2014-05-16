package mesosphere.servicenet.dsl

import java.net.Inet6Address

/**
  * Describes a NAT rule which connects a service to its backends.
  *
  * This part of the DSL might be too low-level or it might be too high level.
  *
  * @param name name of IP Tables rule that will be created
  * @param subnet subnet to use for load-balancing (first IP is skipped)
  * @param instances hosts to which traffic should ultimately be NATed
  */
case class NAT(name: String,
               subnet: Inet6Subnet,
               instances: Seq[Inet6Address]) extends NetworkEntity
