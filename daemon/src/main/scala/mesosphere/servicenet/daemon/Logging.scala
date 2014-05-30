package mesosphere.servicenet.daemon

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.OutputStreamAppender
import org.slf4j.LoggerFactory

import mesosphere.servicenet.config.Config

object Logging extends mesosphere.servicenet.util.Logging {
  def configure()(implicit config: Config = Config()) {
    val rootLogger = LoggerFactory
      .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
      .asInstanceOf[ch.qos.logback.classic.Logger]
    rootLogger.detachAndStopAllAppenders()
    val appender = new ConsoleAppender[ILoggingEvent]()
    appender.setContext(rootLogger.getLoggerContext())
    val encoder = new PatternLayoutEncoder()
    config.logTimestamp match {
      case "long" => encoder.setPattern(
        "%d{yyyy-dd-MM HH:mm:ss.SSS, UTC} [%thread] %-5level %logger %msg%n"
      )
      case "short" => encoder.setPattern(
        "%d{HH:mm:ss.SSS, UTC} [%thread] %-5level %logger %msg%n"
      )
      case _ => encoder.setPattern("[%thread] %level %logger{36} %msg%n")
    }
    encoder.setContext(rootLogger.getLoggerContext())
    appender.setEncoder(encoder)
    rootLogger.addAppender(appender)
    rootLogger.setLevel(Level.toLevel(config.logLevel, Level.INFO))
    encoder.start()
    appender.start()
  }
}
