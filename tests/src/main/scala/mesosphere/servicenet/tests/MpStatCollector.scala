package mesosphere.servicenet.tests

import mesosphere.servicenet.util.Logging

import scala.collection.mutable
import scala.sys.process.{ProcessLogger, Process}

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

  def report() = {
    require(canReport, "The collector must be stopped before reporting")
    statsCollectorLineHandler.g
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
    collector.report()
  }
}
