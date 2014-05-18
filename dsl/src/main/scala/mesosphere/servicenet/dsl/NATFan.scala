package mesosphere.servicenet.dsl

import java.net.Inet6Address

/**
  * Describes a NAT rule which connects a service to its backends.
  *
  * This part of the DSL might be too low-level or it might be too high level.
  *
  * @param name name of IP Tables rule that will be created
  * @param service service IP
  * @param instances hosts to which traffic should ultimately be NATed
  */
case class NATFan(name: String,
                  service: Inet6Address,
                  instances: Seq[Inet6Address]) extends NetworkEntity
