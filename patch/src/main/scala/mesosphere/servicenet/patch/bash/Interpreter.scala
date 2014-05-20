package mesosphere.servicenet.patch.bash

import java.io.File
import scala.sys.process._

import mesosphere.servicenet.dsl
import mesosphere.servicenet.util._
import mesosphere.servicenet.patch.bash.Command._
import mesosphere.servicenet.config._

/**
  * The Bash implementation of the
  */
case class Interpreter(dryRun: Boolean = false)(
  implicit val config: Config = Config())
    extends dsl.Interpreter with Logging {
  lazy val script: Array[Byte] =
    IO.read(getClass.getClassLoader.getResourceAsStream("patch.bash"))

  def interpret(diff: dsl.Diff) = {
    runCommands(
      diff.interfaces.map(_.command) ++
        diff.natFans.map(_.command) ++
        diff.tunnels.map(_.command)
    )
  }

  def runCommands(commands: Seq[Seq[String]]) {
    val tmp: File = File.createTempFile("servicenet-patch.", ".bash")
    log debug s"Extracting script to: ${tmp.getAbsolutePath}"
    tmp.deleteOnExit()
    tmp.setWritable(true)
    tmp.setExecutable(true)
    try {
      IO.overwrite(tmp, script)
      val path = tmp.getAbsolutePath
      val preamble = if (dryRun) Seq(path, "--dry-run") else Seq(path)
      for (command <- commands.map(preamble ++ _)) {
        log debug s"call // ${command.mkString(" ")}"
        val exit = Process(command) ! ProcessLogger(
          s => log info s"stdout // $s",
          s => log info s"stderr // $s"
        )
        val msg = s"exit $exit // ${command.mkString(" ")}"
        if (exit == 0) log debug msg else log warn msg
      }
    }
    finally {
      tmp.delete()
    }
  }
}

