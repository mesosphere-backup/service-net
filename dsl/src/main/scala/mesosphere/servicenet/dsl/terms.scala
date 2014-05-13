package mesosphere.servicenet.dsl

//////////////////////////////////////////////////////////////////////////////
//  Diffable Service Network Descriptions  ///////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

/**
  * The network doc describes a service network, or a piece thereof. The
  * description is in terms of concrete items of network configuration: virtual
  * interfaces, tunnels, DNS entries.
  *
  * @param interfaces virtual interfaces in the service network
  * @param dns DNS entries that point to things in the service net
  * @param nat map a service IP to its backends using NAT
  * @param tunnels we sometimes need to tunnel traffic from one host to another
  *                to connect the service network (for example, tunneling IPv6
  *                over IPv4)
  */
case class Doc(interfaces: Seq[Interface],
               dns: Seq[DNS],
               nat: Seq[NAT],
               tunnels: Seq[Tunnel]) {
  def diff(other: Doc): Diff = Diff(
    interfaces = Diff.diff(interfaces, other.interfaces),
    dns = Diff.diff(dns, other.dns),
    nat = Diff.diff(nat, other.nat),
    tunnels = Diff.diff(tunnels, other.tunnels)
  )
}

/**
  * A network entity is a convenient unit of network configuration -- a virtual
  * interface, a single DNS entry, a single tunnel -- for addition to and
  * removal from a single host's network setup.
  */
trait NetworkEntity {
  def name(): String
}

/**
  * A network diff describes changes to the service network. This diff is what
  * is passed to a DSL implementation.
  *
  * Each parameter is a list of `Add`/`Remove` instances for the corresponding
  * parameter in `Doc`.
  */
case class Diff(interfaces: Seq[Change[Interface]],
                dns: Seq[Change[DNS]],
                nat: Seq[Change[NAT]],
                tunnels: Seq[Change[Tunnel]]) extends ((Doc) => Doc) {
  def apply(original: Doc): Doc = Doc(
    interfaces = Diff.patch(interfaces, original.interfaces),
    dns = Diff.patch(dns, original.dns),
    nat = Diff.patch(nat, original.nat),
    tunnels = Diff.patch(tunnels, original.tunnels)
  )
}

object Diff {
  def diff[T <: NetworkEntity](a: Seq[T], b: Seq[T]): Seq[Change[T]] = {
    val (one, two) = (Set(a: _*), Set(b: _*))
    val remove: Set[Remove[T]] = (one &~ two).map(Remove(_))
    val add: Set[Add[T]] = (two &~ one).map(Add(_))
    val changes: Set[Change[T]] = add ++ remove
    DNSNameSort.sort(changes.toSeq)
  }

  def patch[T <: NetworkEntity](changes: Seq[Change[T]],
                                entities: Seq[T]): Seq[T] = {
    val removes: Seq[String] = for (Remove(name) <- changes) yield name
    val adds: Map[String, T] =
      (for (Add(e) <- changes) yield (e.name(), e)).toMap
    val original: Map[String, T] = entities.map((e) => (e.name(), e)).toMap

    (original -- removes ++ adds).values.toSeq
  }
}

/**
  * Interpreters of the DSL read a [[Diff]] and configure the system. The
  * system is potentially a host's local network, a switch, or a combination of
  * network systems. The system running the interpreter should use local
  * information to determine which parts of the [[Diff]] are relevant to it.
  */
trait Interpreter {
  /**
    * Apply a [[Diff]] to the system, updating the local network settings.
    * @param diff
    */
  def interpret(diff: Diff)

  /**
    * An interpreter can accept a [[Doc]] along with the [[Diff]], for sanity
    * checking.
    * @param diff
    * @param doc
    */
  def interpret(diff: Diff, doc: Doc) { interpret(diff) }
}

//////////////////////////////////////////////////////////////////////////////
//  Changes  /////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

/**
  * For every network entity, there is a corresponding `Add` and `Remove`
  * class.
  *
  * @tparam T type of network entity
  */
trait Change[T <: NetworkEntity]

case class Add[T <: NetworkEntity](item: T) extends Change[T]

case class Remove[T <: NetworkEntity](name: String) extends Change[T]

object Remove {
  def apply[T <: NetworkEntity](t: T): Remove[T] = Remove(t.name())
}

