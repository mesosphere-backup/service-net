package mesosphere.servicenet.util

import scala.collection.JavaConverters._
import java.io.{ ByteArrayInputStream, File }

object Properties {
  lazy val underlying: Map[String, String] = System.getProperties.asScala.toMap

  def trim(prefix: String = "",
           properties: Map[String, String] = underlying,
           clipPrefix: Boolean = true): Map[String, String] =
    if (prefix.nonEmpty) {
      val dotted = if (prefix.last == '.') prefix else prefix :+ '.'
      for ((k, v) <- properties if k.startsWith(dotted))
        yield if (clipPrefix) (k.drop(dotted.size) -> v) else (k -> v)
    }
    else properties

  def load(path: String): Map[String, String] = load(new File(path))

  def load(f: File): Map[String, String] = {
    val prop = new java.util.Properties()
    prop.load(new ByteArrayInputStream(IO.read(f)))
    prop.asScala.toMap
  }
}

