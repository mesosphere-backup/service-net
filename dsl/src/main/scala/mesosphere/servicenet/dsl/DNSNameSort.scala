package mesosphere.servicenet.dsl

import Ordering.Implicits._

/**
  * Sorted on dotted components of a name, starting from the back.
  */
object DNSNameSort {
  def sort[T <: NetworkEntity](seq: Seq[Change[T]]): Seq[Change[T]] = {
    seq.sortBy {
      case Add(item)    => item.name().split('.').reverse.toSeq
      case Remove(name) => name.split('.').reverse.toSeq
    }
  }
}
