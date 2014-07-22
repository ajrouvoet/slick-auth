package aj.slick

import org.slf4j.LoggerFactory

package object auth {

  /**
   * For anything that logs anything
   */
  trait Logging {
    val logger = LoggerFactory.getLogger(getClass)
  }
}
