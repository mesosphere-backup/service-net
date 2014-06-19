package mesosphere.servicenet.tests

import mesosphere.servicenet.util.Logging

import scala.collection.mutable
import scala.sys.process.{ ProcessLogger, Process }
import scala.collection.mutable.ListBuffer

class MpStatCollector(intervalSeconds: Int) {

  private val statsCollectorLineHandler = new MpStatsCollectorLineHandler
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

  def data(): Results = {
    require(canReport, "The collector must be stopped before reporting")
    Results(statsCollectorLineHandler.g.mapValues(_.toSeq).toMap)
  }
}

class MpStatsCollectorLineHandler extends ProcessLogger with Logging {
  val g: mutable.Map[String, mutable.ListBuffer[String]] =
    mutable.Map[String, mutable.ListBuffer[String]]()
  override def out(s: => String): Unit = {
    s.split(" +").toList match {
      case time :: ampm :: "CPU" :: "%usr" :: "%nice" :: "%sys"
        :: "%iowait" :: "%irq" :: "%soft" :: "%steal" :: "%guest" :: "%idle"
        :: Nil => // no-op Header line
      case time :: ampm :: cpuLabel :: usr :: nice :: sys
        :: iowait :: irq :: soft :: steal :: guest :: idle
        :: Nil =>
        val list = g.getOrElse(cpuLabel, mutable.ListBuffer())
        g.put(cpuLabel, list += irq)
      case _ => // no-op blank line
    }
  }

  override def buffer[T](f: => T): T = f
  override def err(s: => String): Unit = ???
}

object MpStatsCollector {
  def main(args: Array[String]) {
    val collector = new MpStatCollector(2)

    collector.start()

    Thread.sleep(10000)

    collector.stop()
    collector.data()
  }
}

case class Result(usr: Double, nice: Double, sys: Double,
                  iowait: Double, irq: Double, soft: Double,
                  steal: Double, guest: Double, gnice: Double, idle: Double)

object Result {
  def apply(strings: Seq[String]): Result = {
    val d = strings.map(_.toDouble)
    Result(d(0), d(1), d(2), d(3), d(4), d(5), d(6), d(7), d(8), d(9))
  }
}

case class Results(all: Result, perCPU: Seq[Result])

object Results {
  def apply(map: Map[String, Seq[String]]): Results = Results(
    Result(map("all")),
    (map - "all").mapValues(Result(_)).toSeq.sortBy(_._1.toDouble).map(_._2)
  )
}