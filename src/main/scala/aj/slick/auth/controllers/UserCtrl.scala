package aj.slick.auth

import aj.slick.auth.controllers.RestCtrl
import io.strongtyped.active.slick.ActiveSlick
import org.scalatra.auth.ScentryConfig
import org.scalatra.{ScalatraServlet, UrlGeneratorSupport}

/**
 * This component mixes in the UserCtrl class
 */
trait UserCtrlComponent { this: ActiveSlick with AuthComponent =>
  import jdbcDriver.simple._

  class UserCtrl(val db: Database)
    extends ScalatraServlet
    with UrlGeneratorSupport
    with RestCtrl
    with AuthSupport { controller =>

    // set login url by reverse lookup of the login route
    override protected def scentryConfig = new ScentryConfig {
        override val login = url(controller.login)
      }.asInstanceOf[ScentryConfiguration]

    // login service
    val login = post("/login") {
      ensureAnonymous(params.getOrElse("next", scentryConfig.returnTo)) {
        // use userpassword strategy to authenticate
        scentry.authenticate("UserPassword") match {
          case Some(u) => s"Welcome ${u.username}"
          case None => "Failed to log in"
        }
      }
    }

    // logout service
    post("/logout") {
      scentry.logout()
    }

    // list all users
    getJson("/list") {
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
