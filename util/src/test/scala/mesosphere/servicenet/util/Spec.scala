package mesosphere.servicenet.util

import org.scalatest._

trait Spec extends FlatSpec with Matchers with Logging {

  import Spec._

  def time[T](description: String)(action: => T) = {
    val start = System.nanoTime
    val result = action
    val end = System.nanoTime
    val elapsed: Double = (end - start).toDouble / nanosPerSecond
    log.info(
      "\n       [%s] took [%.6f] seconds\n".format(description, elapsed))
    result
  }

}

object Spec {
  val nanosPerSecond = 1000 * 1000 * 1000 // 1 billion
}