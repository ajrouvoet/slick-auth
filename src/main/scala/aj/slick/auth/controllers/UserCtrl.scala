package aj.slick.auth

import aj.slick.auth.controllers.RestComponent
import io.strongtyped.active.slick.ActiveSlick
import org.scalatra.auth.ScentryConfig
import org.scalatra.{ScalatraServlet, UrlGeneratorSupport}

/**
 * This component mixes in the UserCtrl class
 */
trait UserCtrlComponent extends RestComponent { this: ActiveSlick with AuthComponent =>
  import jdbcDriver.simple._

  class AuthCtrl(val db: Database) extends ScalatraServlet with UrlGeneratorSupport with RestCtrl with AuthSupport {
    ctrl =>

    // set login url by reverse lookup of the login route
    override protected def scentryConfig = new ScentryConfig {
      override val login = url(ctrl.login)
    }.asInstanceOf[ScentryConfiguration]

    // login service
    val login = post("/login") {
      ensureAnonymous(params.getOrElse("next", scentryConfig.returnTo)) {
        // use userpassword strategy to authenticate
        scentry.authenticate("UserPassword") match {
          case Some(u) => u.toJson
          case None => "errors" -> List("Failed to login")
        }
      }
    }

    // logout service
    post("/logout") {
      scentry.logout()
    }
  }

  class UserCtrl(val db: Database)
    extends ScalatraServlet
    with RestCtrl
    with AuthSupport { controller =>

    // list all users
    get("/list") {
      secure {
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
}
