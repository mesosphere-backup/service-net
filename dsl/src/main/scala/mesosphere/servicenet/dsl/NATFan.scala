package mesosphere.servicenet.dsl

import java.net.Inet6Address

/**
  * Describes a NAT rule which connects a service to its backends.
  *
  * This part of the DSL might be too low-level or it might be too high level.
  *
  * @param name Name of IP Tables rule that will be created, which will be
  *             placed in a comment. It is needed to allow deletion of the
  *             rule.
  * @param entrypoint An IP that is tied to the service.
  * @param endpoints IPs of service backends, to which traffic will be
  *                  ultimately NAT'd.
  */
case class NATFan(name: String,
                  entrypoint: Inet6Address,
                  endpoints: Seq[Inet6Address]) extends NetworkEntity
