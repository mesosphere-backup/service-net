package mesosphere.servicenet.daemon

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
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

    val patterns = Map(
      "none" -> "[%thread] %level %logger{36} %msg%n",
      "short" -> "%d{HH:mm:ss.SSS, UTC} [%thread] %-5level %logger %msg%n",
      "long" ->
        "%d{yyyy-dd-MM HH:mm:ss.SSS, UTC} [%thread] %-5level %logger %msg%n"
    )

    require(patterns.contains(config.logTimestamp),
      "Please choose a timestamp format from: " + patterns.keys.mkString(", "))

    encoder.setPattern(patterns(config.logTimestamp))
    encoder.setContext(rootLogger.getLoggerContext())
    appender.setEncoder(encoder)
    rootLogger.addAppender(appender)
    rootLogger.setLevel(Level.toLevel(config.logLevel, Level.INFO))
    encoder.start()
    appender.start()
  }
}
