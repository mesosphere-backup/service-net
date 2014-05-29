package mesosphere.servicenet.daemon

import ch.qos.logback.classic.Level
import org.slf4j.LoggerFactory

import mesosphere.servicenet.config.Config

object Logging {
  def configure()(implicit config: Config = Config()) {
    val rootLogger = LoggerFactory
      .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
      .asInstanceOf[ch.qos.logback.classic.Logger]
    rootLogger.setLevel(Level.toLevel(config.logLevel, rootLogger.getLevel()))
  }
}