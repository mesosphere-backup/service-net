package mesosphere.servicenet.config

import java.io.File
import java.net.Inet4Address

import mesosphere.servicenet.dsl.Inet6Subnet
import mesosphere.servicenet.util._

/**
  * The configuration for a service net instance can be provided with Java
  * properties. For example, when running the project with SBT, you can use
  * `sbt -Dhttp.port=2100 -Dsvcnet.ipv4=1.1.1.1 run` to set the web server
  * port and control which IP is used for 6in4 tunnels.
  *
  * By default, the configuration system will look for properties files at
  * `/usr/local/etc/svcnet/properties.properties`,
  * `/usr/local/etc/svcnet.properties`,
  * `/etc/svcnet/properties.properties`, and `/etc/svcnet.properties` in that
  * order, and will load the first one found. Properties set in this way will
  * be merged with those set on the command line. To load a different file,
  * pass the property `svcnet.config` or `mesosphere.servicenet.config` on the
  * command line.
  *
  * @param localIPv4 The local endpoint to use for 6in4 tunnels. (Properties:
  *                  `svcnet.ipv4` or `mesosphere.servicenet.ipv4`)
  * @param instanceSubnet The subnet on which instance IPs are allocated, and
  *                        through which traffic should be routed to this host
  *                        if there is a tunnel configured for it. (Properties:
  *                        `svcnet.subnet.instance` or
  *                        `mesosphere.servicenet.subnet.instance`)
  * @param serviceSubnet The subnet on which service IPs are found. This should
  *                      be a `/96` that is fairly high in the address space of
  *                      instance subnet, to prevent collisions between service
  *                      IPs and instance IPs. The service subnet must be below
  *                      the instance subnet. In most situations, Svcnet's
  *                      defaulting will do the right thing with regards to
  *                      picking the service subnet within the the instance
  *                      subnet, so you shouldn't have to set this parameter.
  *                      (Properties: `svcnet.subnet.service` or
  *                       `mesosphere.servicenet.subnet.service`)
  * @param rehearsal In rehearsal mode, the interpreter should filter tasks
  *                  and then print diagnostics for each change that would be
  *                  performed. DNS is updated as usual. (Properties:
  *                  `svcnet.rehearsal` and `mesosphere.servicenet.rehearsal`)
  * @param netState The path at which to store the network state file, to allow
  *                 the service to be restarted safely. The default is
  *                 `/tmp/svcnet.json` (maintaining state between reboots is
  *                 not particularly useful since all virtual interfaces and
  *                 firewalls will by default disappear). (Properties:
  *                 `svcnet.state.net` or `mesosphere.servicenet.state.net`)
  * @param ipState The path at which to store the IP allocation database. The
  *                default is `/var/lib/svcnet`. (Which becomes
  *                `/tmp/svcnet.mv.db` due to H2's naming behaviour.) This
  *                database allows local tasks to obtain a unique IPv6 IP from
  *                the instance subnet. It should be allowed to persist between
  *                reboots, although re-allocating very old IPs is not
  *                disastrous. (Properties: `svcnet.state.ip` or
  *                `mesosphere.servicenet.state.ip`)
  * @param logLevel The level at which to log. The default is INFO.
  *                 (Properties: `svcnet.log.level` and
  *                  `mesosphere.servicenet.log.level`)
  * @param logTimestamp The length of log timestamp to use: `long` (date and
  *                     time), `short` (just time) and `none`. (Properties:
  *                     `svcnet.log.timestamp` and
  *                     `mesosphere.servicenet.log.level`)
  * @param nsPort The port on which to serve DNS traffic. (Properties:
  *               `ns.port` or `svcnet.ns.port` or
  *               `mesosphere.servicenet.ns.port`)
  * @param httpPort The port on which to serve web traffic. (Properties:
  *                 `http.port` or `svcnet.http.port` or
  *                 `mesosphere.servicenet.http.port`)
  */
