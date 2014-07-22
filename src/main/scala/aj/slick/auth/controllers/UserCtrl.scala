package aj.slick.auth.controllers

import io.strongtyped.active.slick.ActiveSlick
import org.nwb.models._
import org.scalatra.ScalatraServlet

trait UserCtrlComponent { this: ActiveSlick with Auth =>

  import jdbcDriver.simple._

  class UserCtrl(db: Database) extends ScalatraServlet with RestCtrl {

    before() {
      contentType = "application/json"
    }

    getJson("/list") {
      db withSession { implicit session =>
        Users.list
      }
    }

  }

}
