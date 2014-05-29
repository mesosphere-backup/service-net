package mesosphere.servicenet.daemon

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level

import mesosphere.servicenet.http.HTTPServer
import mesosphere.servicenet.ns.NameServer
import mesosphere.servicenet.util.Logging
import mesosphere.servicenet.config.Config
import mesosphere.servicenet.patch.bash

object ServiceNet extends App with Logging {
  implicit val config = Config()

  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[ch.qos.logback.classic.Logger]
  rootLogger.setLevel(Level.toLevel(config.logLevel))

  config.logSummary()

  val nameServer = new NameServer
  val httpServer = new HTTPServer({ (diff, doc) =>
    nameServer.update(doc)
    bash.Interpreter().interpret(diff)
  })

  nameServer run config.nsPort
  httpServer run config.httpPort
}
