package mesosphere.servicenet.patch.bash

import java.io.File
import scala.sys.process._

import mesosphere.servicenet.config._
import mesosphere.servicenet.dsl
import mesosphere.servicenet.patch.bash.Command._
import mesosphere.servicenet.util._

/**
  * Calls out to a bundled Bash script to apply the [[dsl.Diff]]. The script
  * contains functions which use `ip` and `iptables` to implement the network
  * configuration.
  */
case class Interpreter()(implicit val config: Config = Config())
    extends dsl.Interpreter with Logging {
  lazy val script: Array[Byte] =
    IO.read(getClass.getClassLoader.getResourceAsStream("patch.bash"))

  def interpret(diff: dsl.Diff) = {
    val interfaces = diff.interfaces.filter {
      case rem: dsl.Remove[_] => true
      case dsl.Add(item) => item.addrs.forall { i =>
        config.instanceSubnet.contains(i) || config.serviceSubnet.contains(i)
      }
    }
    val tunnels = diff.tunnels.filter {
      case r: dsl.Remove[_] => true
      case dsl.Add(item) => item match {
        case tun: dsl.Tunnel6in4 => tun.localEnd == config.localIPv4
      }
    }
    runCommands(
      interfaces.map(_.command) ++
        diff.natFans.map(_.command) ++
        tunnels.map(_.command)
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
      val pre = if (config.rehearsal) Seq(path, "--dry-run") else Seq(path)
      for (command <- commands.map(pre ++ _)) {
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

