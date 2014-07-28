package aj.slick.auth

import io.strongtyped.active.slick.ActiveSlick
import org.scalatra.auth.ScentryConfig
import org.scalatra.{ScalatraServlet, UrlGeneratorSupport}

trait UserCtrlComponent { this: ActiveSlick with Auth =>

  import jdbcDriver.simple._

  class UserCtrl(val db: Database) extends ScalatraServlet
    with RestCtrl
    with AuthSupport
    with UrlGeneratorSupport
    with Logging {
    controller =>

    trait MyScentryConfig extends ScentryConfig {
      override val login = url(controller.login)
    }

    override protected def scentryConfig: ScentryConfiguration = new MyScentryConfig {}.asInstanceOf[ScentryConfiguration]

    val login = post("/login") {
      ensureAnonymous(params.getOrElse("next", scentryConfig.returnTo)) {
        // use userpassword strategy to authenticate
        scentry.authenticate("UserPassword") match {
          case Some(u) => s"Welcome ${u.username}"
          case None => "Failed to log in"
        }
      }
    }

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
