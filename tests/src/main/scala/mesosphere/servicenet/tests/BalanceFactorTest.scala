package mesosphere.servicenet.tests

import com.twitter.util.{ Await, Future, Stopwatch }
import java.net.InetSocketAddress

import play.api.libs.json._

import mesosphere.servicenet.util.Logging

case class BalanceFactorTestResults(clientConnection: InetSocketAddress,
                                    requestCount: Int,
                                    expectedBalanceFactor: Double,
                                    balanceVariance: Double,
                                    durationMillis: Long,
                                    bwMBs: Double,
                                    cpuMetrics: MpStatResults = MpStatResults(),
                                    pass: Boolean,
                                    failureException: Option[Throwable],
                                    allResults: List[ServerRequestSummary],
                                    unbalanced: List[ServerRequestSummary])

case class ServerRequestSummary(serverIp: String,
                                numRequests: Int,
                                percentage: Double)

class BalanceFactorTest(client: Client) extends Logging {
  def runBalanceFactorTest(
    requestCount: Int,
    requestSize: Int,
    expectBalanceFactorDelta: Double): BalanceFactorTestResults = {
    val stopwatch = Stopwatch.start()
    val mpStatsCollector = new MpStatCollector(1).start()

    var possibleError: Option[Throwable] = None
    val f = {
      Future.collect(
        1 to requestCount map {
          i => client.ping(requestSize, i)
        }
      ) onFailure {
          case t: Throwable =>
            log.error("Error", t)
            possibleError = Some(t)
        }
    }

    val resp = Await.result(f)
    val duration = stopwatch().inMillis
    mpStatsCollector.stop()

    assert(
      resp.size == requestCount,
      s"expected: $requestCount actual: ${resp.size}"
    )
    val totalRequests: Double = resp.size

    val results = resp.groupBy { case response => response.responseServerIp }
      .toList
      .map {
        case g =>
          val (serverIp, responses) = g
          ServerRequestSummary(
            serverIp,
            responses.size,
            responses.size / totalRequests
          )
      }

    val expectBalanceFactor = 1d / results.size
    val expectBalanceFactorMin = expectBalanceFactor - expectBalanceFactorDelta
    val expectBalanceFactorMax = expectBalanceFactor + expectBalanceFactorDelta
    val unbalanced = results.filterNot {
      case result =>
        expectBalanceFactorMin <= result.percentage &&
          result.percentage <= expectBalanceFactorMax
    }

    val totalBandwidthBytesPerMs = {
      val totalBytes = resp.map(_.numBytes).sum
      totalBytes / duration.toDouble
    }

    new BalanceFactorTestResults(
      client.address,
      requestCount,
      expectBalanceFactor,
      expectBalanceFactorDelta,
      bwMBs = 1000 * (totalBandwidthBytesPerMs / (1024 * 1024)),
      cpuMetrics = mpStatsCollector.data(),
      durationMillis = duration,
      pass = unbalanced.isEmpty,
      failureException = possibleError,
      allResults = results,
      unbalanced = unbalanced
    )
  }
}

trait BalanceFactorTestFormatters {

  implicit val seqServerRequestSummaryFormat =
    new Format[Seq[ServerRequestSummary]] {
      override def reads(json: JsValue): JsResult[Seq[ServerRequestSummary]] = {
        ???
      }

      override def writes(o: Seq[ServerRequestSummary]): JsValue = {
        JsObject(o.map {
          case srs: ServerRequestSummary =>
            srs.serverIp -> JsNumber(srs.numRequests)
        })
      }
    }

  implicit val mpStatResultsFormat = new Format[MpStatResults] {
    override def reads(json: JsValue): JsResult[MpStatResults] = ???

    override def writes(o: MpStatResults): JsValue = {
      JsObject(
        for {
          cpu <- o.results
          value <- forResultSummary(cpu)
        } yield value
      )
    }

    def forResultSummary(rs: ResultSummary): Seq[(String, JsValue)] = {
      forMetric(rs.usr, rs.cpuLabel) ++
        forMetric(rs.sys, rs.cpuLabel) ++
        forMetric(rs.irq, rs.cpuLabel) ++
        forMetric(rs.iowait, rs.cpuLabel)
    }

    def forMetric(o: MetricPercentile, pre: String): Seq[(String, JsValue)] = {
      Seq(
        s"system/cpu/$pre/${o.metricLabel}/p50" -> JsNumber(o.p50),
        s"system/cpu/$pre/${o.metricLabel}/p95" -> JsNumber(o.p95)
      )
    }
  }

  implicit val balanceFactorTestResultsFormat =
    new Format[BalanceFactorTestResults] {
      override def reads(json: JsValue): JsResult[BalanceFactorTestResults] = {
        ???
      }

      override def writes(o: BalanceFactorTestResults): JsValue = {
        val conn = o.clientConnection
        val qps = 1000 * (o.requestCount.toDouble / o.durationMillis.toDouble)
        JsObject(Seq(
          "connection" -> JsString(s"${conn.getHostName}:${conn.getPort}"),
          "resolvedAddress" -> JsString(conn.getAddress.getHostAddress),
          "requestCount" -> JsNumber(o.requestCount),
          "expectedBalanceFactor" -> JsNumber(o.expectedBalanceFactor),
          "balanceVariance" -> JsNumber(o.balanceVariance),
          "durationMillis" -> JsNumber(o.durationMillis),
          "testPassed" -> JsBoolean(o.pass),
          "results" -> seqServerRequestSummaryFormat.writes(o.allResults),
          "unbalanced" -> seqServerRequestSummaryFormat.writes(o.unbalanced),
          "totalBandwidthInMBsPerSecond" -> JsNumber(o.bwMBs),
          "requestsPerSecond" -> JsNumber(qps),
          "metrics" -> mpStatResultsFormat.writes(o.cpuMetrics)
        ))
      }
    }
}
