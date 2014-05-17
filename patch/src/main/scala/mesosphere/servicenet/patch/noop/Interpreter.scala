package mesosphere.servicenet.patch.noop

import mesosphere.servicenet.dsl
import mesosphere.servicenet.util.Logging
import scala.reflect.ClassTag

/**
  * For each change, logs whether it is an add or a remove, and the name and
  * type of each affected entity.
  */
case class Interpreter() extends dsl.Interpreter with Logging {
  def interpret(diff: dsl.Diff) = {
    logChanges(diff.interfaces)
    logChanges(diff.dns)
    logChanges(diff.nat)
    logChanges(diff.tunnels)
  }

  // format: OFF
  def logChanges[T <: dsl.NetworkEntity](changes: Seq[dsl.Change[T]])
                                        (implicit tag: ClassTag[T]) {
    val token = tag.toString().split('.').last.toLowerCase
    for (change <- changes) change match {
      case dsl.Add(item)    => log info s"+$token ${item.name()}"
      case dsl.Remove(name) => log info s"-$token $name"
    }
  }
  // format: ON
}

