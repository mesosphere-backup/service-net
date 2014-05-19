package mesosphere.servicenet.patch.bash

import java.io.{ FileOutputStream, File }
import scala.sys.process._

import mesosphere.servicenet.dsl
import mesosphere.servicenet.util._
import mesosphere.servicenet.patch.bash.Command._ // For implicits

/**
  * The
  */
case class Interpreter() extends dsl.Interpreter with Logging {
  lazy val script: Array[Byte] =
    IO.read(getClass.getClassLoader.getResourceAsStream("patch.bash"))

  def interpret(diff: dsl.Diff) = runCommands(
    diff.interfaces.map(_.command) ++
      diff.natFans.map(_.command) ++
      diff.tunnels.map(_.command)
  )

  def runCommands(commands: Seq[Seq[String]]) {
    val tmp: File = File.createTempFile("servicenet-patch.", ".bash")
    tmp.deleteOnExit()
    tmp.setWritable(true)
    tmp.setExecutable(true)
    try {
      IO.overwrite(tmp, script)
      val path = tmp.getAbsolutePath
      for (command <- commands) {
        log debug s"call // ${command.mkString(" ")}"
        val exitCode = Process(path +: command).!
        val msg = s"exit $exitCode // ${command.mkString(" ")}"
        if (exitCode == 0) log debug msg else log warn msg
      }
    }
    finally {
      tmp.delete()
    }
  }
}

