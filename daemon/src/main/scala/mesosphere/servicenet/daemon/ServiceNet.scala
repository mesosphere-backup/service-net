package mesosphere.servicenet.daemon

import mesosphere.servicenet.http.HTTPServer
import mesosphere.servicenet.ns.NameServer
import mesosphere.servicenet.util.Logging
import mesosphere.servicenet.config.Config
import mesosphere.servicenet.patch.bash

object ServiceNet extends App with Logging {
  implicit val config = Config()

  config.logSummary()

  val nameServer = new NameServer
  val httpServer = new HTTPServer({ (diff, doc) =>
    nameServer.update(diff)
    bash.Interpreter().interpret(diff)
  })

  nameServer run config.nsPort
  httpServer run config.httpPort
}
