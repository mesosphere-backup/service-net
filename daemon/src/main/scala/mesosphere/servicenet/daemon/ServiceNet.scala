package mesosphere.servicenet.daemon

import mesosphere.servicenet.http.HTTPServer
import mesosphere.servicenet.ns.NameServer
import mesosphere.servicenet.util.Logging
import mesosphere.servicenet.config.Config
import mesosphere.servicenet.patch.bash

object ServiceNet extends App with Logging {
  implicit val config = Config()
  val nameServer = new NameServer
  val httpServer = new HTTPServer({ (diff, doc) =>
    nameServer.update(diff)
    bash.Interpreter().interpret(diff)
  })

  // TODO: Get these from config
  val namePort = 8888
  val httpPort = 9000

  log info s"$config"

  nameServer run config.nsPort
  httpServer run config.httpPort
}
