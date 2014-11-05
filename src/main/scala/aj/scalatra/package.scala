package aj.scalatra

import aj.scalatra.auth.controllers.SecuredComponent
import aj.scalatra.auth.strategies.ScentryComponent
import aj.slick.Profile
import aj.slick.auth._

package object auth {

  /**
   * The main Auth support trait that mixes in access to the user classes
   * and the scentry strategies/support classes
   */
  trait AuthComponent extends UserComponent with ScentryComponent with SecuredComponent {
    self: Profile =>
  }
}
