package mesosphere.servicenet.util

import scala.collection.JavaConverters._

object Properties {
  lazy val underlying: Map[String, String] = System.getProperties.asScala.toMap

  def get(prefix: String = "",
          clipPrefix: Boolean = true): Map[String, String] =
    if (prefix.nonEmpty) {
      val dotted = if (prefix.last == '.') prefix else prefix :+ '.'
      for ((k, v) <- underlying if k.startsWith(dotted))
        yield if (clipPrefix) (k.drop(dotted.size) -> v) else (k -> v)
    }
    else underlying
}

