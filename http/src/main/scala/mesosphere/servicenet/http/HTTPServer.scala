package mesosphere.servicenet.http

import unfiltered.jetty.Http
import unfiltered.request._
import unfiltered.response._

object HTTPServer extends App {

  val echo = unfiltered.filter.Planify {
    case Path(Seg(p :: Nil)) => ResponseString(p)
  }

  Http.anylocal.filter(echo).run

}