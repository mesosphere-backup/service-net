package mesosphere.servicenet.dsl.dns

import java.net.Inet6Address
import mesosphere.servicenet.dsl.NetworkEntity

//////////////////////////////////////////////////////////////////////////////
//  DNS Record Types  ////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

/**
  * IPv6 record
  */
case class AAAA(label: String, addresses: Seq[Inet6Address]) extends Record {
  val data: Seq[String] = addresses.map(_.getHostAddress)
}

/**
  * SRV record
  */
case class SRV(label: String, endpoints: Seq[SRVData] = Seq()) extends Record {
  val data: Seq[String] = for (srv <- endpoints) yield {
    import srv._
    s"$priority $weight $port $target"
  }
  /**
    * Clients should never cache SRV records but TTL of 0 is not to be used.
    *
    * http://mark.lindsey.name/2009/03/never-use-dns-ttl-of-zero-0.html
    */
  override val ttl: Int = 1
}

case class SRVData(
  target: String,
  port: Int = 0,
  weight: Int = 1,
  priority: Int = 1)

/**
  * See http://www.zytrax.com/books/dns/ch8/#generic
  */
sealed trait Record extends NetworkEntity {
  val label: String
  val recordType: String = getClass.getSimpleName.toUpperCase()
  val recordClass: String = "IN" // Internet class records are the norm
  val ttl: Int = 3600 // 1 hour
  val data: Seq[String]
  val records: Seq[(String, String, String, String, String)] =
    for (d <- data) yield (label, ttl.toString, recordClass, recordType, d)
  val name: String = label
}
