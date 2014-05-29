package mesosphere.servicenet.tests

import java.net.InetSocketAddress
import com.twitter.util.{ Await, Future, Stopwatch }
import mesosphere.servicenet.util.Logging

case class BalanceFactorTestResults(
  clientConnection: InetSocketAddress,
  requestCount: Int,
  expectedBalanceFactor: Double,
  expectedBalanceFactorDelta: Double,
  durationMillis: Long,
  pass: Boolean,
  failureException: Option[Throwable],
  allResults: List[ServerRequestSummary],
  unbalancedResults: List[ServerRequestSummary])

case class ServerRequestSummary(
  serverIp: String,
  numRequests: Int,
  percentage: Double)

class BalanceFactorTest(client: Client) extends Logging {

  def runBalanceFactorTest(
    requestCount: Int,
    expectBalanceFactor: Double,
    expectBalanceFactorDelta: Double): BalanceFactorTestResults = {
    val stopwatch = Stopwatch.start()

    var possibleError: Option[Throwable] = None
    val f = {
      Future.collect(
        1 to requestCount map {
          i => client.ping(i)
        }
      ) onFailure {
          case t: Throwable =>
            log.error("Error", t)
            possibleError = Some(t)
        }
    }

    val resp = Await.result(f)

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
            (responses.size / totalRequests) * 100
          )
      }

    val expectBalanceFactorMin = expectBalanceFactor - expectBalanceFactorDelta
    val expectBalanceFactorMax = expectBalanceFactor + expectBalanceFactorDelta
    val unbalanced = results.filterNot {
      case result =>
        expectBalanceFactorMin <= result.percentage &&
          result.percentage <= expectBalanceFactorMax
    }

    new BalanceFactorTestResults(
      client.address,
      requestCount,
      expectBalanceFactor,
      expectBalanceFactorDelta,
      durationMillis = stopwatch().inMillis,
      pass = unbalanced.isEmpty,
      failureException = possibleError,
      allResults = results,
      unbalancedResults = unbalanced
    )
  }

}
