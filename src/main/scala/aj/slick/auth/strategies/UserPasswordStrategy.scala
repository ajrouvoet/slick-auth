package aj.slick.auth.strategies

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import aj.slick.auth._
import io.strongtyped.active.slick.ActiveSlick
import org.scalatra._
import org.scalatra.auth.{ScentryConfig, ScentryStrategy, ScentrySupport}

trait ScentryComponent { self: ActiveSlick with Auth =>

  import jdbcDriver.simple._

  trait AuthSupport extends ScentrySupport[User] {
    self: ScalatraBase =>

    // we need access to the database
    // this will be injected in the extending controller
    val db: Database

    // use default scentry config
    protected def scentryConfig = new ScentryConfig {}.asInstanceOf[ScentryConfiguration]

    override protected def toSession = {
      case User(Some(id), _, _, _, _) => id.toString
    }

    override protected def fromSession = {
      case id: String =>
        db.withSession { implicit session =>
          Users.filter(_.id === id.toInt).firstOption.getOrElse {
            throw new RuntimeException("Logged in user no longer exist.")
          }
        }
    }

    override protected def configureScentry = {
      scentry.unauthenticated {
        scentry.strategies("UserPassword").unauthenticated()
      }
    }

    override protected def registerAuthStrategies: Unit = {
      scentry.register("UserPassword", app => new UserPasswordStrategy(db, app))
    }

    /**
     * Utility function for redirecting away from anything that
     */
    def ensureAnonymous(next: String = scentryConfig.returnTo)(action: => AnyRef) = {
      if (isAuthenticated)
        redirect(next)
      else
        action
    }

    /**
     * Utility function to ensure the user is authenticated, and otherwise calls doOnUnauthenticated
     */
    def secure(action: => AnyRef) = {
      if (!isAuthenticated)
        doOnUnauthenticated()
      else
        action
    }

    /**
     * Determines what happens when a user must be authenticated but isn't
     */
    def doOnUnauthenticated() = {
      halt(Unauthorized("You must login to access this url"))
    }
  }

  case class UserPasswordStrategy(db: Database, app: ScalatraBase) extends ScentryStrategy[User] with Logging {
    def uname(implicit request: HttpServletRequest) = app.params.get("user")
    def key(implicit request: HttpServletRequest) = app.params.get("key")

    import userimplicits._

    override def isValid(implicit request: HttpServletRequest): Boolean = {
      uname.isDefined && key.isDefined
    }

    override def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) = db.withSession {
      implicit session =>
        for {
          username <- uname
          pw <- key
          usr <- Users.fromLogin(username, pw)
        } yield usr
    }
  }
}
