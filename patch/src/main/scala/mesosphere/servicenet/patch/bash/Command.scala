package mesosphere.servicenet.patch.bash

import mesosphere.servicenet.dsl
import mesosphere.servicenet.dsl.Tunnel6in4
import scala.language.implicitConversions

sealed trait Command {
  def command(): Seq[String]
}

object Command {
  case class Interface(change: dsl.Change[dsl.Interface]) extends Command {
    val command = change match {
      case dsl.Remove(name) => Seq("rm", "dummy", name)
      case dsl.Add(item) => item.addr match {
        case Right(net) => Seq("dummy", item.name, net.getCanonicalForm)
        case Left(ip)   => Seq("dummy", item.name, ip.getHostAddress)
      }
    }
  }

  implicit def interface2cmd(change: dsl.Change[dsl.Interface]): Interface =
    Interface(change)

  case class NAT(change: dsl.Change[dsl.NAT]) extends Command {
    val command = change match {
      case dsl.Remove(name) => Seq("rm", "nat-fan", name)
      case dsl.Add(item) => {
        import item._
        Seq("nat-fan", name, subnet.getCanonicalForm) ++
          instances.map(_.getHostAddress)
      }
    }
  }

  implicit def nat2cmd(change: dsl.Change[dsl.NAT]): NAT = NAT(change)

  case class Tunnel(change: dsl.Change[dsl.Tunnel]) extends Command {
    val command = change match {
      case dsl.Remove(name) => Seq("rm", "tunnel", name)
      case dsl.Add(tun @ Tunnel6in4(name, local, remote, addr, net)) => Seq(
        "tunnel",
        name,
        local.getHostAddress,
        remote.getHostAddress,
        addr.getHostAddress,
        net.getCanonicalForm
      )
    }
  }

  implicit def tunnel2cmd(change: dsl.Change[dsl.Tunnel]): Tunnel =
    Tunnel(change)
}
