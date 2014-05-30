package mesosphere.servicenet.tests

import mesosphere.servicenet.util.Logging

object Test extends Logging {
  def usage() = {
    System.err.println(
      """
|Usage: java -jar service-net-functional-tests-assembly-x.y.z.jar
|  --help     Print this help
|  --server   Run the testing server
|               http.ip     The IP to bind the testing server to
|               http.port   The port to bind the testing server to
|  --client   Run the testing client
|               Requires <hostname>[:<port] to connect to
|               svcnet.test.requests            Number of requests to send
|               svcnet.test.balance.variance    Acceptable variance from balance
""".stripMargin)
  }
  def main(args: Array[String]) {

    args.toList match {
      case arg0 :: Nil => arg0 match {
        case "--help"       => usage()
        case "--server"     => Server.main(Array())
        case "--client" | _ => Client.main(args)
      }
      case arg0 :: xs => arg0 match {
        case "--help"       => usage()
        case "--server"     => Server.main(xs.toArray)
        case "--client" | _ => Client.main(xs.toArray)
      }
      case _ => Client.main(args)
    }
  }
}
