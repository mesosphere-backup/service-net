package mesosphere.servicenet.dsl

import java.net.Inet6Address

/**
  * Describes a NAT rule which connects a service to its backends.
  *
  * This part of the DSL might be too low-level or it might be too high level.
  *
  * @param name Name of IP Tables rule that will be created, which will be
  *             assigned to a comment. It is needed to allow deletion of the
  *             rule.
  * @param entrypoint A clsuter-wide IP identifying the service.
  * @param midpoint An IP on the host's instance network, which traffic
  *                 passes through before going out to endpoints, so that
  *                 endpoints on other hosts will have their return packets
  *                 routed correctly.
  * @param endpoints IPs of service backends, to which traffic will be
  *                  ultimately NAT'd.
  */
case class NATFan(name: String,
                  entrypoint: Inet6Address,
                  midpoint: Inet6Address,
                  endpoints: Seq[Inet6Address]) extends NetworkEntity
