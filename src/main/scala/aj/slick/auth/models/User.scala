package aj.slick.auth

import aj.slick.Profile
import aj.slick.tables._

import com.github.tototoshi.slick.MySQLJodaSupport._
import org.joda.time._
import com.github.t3hnar.bcrypt._

case class User(
  id: Option[Long],
  username: String,
  email: String,
  crypt: String,
  created_at: DateTime = new DateTime()
)

case class Permission(
  id: Option[Long],
  name: String,
  description: String
)

case class Group(
  id: Option[Long],
  name: String,
  description: String
)

/**
 * ActiveSlick component with the user table
 */
trait UserComponent extends TableWithId { this: Profile =>

  import profile.simple._

  class UserTable(tag: Tag) extends Table[User](tag, "auth_users") with HasId[Long] {
    // fields
    def id = column[Long]("uid", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def email = column[String]("email")
    def crypt = column[String]("crypt")
    def created_at = column[DateTime]("created_at")

    // constraints
    def unique_email = index(s"${tableName}__unique_email", email, unique=true)

    // mappings
    override def * = (id.?, username, email, crypt, created_at) <> (
      (x: (Option[Long], String, String, String, DateTime)) => new User(x._1, x._2, x._3, x._4, x._5),
      User.unapply
    )
  }

  val Users = TableQuery[UserTable]

  class PermissionTable(tag: Tag) extends Table[Permission](tag, "auth_permissions") with HasId[Long] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[String]("description")

    def unique_name = index(s"${tableName}__unique_name", name, unique=true)

    def * = (id.?, name, description) <> (Permission.tupled, Permission.unapply)
  }

  val Permissions = TableQuery[PermissionTable]

  class GroupTable(tag: Tag) extends Table[Group](tag, "auth)groups") with HasId[Long] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[String]("description")

    def * = (id.?, name, description) <> (Group.tupled, Group.unapply)
  }

  val Groups = TableQuery[GroupTable]

  class UserPermissionTable(tag: Tag) extends Table[(Long, Long)](tag, "auth_user_permissions") {
    def userId = column[Long]("userId")
    def permission = column[Long]("permission")

    def key = index(s"${tableName}__unique", (userId, permission), unique=true)
    def user_fk = foreignKey(s"${tableName}__user_fk", userId, Users)(_.id)
    def permission_fk = foreignKey(s"${tableName}__permission_fk", permission, Permissions)(_.id)

    def * = (userId, permission)
  }

  val UserPermissions = TableQuery[UserPermissionTable]

  class UserGroupTable(tag: Tag) extends Table[(Long, Long)](tag, "auth_user_groups") {
    def userId = column[Long]("userId")
    def groupId = column[Long]("groupId")
    def key = index(s"${tableName}__unique", (userId, groupId), unique = true)
    def user_fk = foreignKey(s"${tableName}__user_fk", userId, Users)(_.id)
    def group_fk = foreignKey(s"${tableName}__group_fk", groupId, Groups)(_.id)

    def * = (userId, groupId)
  }

  val UserGroups = TableQuery[UserGroupTable]

  class GroupPermissionTable(tag: Tag) extends Table[(Long, Long)](tag, "auth_group_permissions") {
    def groupId = column[Long]("groupId")
    def permission = column[Long]("permission")

    def key = index(s"${tableName}__unique", (groupId, permission), unique=true)
    def group_fk = foreignKey(s"${tableName}__group_fk", groupId, Groups)(_.id)
    def permission_fk = foreignKey(s"${tableName}__permission_fk", permission, Permissions)(_.id)

    def * = (groupId, permission)
  }

  val GroupPermissions = TableQuery[GroupPermissionTable]

  implicit class UsersExt(users: TableQuery[UserTable]) {
    def fromLogin(username: String, key: String)(implicit session: Session): Option[User] = {
      Users
        .filter(_.username === username)
        .firstOption
        .filter(u => key.isBcrypted(u.crypt))
    }

    def withPermissions = {
      val userperms = for {
        (u, (_, perm)) <- Users
          .leftJoin(
            UserPermissions.withPermissions
          ).on(_.id === _._1)
      } yield (u, perm)

      val groupperms = for {
        ((u, _), (_, perm)) <- Users
          .innerJoin(UserGroups).on(_.id === _.userId)
          .innerJoin(
            GroupPermissions.withPermissions
          ).on(_._2.groupId === _._1)
      } yield (u, perm)

      userperms ++ groupperms
    }
  }

  implicit class GroupPermissionsExt(query: TableQuery[GroupPermissionTable]) {
    def withPermissions = query
      .innerJoin(Permissions).on(_.permission === _.id)
      .map { case (g, p) => Tuple2(g.groupId, p.id)}
  }

  implicit class UserPermissionsExt(query: TableQuery[UserPermissionTable]) {
    def withPermissions = query
      .innerJoin(Permissions).on(_.permission === _.id)
      .map { case (u, p) => (u.userId, p.id)}
  }
}
