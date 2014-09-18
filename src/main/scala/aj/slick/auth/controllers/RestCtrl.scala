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

  /**
   * Abstraction of typical Rest controller
   */
  abstract class RestFullCtrl[U <: AnyRef, Id : BaseColumnType, T <: IdTable[U, Id]]
    (
      val db: Database,
      val tablequery: TableQuery[T],
      val getQuery: Query[T, U]
    )(implicit
      manifest: Manifest[U],
      text: TableQuery[T] => BaseIdTableExt[U,Id]
    ) extends ScalatraServlet with RestCtrl {

    /**
     * Overload that allows you to omit the getQuery parameter,
     * it is assumed to be equal to tablequery
     */
    def this
      (db: Database, tablequery: TableQuery[T])
      (implicit manifest: Manifest[U], text: TableQuery[T] => BaseIdTableExt[U,Id]) =
      this(db, tablequery, tablequery)

    def parseId(s: String): Option[Id]

    def idParam: Id = parseId(params("id")).getOrElse(
      halt(BadRequest("Needed integer id as parameter"))
    )

    val defaultFormats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

    val deserializer: PartialFunction[JValue, U] = {
      case u: JObject => u.extract[U](defaultFormats, manifest)
    }

    val serializer: PartialFunction[Any, JValue] = {
      case u:U => Extraction.decompose(u)(defaultFormats)
    }

    override implicit val formats = org.json4s.DefaultFormats ++
      org.json4s.ext.JodaTimeSerializers.all +
      new CustomSerializer[U](formats => (deserializer, serializer))(manifest)

    // routes

    get("/") {
      db withSession { implicit session =>
        getQuery.list.toJson
      }
    }

    get("/:id") {
      db withSession { implicit session =>
        getQuery.filter(_.id === idParam).first.toJson
      }
    }

    post("/") {
      val inst = deserializer(parse(request.body))

      db withSession { implicit session =>
        tablequery.save(inst).toJson
      }
    }

    post("/:id") {
      db withSession { implicit session =>
        val inst = tablequery.withId(deserializer(parse(request.body)), idParam)
        tablequery.save(inst).toJson
      }
    }

    delete("/:id") {
      db withSession { implicit session =>
        val x = tablequery.deleteById(idParam)
        Ok(x)
      }
    }
  }
}
