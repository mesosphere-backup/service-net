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
      case dsl.Remove(name) => Seq("remove", "dummy", name)
      case dsl.Add(item) =>
        Seq("dummy", item.name) ++ item.addrs.map(_.getHostAddress)
    }
  }

  implicit def interface2cmd(change: dsl.Change[dsl.Interface]): Interface =
    Interface(change)

  case class NATFan(change: dsl.Change[dsl.NATFan]) extends Command {
    val mark = "0x12345678" // Hopefully no one else chooses this one.
    val command = change match {
      case dsl.Remove(name) => Seq("remove", "natfan", name)
      case dsl.Add(item) => "natfan" +: item.name +: mark +:
        item.entrypoint.getHostAddress +:
        item.endpoints.map(_.getHostAddress)
    }
  }

  implicit def nat2cmd(change: dsl.Change[dsl.NATFan]): NATFan = NATFan(change)

  case class Tunnel(change: dsl.Change[dsl.Tunnel]) extends Command {
    val command = change match {
      case dsl.Remove(name) => Seq("remove", "tunnel", name)
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
