package aj.scalatra.auth.strategies

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import aj.slick.auth._
import aj.scalatra.auth._
import aj.slick.Profile

import org.scalatra._
import org.scalatra.auth.{ScentryConfig, ScentryStrategy, ScentrySupport}

trait ScentryComponent { self: Profile with AuthComponent =>

  import profile.simple._

  trait AuthSupport extends ScentrySupport[User] { self: ScalatraBase =>

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
          Users.filter(_.id === id.toLong).firstOption.getOrElse {
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
      scentry.register("Mock", app => new MockStrategy(db, app))
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

  case class UserPasswordStrategy(db: Database, app: ScalatraBase) extends ScentryStrategy[User] {
    def uname(implicit request: HttpServletRequest) = app.params.get("user")
    def key(implicit request: HttpServletRequest) = app.params.get("key")

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

  /**
   * Scentry mocking strategy that only needs a username to authenticate
   */
  case class MockStrategy(db: Database, app: ScalatraBase) extends ScentryStrategy[User] {
    def uname(implicit request: HttpServletRequest) = app.params.get("user")

    override def isValid(implicit request: HttpServletRequest): Boolean = {
      uname.isDefined
    }

    override def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) = db.withSession {
      // return requested user, do not authenticate
      implicit session =>
        Users.filter(_.username === uname).firstOption
    }
  }
}
