package aj.slick

import aj.slick.auth.controllers.SecuredComponent
import aj.slick.auth.strategies.ScentryComponent
import io.strongtyped.active.slick.ActiveSlick
import org.slf4j.LoggerFactory

package object auth {

  /**
   * For anything that logs anything
   */
  trait Logging {
    val logger = LoggerFactory.getLogger(getClass)
  }

  /**
   * The main Auth support trait that mixes in access to the user classes
   * and the scentry strategies/support
   */
  trait Auth extends UserComponent with ScentryComponent with SecuredComponent { self: ActiveSlick => }
}