case class Config(localIPv4: Inet4Address,
                  instanceSubnet: Inet6Subnet,
                  serviceSubnet: Inet6Subnet,
                  rehearsal: Boolean,
                  netState: String,
                  ipState: String,
                  logLevel: String,
                  logTimestamp: String,
                  nsPort: Int,
                  httpPort: Int) extends Logging {
  val propertyLines: Seq[String] = Seq(
    s"svcnet.ipv4=${localIPv4.getHostAddress}",
    s"svcnet.subnet.instance=${instanceSubnet.getCanonicalForm}",
    s"svcnet.subnet.service=${serviceSubnet.getCanonicalForm}",
    s"svcnet.rehearsal=$rehearsal",
    s"svcnet.state.net=$netState",
    s"svcnet.state.ip=$ipState",
    s"svcnet.log.level=$logLevel",
    s"svcnet.log.timestamp=$logTimestamp",
    s"ns.port=$nsPort",
    s"http.port=$httpPort"
  )

  require(instanceSubnet.contains(serviceSubnet),
    s"The service subnet ${serviceSubnet.getCanonicalForm} must be within " +
      s"the instance subnet ${instanceSubnet.getCanonicalForm}")

  def logSummary() {
    for (line <- propertyLines) log info line
  }
}

object Config extends Logging {
  val fromEnvironment: Map[String, String] = trimmed(Properties.underlying)

  val defaultSearchPath: Seq[String] = Seq(
    "/usr/local/etc/svcnet/properties.properties",
    "/usr/local/etc/svcnet.properties",
    "/etc/svcnet/properties.properties",
    "/etc/svcnet.properties"
  )

  lazy val fromFiles: Map[String, String] = searchFiles(
    fromEnvironment.get("config").map(Seq(_)).getOrElse(defaultSearchPath)
  )

  def searchFiles(
    paths: Seq[String] = defaultSearchPath): Map[String, String] = {
    for (f <- paths.map(new File(_)) if f.exists()) {
      log info s"Loading properties from ${f.getAbsolutePath}"
      return trimmed(Properties.load(f))
    }
    Map()
  }

  /**
    * The properties mapping that results from aggregating properties files and
    * command-line properties settings.
    */
  lazy val merged: Map[String, String] = fromFiles ++ fromEnvironment

  def trimmed(properties: Map[String, String]) = Map() ++
    Properties.trim("ns", properties, clipPrefix = false) ++
    Properties.trim("http", properties, clipPrefix = false) ++
    Properties.trim("mesosphere.servicenet", properties) ++
    Properties.trim("svcnet", properties)

  /**
    * Obtain a config, using various defaulting rules to substitute for missing
    * properties. Even a completely disconnected node will be able to get a
    * valid config.
    *
    * @param properties a map of properties (by default, the system properties)
    * @return
    */
  def apply(properties: Map[String, String] = merged): Config = {
    val (ipv4, ipv6) = Net.addresses()

    // Use the IP passed as a property, or the host's IPv4 address, or else
    // 127.0.0.1 and hope for the best.
    val localIPv4 = properties.get("ipv4").map(InetAddressHelper.ipv4(_))
      .orElse(ipv4).getOrElse(InetAddressHelper.ipv4("127.0.0.1"))
    // The instance subnet should be the one specified, or the one derived from
    // the host's IPv6 address, or the one derived from the host's 6to4
    // address, or failing that, the subnet 2001:db8:1:1::/64.
    val forInstances = Inet6Subnet(
      properties.get("subnet.instance")
        .orElse(ipv6.map(_.getHostAddress ++ "/64"))
        .orElse(ipv4.map(InetAddressHelper.ipv6(_))
          .map(_.getHostAddress ++ "/64"))
        .getOrElse("2001:db8:1:1:/64")
    )
    // The service subnet should be the one specified, or if none is specified
    // then the highest /96 under the instance subnet is used.
    val forServices = properties.get("subnet.service").map(Inet6Subnet(_))
      .getOrElse(forInstances.highest(96))

    /*  The prefix 2001:db8::/32 used above is reserved for documentation.

        http://tools.ietf.org/html/rfc3849

        "The document describes the use of the IPv6 address
         prefix 2001:DB8::/32 as a reserved prefix for use
         in documentation."

       Configurations that don't specify a service subnet and provide a way to
       derive the instance subnet are thus effectively limited to serving as
       demonstrations.
    */

    Config(
      localIPv4 = localIPv4,
      instanceSubnet = forInstances,
      serviceSubnet = forServices,
      rehearsal = properties.get("rehearsal").map(_.toBoolean).getOrElse(false),
      netState = properties.getOrElse("state.net", "/tmp/svcnet.json"),
      ipState = properties.getOrElse("state.ip", "/var/lib/svcnet.db"),
      logLevel = properties.getOrElse("log.level", "INFO"),
      logTimestamp = properties.getOrElse("log.timestamp", "short"),
      nsPort = properties.get("ns.port").map(_.toInt).getOrElse(9001),
      httpPort = properties.get("http.port").map(_.toInt).getOrElse(9000)
    )
  }
}
