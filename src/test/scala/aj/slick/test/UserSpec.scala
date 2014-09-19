package aj.slick.auth.test

import org.scalatest._

import aj.slick.test._
import aj.slick.auth._
import aj.slick.fixtures._

class UserSpec extends SlickSpec with UserComponent {

  import profile.simple._

  val alice = User(Some(1L), "Alice", "alice@dot.dot", "1234")
  val bob = User(Some(2L), "Bob", "bob@dot.dot", "2345")
  val carice = User(Some(3L), "Carice", "carice@dot.dot", "3456")

  object usersFix extends Fixture(Users, alice, bob, carice)

  val pall = Permission(Some(1L), "everything", "Do Everything")
  val pnone = Permission(Some(2L), "nothing", "Get your hands off")
  val padmin = Permission(Some(3L), "administrate", "Basically everything again")
  object permissionsFix extends Fixture(Permissions, pall, pnone, padmin)

  val admins = Group(Some(1L), "admins", "Administrator group")
  object groupFix extends Fixture(Groups, admins)

  object userpermFix extends Fixture(UserPermissions,
    (alice.id.get, pall.id.get),
    (carice.id.get, padmin.id.get)
  )
  object grouppermFix extends Fixture(GroupPermissions, (1L, 3L))
  object usergroupsFix extends Fixture(UserGroups,
    (alice.id.get, 1L),
    (carice.id.get, 1L)
  )

  def withAuthFix(implicit s: Session) = {
    // create the tables
    ( Users.ddl ++
      Permissions.ddl ++
      Groups.ddl ++
      UserPermissions.ddl ++
      UserGroups.ddl ++
      GroupPermissions.ddl
    ).create

    // insert the data
    usersFix.install()
    permissionsFix.install()
    userpermFix.install()
    groupFix.install()
    grouppermFix.install()
    usergroupsFix.install()
  }

  "users fixture" should "contain user Alice" in db.withSession { implicit s =>
    withAuthFix

    Users.filter(_.username === "Alice").first should have ('username ("Alice"))
  }

  "user groups" should "return all user groups" in db.withSession { implicit s =>
    withAuthFix

    Users.groups.list should be (List(admins))
  }

  "bob" should "have no permissions" in db.withSession { implicit s =>
    withAuthFix

    Users.filter(_.username === "Bob").permissions.list should be (List())
  }

  "user permissions" should "should be distinct" in db.withSession { implicit s =>
    withAuthFix

    Users.filter(_.username === "Carice").permissions.list should be (List(padmin))
  }

  "user withPermissions" should "contain ALL user permissions, including group inherited" in
  db.withSession { implicit s =>
    withAuthFix

    Users
      .filter(_.username === "Alice")
      .permissions
      .sortBy(_.id)
      .list should be (List(pall, padmin))
  }
}
