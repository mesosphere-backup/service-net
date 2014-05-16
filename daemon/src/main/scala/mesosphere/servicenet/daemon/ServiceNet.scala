package mesosphere.servicenet.daemon

import mesosphere.servicenet.http.HTTPServer
import mesosphere.servicenet.ns.NameServer
import mesosphere.servicenet.util.Logging

object ServiceNet extends App with Logging {
  val nameServer = new NameServer
  val httpServer = new HTTPServer(nameServer.update)

  // TODO: Get these from config
  val namePort = 8888
  val httpPort = 9000

  nameServer run namePort
  httpServer run httpPort
}
