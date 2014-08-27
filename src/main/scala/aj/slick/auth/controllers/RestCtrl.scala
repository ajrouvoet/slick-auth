package aj.slick.auth.controllers

import org.json4s.jackson.Serialization._
import org.json4s.JsonAST.JValue
import org.json4s.{DefaultFormats, Extraction, Formats}
import org.scalatra._

trait RestCtrl extends ScalatraServlet {

  implicit val jsonFormats: Formats = DefaultFormats

  implicit class JSONable(x: AnyRef) {
    def toJson: JValue  = Extraction.decompose(x)
  }

  def getJson(transformers: RouteTransformer*)(action: => AnyRef): Route = {
    get(transformers: _*) {
      contentType = "application/json"
      action match {
        case v: JValue => writePretty(v)
        case o => o
      }
    }
  }

}

