package mesosphere.servicenet

import scala.language.implicitConversions
import java.net.Inet6Address

package object dsl {

  implicit def inet6Address2Either(
    addr: Inet6Address): Either[Inet6Address, Inet6Subnet] = Left(addr)

  implicit def inet6Subnet2Either(
    subnet: Inet6Subnet): Either[Inet6Address, Inet6Subnet] = Right(subnet)

}
