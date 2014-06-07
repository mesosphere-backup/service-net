package mesosphere.servicenet.http

import java.sql.{ Connection, DriverManager }
import java.net.Inet6Address
import scala.collection.mutable.ListBuffer

import mesosphere.servicenet.dsl.Inet6Subnet
import mesosphere.servicenet.util.{ Logging, InetAddressHelper }

case class IPService(subnet: Inet6Subnet,
                     dbConn: Connection,
                     autoTrim: Boolean = true) extends Logging {
  def allocate(req: IPServiceRequest): Inet6Address = synchronized {
    // TODO: Improve parallel performance of allocation.
    //
    // * Coalesce INSERTs by putting them in a background thread and returning
    //   a future for the allocation (for example)
    //
    // * The background thread can update an in memory table of available IPs
    //   and prepare a bulk INSERT, and then perform the cleanup action
    //
    // * The background thread should be on a timer, INSERTing once every
    //   25-50ms at most
    //
    // This should get us allocations closer to the row write rate (> 1000/s)
    // than the transaction rate (~100/s)
    val name = req.name
    val last = latestIP().getOrElse(subnet.addr)
    val next = InetAddressHelper.next(last)

    // For comparisons and storage
    val nextFullString = InetAddressHelper.fullLengthIPv6(next)
    val lastFullString = InetAddressHelper.fullLengthIPv6(last)

    if (nextFullString <= lastFullString) {
      val (sub, ip) = (subnet.getCanonicalForm, last.getHostAddress)
      val msg = s"Out of IPs in subnet $sub (last was $ip)"
      log error msg
      throw new IllegalStateException(msg)
    }

    dbConn.createStatement().executeUpdate(s"""
      | INSERT INTO allocated_ip VALUES ('$nextFullString', '$name')
    """.stripMargin)

    if (autoTrim) trim()
    next
  }

  def recent(): Seq[(String, Inet6Address)] = {
    val r = dbConn.createStatement().executeQuery("SELECT * FROM most_recent")
    val buffer: ListBuffer[(String, Inet6Address)] = ListBuffer()
    while (r.next()) buffer += {
      (r.getNString("name"), InetAddressHelper.ipv6(r.getNString("ip")))
    }
    buffer.toSeq
  }

  def latestIP(): Option[Inet6Address] = {
    val r = dbConn.createStatement().executeQuery("SELECT * FROM latest_ip")
    if (r.next()) Some(InetAddressHelper.ipv6(r.getNString("ip"))) else None
  }

  def earliestIP(): Option[Inet6Address] = {
    val r = dbConn.createStatement().executeQuery("SELECT * FROM earliest_recent_ip")
    if (r.next()) Some(InetAddressHelper.ipv6(r.getNString("ip"))) else None
  }

  def trim() {
    earliestIP().map(InetAddressHelper.fullLengthIPv6).map { str =>
      dbConn.createStatement().executeUpdate(
        s"DELETE FROM allocated_ip WHERE ip < '$str'"
      )
    }
  }
}

object IPService {
  def apply(subnet: Inet6Subnet, path: String): IPService = {
    Class.forName("org.h2.Driver")
    val conn: Connection = DriverManager.getConnection(Seq(
      s"jdbc:h2:file:$path",
      s"INIT=RUNSCRIPT FROM 'classpath:schema.sql'",
      "FILE_LOCK=FS",
      "MODE=PostgreSQL",
      "TRACE_LEVEL_FILE=4"
    ).mkString(";"))
    IPService(subnet, conn)
  }
}
