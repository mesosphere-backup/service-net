package mesosphere.servicenet.dsl

//////////////////////////////////////////////////////////////////////////////
//  Diffable Service Network Descriptions  ///////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

case class Doc(
  interfaces: Seq[Interface],
  dns: Seq[DNS],
  nat: Seq[NAT],
  tunnels: Seq[Tunnel])

case class Diff(
  interfaces: Seq[Change[Interface]],
  dns: Seq[Change[DNS]],
  nat: Seq[Change[NAT]],
  tunnels: Seq[Change[Tunnel]])

trait NetworkItem {
  def name(): String
}

case class Interface(name: String) extends NetworkItem

case class DNS(name: String) extends NetworkItem

case class NAT(name: String) extends NetworkItem

case class Tunnel(name: String) extends NetworkItem

//////////////////////////////////////////////////////////////////////////////
//  Changes  /////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

trait Change[T <: NetworkItem]

case class Add[T <: NetworkItem](item: T) extends Change[T]

case class Remove[T <: NetworkItem](name: String) extends Change[T]
