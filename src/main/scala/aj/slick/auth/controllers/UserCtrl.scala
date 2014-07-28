package aj.slick.auth

import io.strongtyped.active.slick.ActiveSlick
import org.scalatra.ScalatraServlet
import com.github.t3hnar.bcrypt._

trait UserCtrlComponent { this: ActiveSlick with Auth =>

  import jdbcDriver.simple._

  class UserCtrl(db: Database) extends ScalatraServlet with RestCtrl {

    post("/login") {
      db withSession { implicit session =>
        val username = params.getOrElse("user", "")
        val passwd = params.getAsOrElse[String]("key", "")
        val bcrypt = Users
          .filter(_.username === username)
          .map(_.crypt)
          .firstOption

        bcrypt.map(crypt => passwd.isBcrypted(crypt)) match {
          case Some(true) => "now logged in!"
          case _ => s"failed to login"
        }
      }
    }

    getJson("/list") {
      // get all users
      val users = db withSession { implicit session =>
        Users.list
      }

      // remove sensitive fields
      users.toJson.removeField {
        case ("crypt", _) => true
        case _ => false
      }
    }

  }

}
