package mesosphere.servicenet.http

import play.api.libs.json._
import unfiltered.jetty.Http
import unfiltered.request._
import unfiltered.response._

import mesosphere.servicenet.config.Config
import mesosphere.servicenet.dsl._
import mesosphere.servicenet.http.json.DocProtocol
import mesosphere.servicenet.util.{ IO, Logging }
import java.io.File

class HTTPServer(updated: (Diff, Doc) => Unit = (diff: Diff, doc: Doc) => ())(
  implicit val conf: Config = Config())
    extends DocProtocol with Logging {

  @volatile protected var doc: Doc = Doc()

  def update(docPrime: Doc) {
    update(Diff(doc, docPrime))
  }

  // TODO: Control update rate -- allow updates only once every 10-100 millis
  def update(diff: Diff) = synchronized {
    log info diff.summary()
    doc = diff(doc)
    updated(diff, doc)
    State.store()
  }

  object State {
    def load(): Unit = synchronized {
      val f = new File(conf.stateStore)
      if (f.exists()) {
        val data = IO.read(f)
        if (data.length <= 0) return
        log info s"Reading state from: ${conf.stateStore}"
        Json.parse(data).validate[Doc] match {
          case JsSuccess(docPrime, _) => {
            doc = docPrime
            update(docPrime)
          }
          case error: JsError => log warn "Ignoring bad state store"
        }
      }
    }

    def store() {
      log info s"Writing state to: ${conf.stateStore}"
      val json = Json.toJson(doc)
      IO.replace(new File(conf.stateStore), Json.prettyPrint(json) + "\n")
    }
  }

  object RestRoutes extends unfiltered.filter.Plan {
    def intent = {
      case req @ Path(Seg("doc" :: Nil)) => req match {

        case GET(_) => jsonResponse(doc)

        case PUT(_) =>
          Json.parse(Body.bytes(req)).validate[Doc] match {
            case success: JsSuccess[Doc] =>
              update(success.get)
              ResponseString("OK")
            case error: JsError =>
              BadRequest ~> jsonResponse(error)
          }

        case PATCH(_) =>
          Json.parse(Body.bytes(req)).validate[Diff] match {
            case success: JsSuccess[Diff] =>
              update(success.get)
              ResponseString("OK")
            case error: JsError =>
              BadRequest ~> jsonResponse(error)
          }

        case DELETE(_) =>
          update(Doc())
          ResponseString("OK")
      }

      case req @ Path(Seg("subnet" :: Nil)) => req match {
        case GET(_) => jsonResponse(Map(
          "instance" -> conf.instanceSubnet,
          "service" -> conf.serviceSubnet
        ))
      }

      case _ => NotFound ~> ResponseString("Not found")
    }
  }

  def jsonResponse(json: JsValue): ResponseFunction[Any] =
    ResponseHeader("Content-Type", Set("application/json")) ~>
      ResponseString(Json.prettyPrint(json) + "\n")

  def jsonResponse(e: JsError): ResponseFunction[Any] =
    jsonResponse(JsError.toFlatJson(e))

  def jsonResponse[T](o: T)(implicit tjs: Writes[T]): ResponseFunction[Any] =
    jsonResponse(Json.toJson(o))

  def run(port: Int = conf.httpPort) {
    State.load()
    Http(port).filter(RestRoutes).run
  }
}

object HTTPServer extends App {
  (new HTTPServer).run()
}
