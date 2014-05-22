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
  implicit val config: Config = Config())
    extends DocProtocol with Logging {

  @volatile protected var doc: Doc = Doc(Nil, Nil, Nil, Nil)

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
      val f = new File(config.stateStore)
      if (f.exists()) {
        val data = IO.read(f)
        if (data.length <= 0) return
        log info s"Reading state from: ${config.stateStore}"
        Json.parse(data).validate[Doc] match {
          case JsSuccess(docPrime, _) => doc = docPrime
          case error: JsError         => log warn "Ignoring bad state store"
        }
      }
    }

    def store() {
      log info s"Writing state to: ${config.stateStore}"
      val json = Json.toJson(doc)
      IO.replace(new File(config.stateStore), Json.prettyPrint(json))
    }
  }

  object RestRoutes extends unfiltered.filter.Plan {
    def intent = {
      case req @ Path(Seg("doc" :: Nil)) => req match {

        case GET(_) =>
          ResponseHeader("Content-Type", Set("application/json")) ~>
            ResponseString(Json.toJson(doc).toString)

        case PUT(_) =>
          Json.parse(Body.bytes(req)).validate[Doc] match {
            case success: JsSuccess[Doc] =>
              update(success.get)
              ResponseString("OK")
            case error: JsError =>
              BadRequest ~>
                ResponseHeader("Content-Type", Set("application/json")) ~>
                ResponseString(JsError.toFlatJson(error).toString)
          }

        case PATCH(_) =>
          MethodNotAllowed ~> ResponseString("Must be GET or PUT")
      }

      case _ => NotFound ~> ResponseString("Not found")
    }
  }

  def run(port: Int = config.httpPort) {
    State.load()
    Http(port).filter(RestRoutes).run
  }
}

object HTTPServer extends App {
  (new HTTPServer).run()
}
