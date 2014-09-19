package aj.slick.auth.test

import org.scalatest._

import aj.slick.test._
import aj.slick.auth._
import aj.slick.fixtures._

class UserSpec extends SlickSpec with UserComponent {

  import profile.simple._

  object usersFix extends Fixture(Users,
    User(None, "Alice", "alice@dot.dot", "1234")
  )

  object permissionsFix extends Fixture(Permissions,
    Permission(None, "everything", "Do Everything"),
    Permission(None, "nothing", "Get your hands off")
  )

  def withAuthFix(implicit s: Session) = {
    // create the tables
    (Users.ddl ++ Permissions.ddl).create

    // insert the data
    usersFix.install()
    permissionsFix.install()
  }

  "users fixture" should "contain user Alice" in db.withSession { implicit s =>
    withAuthFix

    Users.first should have ('username ("Alice"))
  }
}
