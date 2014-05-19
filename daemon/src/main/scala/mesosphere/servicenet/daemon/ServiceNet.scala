package mesosphere.servicenet.daemon

import mesosphere.servicenet.http.HTTPServer
import mesosphere.servicenet.ns.NameServer
import mesosphere.servicenet.util.Logging
import mesosphere.servicenet.patch.bash

object ServiceNet extends App with Logging {
  val nameServer = new NameServer
  val httpServer = new HTTPServer({ diff =>
    nameServer.update(diff)
    bash.Interpreter(dryRun = true).interpret(diff)
  })

  // TODO: Get these from config
  val namePort = 8888
  val httpPort = 9000

  nameServer run namePort
  httpServer run httpPort
}
