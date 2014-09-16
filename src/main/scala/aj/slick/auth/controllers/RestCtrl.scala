package aj.slick.auth.controllers

import io.strongtyped.active.slick.ActiveSlick

import org.scalatra._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization._
import org.json4s._
import org.json4s.jackson.JsonMethods._

trait RestComponent { self: ActiveSlick =>

  import jdbcDriver.simple._

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

  abstract class RestFullCtrl[U <: AnyRef <% Model[U], Id](
      val query: BaseIdTableExt[U,Id],
      val db: Database
    )(implicit manifest: Manifest[U]) extends ScalatraServlet with RestCtrl {

    def parseId(s: String): Id

    // get some good serialization defaults
    val defaultFormats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

    val deserializer: PartialFunction[JValue, U] = {
      case u: JObject => u.extract[U](defaultFormats, manifest)
    }

    val serializer: PartialFunction[Any, JValue] = {
      case u:U => Extraction.decompose(u)(defaultFormats)
    }

    override implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all +
      new CustomSerializer[U](formats => (deserializer, serializer))(manifest)

    // routes

    get("/") {
      db withSession { implicit session =>
        query.list.toJson
      }
    }

    get("/:id") {
      db withSession { implicit session =>
        query.filterById(parseId(params("id"))).first.toJson
      }
    }

    post("/") {
      val inst = deserializer(parse(request.body))

      db withSession { implicit session =>
        inst.save().toJson
      }
    }

    post("/:id") {
      db withSession { implicit session =>
        val inst = query.withId(deserializer(parse(request.body)), parseId(params("id")))
        inst.save().toJson
      }
    }

    delete("/:id") {
      db withSession { implicit session =>
        val x = query.deleteById(parseId(params("id")))
        Ok(x)
      }
    }
  }
}
