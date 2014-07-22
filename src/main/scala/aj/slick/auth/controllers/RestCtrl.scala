package aj.slick.auth

import org.json4s.jackson.Serialization._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

trait RestCtrl extends ScalatraServlet {

  def getJson(transformers: RouteTransformer*)(action: => AnyRef): Route = {
    implicit val jsonFormats: Formats = DefaultFormats
    get(transformers: _*) {
      writePretty(action)
    }
  }

}

