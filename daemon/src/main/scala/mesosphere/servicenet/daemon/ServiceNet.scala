package mesosphere.servicenet.daemon

import mesosphere.servicenet.config.Config
import mesosphere.servicenet.http.HTTPServer
import mesosphere.servicenet.ns.NameServer
import mesosphere.servicenet.util
import mesosphere.servicenet.patch.bash

object ServiceNet extends App with util.Logging {
  implicit val config = Config()

  Logging.configure()

  config.logSummary()

  val nameServer = new NameServer
  val httpServer = new HTTPServer({ (diff, doc) =>
    nameServer.update(doc)
    bash.Interpreter().interpret(diff)
  })

  nameServer run config.nsPort
  httpServer run config.httpPort
}
