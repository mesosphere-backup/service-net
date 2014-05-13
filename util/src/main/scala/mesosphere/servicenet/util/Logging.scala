package mesosphere.servicenet.util

import org.slf4j.{ Logger, LoggerFactory }

trait Logging { val log = LoggerFactory.getLogger(getClass.getName) }
