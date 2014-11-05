package aj.scalatra.controllers

import aj.slick.Profile
import aj.slick.tables._

import org.scalatra._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.slf4j.{Logger, LoggerFactory}

trait RestComponent extends TableWithId { self: Profile =>

  import profile.simple._

  trait RestCtrl extends ScalatraServlet with JsonDSL with DoubleMode {

    case class BadRequestException(msg: String) extends Exception(msg)

    val logger = LoggerFactory.getLogger(getClass)

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

    override def addRoute(method: HttpMethod, transformers: Seq[RouteTransformer], action: => Any): Route = {
      super.addRoute(method, transformers, toPrettyJson(action))
    }

    def jsonError(msg: String): String = pretty(render(("message", msg) ~ JObject()))

    def errorHandling: PartialFunction[Throwable, Any] = {
      case e: BadRequestException =>
        contentType = "application/json"
        halt(BadRequest(jsonError(e.getMessage)))

      case e =>
        contentType = "application/json"
        halt(InternalServerError(jsonError(e.getMessage)))
        logger.warn(e.getMessage)
    }

    error(errorHandling)
  }

  /**
   * Abstraction of typical Rest controller
   */
  abstract class RestFullCtrl[U <: AnyRef, Id : BaseColumnType, T <: Table[U] with HasId[Id]]
    ( val db: Database,
      val tablequery: TableQuery[T],
      val getQuery: Query[T, U]
    )(implicit manifest: Manifest[U]) extends ScalatraServlet with RestCtrl {

    implicit class Serializable(x: U) {
      def toJson: JValue = serializer(x)
    }

    implicit class Deserializable(x: JValue) {
      def fromJson: U = deserializer(x)
    }

    /**
     * Overload that allows you to omit the getQuery parameter,
     * it is assumed to be equal to tablequery
     */
    def this
      (db: Database, tablequery: TableQuery[T])
      (implicit manifest: Manifest[U]) = this(db, tablequery, tablequery)

    def parseId(s: String): Option[Id]

    def idParam: Id = parseId(params("id")).getOrElse(
      throw BadRequestException("Needed integer id as parameter")
    )

    def withId(u: U, id: Id): U

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
        Ok(withId(inst, tablequery.insertReturnId(inst)).toJson)
      }
    }

    post("/:id") {
      db withSession { implicit session =>
        val inst = deserializer(parse(request.body))
        val id = idParam
        val x = tablequery.filterById(id).update(inst)
        Ok(withId(inst, id).toJson)
      }
    }

    delete("/:id") {
      db withSession { implicit session =>
        val id = idParam
        val x = tablequery.filterById(id).delete
        Ok(id)
      }
    }

    // error handling
    override def errorHandling: PartialFunction[Throwable, Any] = {
      case e: SlickException =>
        logger.warn(e.getMessage)
        contentType = "application/json"
        halt(BadRequest(jsonError("Woops, database constraint failed")))

      case e => super.errorHandling(e)
    }
  }
}
