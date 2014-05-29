package mesosphere.servicenet.tests

import mesosphere.servicenet.util.{Logging, IO}
import java.io.File
import scala.sys.process.{Process, ProcessLogger}

object TestToolsInvoker extends Logging {
  lazy val script: Array[Byte] =
    IO.read(getClass.getClassLoader.getResourceAsStream("test-tools.bash"))

  def runCommands(commands: Seq[Seq[String]]) {
    val tmp: File = File.createTempFile("servicenet-test-tools.", ".bash")
    log debug s"Extracting script to: ${tmp.getAbsolutePath}"
    tmp.deleteOnExit()
    tmp.setWritable(true)
    tmp.setExecutable(true)
    try {
      IO.overwrite(tmp, script)
      val path = tmp.getAbsolutePath
      for (c <- commands) {
        val command = path +: c
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
