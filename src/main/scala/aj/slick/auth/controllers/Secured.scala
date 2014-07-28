package aj.slick.auth.controllers

import aj.slick.auth.Auth
import org.scalatra.ScalatraServlet

trait SecuredComponent { this: Auth =>

  /**
   * Trait that ensurs that before every request, the user is authenticated.
   * And otherwise calls scentry's authenticate method
   */
  trait Secured { this: ScalatraServlet with AuthSupport =>

    before() {
      if(!isAuthenticated)
        doOnUnauthenticated()
    }
  }
}
