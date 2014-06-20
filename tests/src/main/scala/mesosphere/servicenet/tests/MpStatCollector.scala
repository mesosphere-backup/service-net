package mesosphere.servicenet.tests

import mesosphere.servicenet.util.Logging

import scala.collection.mutable
import scala.sys.process.{ ProcessLogger, Process }

class MpStatCollector(intervalSeconds: Int) extends Logging {

  private val statsCollectorLineHandler = new MpStatCollectorLineHandler
  private var process: Option[Process] = None
  private var canStart = true
  private var canReport = false

  def start(): MpStatCollector = {
    require(canStart, "Can not restart an already stopped collector")

    process = Some(
      Process(Seq("mpstat", "-P", "ALL", s"$intervalSeconds"))
        .run(statsCollectorLineHandler, connectInput = true)
    )

    this
  }

  def stop() = {
    canStart = false

    process.map(_.destroy())

    canReport = true
  }

  def data(): MpStatResults = {
    require(canReport, "The collector must be stopped before reporting")
    val parsedResults = statsCollectorLineHandler.grouping.map{
      case (cpuLabel, rawResults) =>
        cpuLabel -> rawResults.map(Result(_)).toSeq
    }

    MpStatResults(
      parsedResults.toSeq
        .map { case (cpuLabel, results) => ResultSummary(cpuLabel, results) }
        .sortBy(_.cpuLabel)
    )
  }
}

class MpStatCollectorLineHandler extends ProcessLogger with Logging {
  val grouping = mutable.Map[String, mutable.ListBuffer[Seq[String]]]()
  override def out(s: => String): Unit = {
    val toMatch = s.split(" +").toList
    toMatch match {
      case newLine :: Nil => // no-op empty line
      case "Linux" :: version :: hostname :: date :: arch :: cpus :: Nil =>
      // no-op Starting header line

      /* two-different versions/formats of mpstat between some machines */

      // mpstat@10.0.5  -------------------------------------------------------
      case time :: ampm :: "CPU" :: "%usr" :: "%nice" :: "%sys"
        :: "%iowait" :: "%irq" :: "%soft" :: "%steal" :: "%guest" :: "%idle"
        :: Nil => // no-op Header line
      case time :: ampm :: cpuLabel :: usr :: nice :: sys
        :: iowait :: irq :: soft :: steal :: guest :: idle
        :: Nil =>
        log.trace(">10.0.5+> {}", s)
        val list = grouping.getOrElse(cpuLabel, mutable.ListBuffer())
        grouping.put(
          cpuLabel, list += Seq(
            usr, nice, sys, iowait, irq, soft, steal, guest, idle
          )
        )

      // mpstat@10.2.0  /*extra gnice column*/ --------------------------------
      case time :: ampm :: "CPU" :: "%usr" :: "%nice" :: "%sys"
        :: "%iowait" :: "%irq" :: "%soft" :: "%steal" :: "%guest"
        :: "%gnice" :: "%idle"
        :: Nil => // no-op Header line
      case time :: ampm :: cpuLabel :: usr :: nice :: sys
        :: iowait :: irq :: soft :: steal :: guest
        :: gnice :: idle
        :: Nil =>
        log.trace(">10.2.0+> {}", s)
        val list = grouping.getOrElse(cpuLabel, mutable.ListBuffer())
        grouping.put(
          cpuLabel, list += Seq(
            usr, nice, sys, iowait, irq, soft, steal, guest, idle
          )
        )
      case _ => log.warn(
        s"unhandled line: ${toMatch.mkString("['", "','", "']")}"
      )
    }
  }

  override def buffer[T](f: => T): T = f
  override def err(s: => String): Unit = { /* no-op */ }
}

private case class Result(usr: Double, nice: Double, sys: Double,
                          iowait: Double, irq: Double, soft: Double,
                          steal: Double, guest: Double, idle: Double)

private object Result {
  def apply(strings: Seq[String]): Result = {
    val d = strings.map(_.toDouble)
    Result(d(0), d(1), d(2), d(3), d(4), d(5), d(6), d(7), d(8))
  }
}

case class MpStatResults(results: Seq[ResultSummary])

case class ResultSummary(cpuLabel: String,
                         usr: MetricPercentile,
                         sys: MetricPercentile,
                         irq: MetricPercentile,
                         iowait: MetricPercentile)
object ResultSummary {
  private val usrMetric = metric("usr", { r: Result => r.usr })_
  private val sysMetric = metric("sys", { r: Result => r.sys })_
  private val irqMetric = metric("irq", { r: Result => r.irq })_
  private val iowaitMetric = metric("iowait", { r: Result => r.iowait })_

  def apply(cpuLabel: String, data: Seq[Result]): ResultSummary = {
    ResultSummary(
      cpuLabel,
      usrMetric(data),
      sysMetric(data),
      irqMetric(data),
      iowaitMetric(data)
    )
  }

  def metric(label: String, e: Result => Double)(data: Seq[Result]) = {
    val distribution = data.map(e).sorted
    val p50Index = (distribution.length * 0.50).toInt
    val p95Index = (distribution.length * 0.95).toInt
    MetricPercentile(
      label,
      distribution(p50Index),
      distribution(p95Index)
    )
  }

}

case class MetricPercentile(metricLabel: String, p50: Double, p95: Double)
