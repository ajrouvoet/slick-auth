package aj.slick.auth

import com.github.tototoshi.slick.MySQLJodaSupport._
import io.strongtyped.active.slick.ActiveSlick
import io.strongtyped.active.slick.models.Identifiable
import org.joda.time._
import com.github.t3hnar.bcrypt._

case class User(
  id: Option[Int],
  username: String,
  email: String,
  crypt: String,
  created_at: DateTime = new DateTime()
) extends Identifiable[User] {
  override type Id = Int
  override def withId(id: Id): User = copy(id = Some(id))
}

object User {
  def apply(username: String, email: String, crypt: String) = new User(None, username, email, crypt)
}

case class Permission(
  id: Option[String],
  description: String
) extends Identifiable[Permission] {
  override type Id = String
  override def withId(id: Id): Permission = copy(id=Some(id))
}

case class Group(
  id: Option[Long],
  name: String,
  description: String
) extends Identifiable[Group] {
  override type Id = Long
  override def withId(id: Id) = copy(id=Some(id))
}

/**
 * ActiveSlick component with the user table
 */
trait UserComponent { this: ActiveSlick =>

  import jdbcDriver.simple._

  class UserTable(tag: Tag) extends IdTable[User, Int](tag, "users") {
    // fields
    def id = column[Int]("uid", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def email = column[String]("email")
    def crypt = column[String]("crypt")
    def created_at = column[DateTime]("created_at")

    // constraints
    def unique_email = index("unique_email", email, unique=true)

    // mappings
    override def * = (id.?, username, email, crypt, created_at) <> (
      (x: (Option[Int], String, String, String, DateTime)) => new User(x._1, x._2, x._3, x._4, x._5),
      User.unapply
    )
  }

  val Users = TableQuery[UserTable]

  object userimplicits extends ModelImplicits[User](Users) {
    implicit class UsersExt(users: TableQuery[UserTable]) extends QueryExt(users) {
      def fromLogin(username: String, key: String)(implicit session: Session): Option[User] = {
        Users
          .filter(_.username === username)
          .firstOption
          .filter(u => key.isBcrypted(u.crypt))
      }
    }
  }

  class PermissionTable(tag: Tag) extends IdTable[Permission, String](tag, "permissions") {
    def id = column[String]("id", O.PrimaryKey)
    def description = column[String]("description")

    def * = (id.?, description) <> (Permission.tupled, Permission.unapply)
  }

  val Permissions = TableQuery[PermissionTable]

  class GroupTable(tag: Tag) extends IdTable[Group, Long](tag, "groups") {
    def id = column[Long]("id", O.PrimaryKey)
    def name = column[String]("name")
    def description = column[String]("description")

    def * = (id.?, name, description) <> (Group.tupled, Group.unapply)
  }

  val Groups = TableQuery[GroupTable]

  class UserPermissionTable(tag: Tag) extends Table[(Int, String)](tag, "user_permissions") {
    def userId = column[Int]("userId")
    def permission = column[String]("permission")

    def key = index("unique_user_permission", (userId, permission), unique=true)
    def user_fk = foreignKey("user_fk", userId, Users)(_.id)
    def permission_fk = foreignKey("permission_fk", permission, Permissions)(_.id)

    def * = (userId, permission)
  }

  val UserPermissions = TableQuery[UserPermissionTable]

  class GroupPermissionTable(tag: Tag) extends Table[(Long, String)](tag, "user_permissions") {
    def groupId = column[Long]("groupId")
    def permission = column[String]("permission")

    def key = index("unique_group_permission", (groupId, permission), unique=true)
    def group_fk = foreignKey("group_fk", groupId, Groups)(_.id)
    def permission_fk = foreignKey("permission_fk", permission, Permissions)(_.id)

    def * = (groupId, permission)
  }

  val GroupPermissions = TableQuery[GroupPermissionTable]
}
