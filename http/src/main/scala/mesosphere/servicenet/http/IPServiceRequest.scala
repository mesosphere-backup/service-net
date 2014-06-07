package mesosphere.servicenet.http

import play.api.libs.json._

case class IPServiceRequest(name: String) {
  require(IPServiceRequest.ldhRE.findFirstMatchIn(name).nonEmpty,
    "Name must be an LDH domain name")
}

object IPServiceRequest {
  val ldhRE = ("^([a-z0-9]|[a-z0-9][a-z0-9-]*[a-z0-9])" +
    "([.]([a-z0-9]|[a-z0-9][a-z0-9-]*[a-z0-9]))*$").r

  implicit val inet6AddressFormat = new Format[IPServiceRequest] {
    def writes(req: IPServiceRequest): JsValue =
      Json.toJson(Map("name" -> req.name))

    def reads(json: JsValue): JsResult[IPServiceRequest] = json match {
      case o: JsObject => o.value.get("name") match {
        case None => JsError("IPServiceRequest must specify a \"name\"")
        case Some(thing) => thing match {
          case JsString(name) => try {
            JsSuccess(IPServiceRequest(name))
          }
          catch {
            case e: IllegalArgumentException => JsError(e.getMessage)
          }
          case _ => JsError("IPServiceRequest must specify a \"name\"")
        }
        case _ => JsError("IPServiceRequest must specify a \"name\"")
      }
      case _ => JsError("IPServiceRequest must be a map")
    }
  }
}
