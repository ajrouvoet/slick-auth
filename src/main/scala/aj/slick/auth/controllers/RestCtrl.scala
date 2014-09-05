package aj.slick.auth.controllers

import org.scalatra._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization._
import org.json4s._
import org.json4s.jackson.JsonMethods._

trait RestCtrl extends ScalatraServlet with JsonDSL with DoubleMode {


  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  implicit class JSONable(x: AnyRef) {
    def toJson: JValue  = Extraction.decompose(x)
  }

  private def toPrettyJson(action: => Any) = action match {
    // try to get some json out
    case a@ActionResult(s, o: JValue, h) =>
      contentType = "application/json"
      a.copy(body = pretty(render(o)))

    case j:JValue =>
      contentType = "application/json"
      Ok(pretty(render(j)))

    // pass the stuff through
    case others => others
  }

  override def get(transformers: RouteTransformer*)(action: => Any): Route = {
    super.get(transformers: _*) {
      toPrettyJson(action)
    }
  }

  override def post(transformers: RouteTransformer*)(action: => Any): Route = {
    super.post(transformers: _*) {
      toPrettyJson(action)
    }
  }
}


