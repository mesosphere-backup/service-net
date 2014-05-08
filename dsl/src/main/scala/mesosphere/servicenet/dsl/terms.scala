package mesosphere.servicenet.dsl

import mesosphere.servicenet.dsl.dns.Record

//////////////////////////////////////////////////////////////////////////////
//  Diffable Service Network Descriptions  ///////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

case class Doc(
  interfaces: Seq[Interface],
  dns: Seq[Record],
  nat: Seq[NAT],
  tunnels: Seq[Tunnel])

case class Diff(
  interfaces: Seq[Change[Interface]],
  dns: Seq[Change[Record]],
  nat: Seq[Change[NAT]],
  tunnels: Seq[Change[Tunnel]])

trait NetworkEntity {
  def name(): String
}

case class Interface(name: String) extends NetworkEntity

case class NAT(name: String) extends NetworkEntity

case class Tunnel(name: String) extends NetworkEntity

//////////////////////////////////////////////////////////////////////////////
//  Changes  /////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

trait Change[T <: NetworkEntity]

case class Add[T <: NetworkEntity](item: T) extends Change[T]

case class Remove[T <: NetworkEntity](name: String) extends Change[T]
