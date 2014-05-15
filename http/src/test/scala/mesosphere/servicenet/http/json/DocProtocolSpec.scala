package mesosphere.servicenet.http.json

import mesosphere.servicenet.dsl._
import mesosphere.servicenet.util.{ InetAddressHelper, Spec }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.net.{ InetAddress, Inet4Address, Inet6Address }

class DocProtocolSpec extends Spec {

  object Fixture {
    val inet4Address = InetAddressHelper.ipv4("192.168.1.115")

    val inet6Address =
      InetAddressHelper.ipv6("fc75:0000:0000:0000:0000:9fb2:0000:0804")
  }

  import DocProtocol._

  "DocProtocol" should "read and write Inet4Address" in {
    import Fixture._

    val json = Json.toJson(inet4Address)
    json should equal (JsString("192.168.1.115"))

    val readResult = json.as[Inet4Address]
    readResult should equal (inet4Address)

    intercept[JsResultException] {
      JsString("one fish two fish").as[Inet4Address]
    }

    intercept[JsResultException] {
      JsString("FC75:0000:0000:0000:0000:9FB2:0000:0804").as[Inet4Address]
    }

    intercept[JsResultException] {
      JsArray(Seq(JsString("one"), JsString("two"))).as[Inet4Address]
    }
  }

  "DocProtocol" should "read and write Inet6Address" in {
    import Fixture._

    val json = Json.toJson(inet6Address)
    json should equal (JsString("fc75:0:0:0:0:9fb2:0:804"))

    val readResult = json.as[Inet6Address]
    readResult should equal (inet6Address)

    intercept[JsResultException] {
      JsString("one fish two fish").as[Inet6Address]
    }

    intercept[JsResultException] {
      JsString("192.168.1.115").as[Inet6Address]
    }

    intercept[JsResultException] {
      JsArray(Seq(JsString("one"), JsString("two"))).as[Inet6Address]
    }

  }

}
