package aj.slick.auth

import aj.slick.Profile
import aj.slick.tables._

import com.github.tototoshi.slick.GenericJodaSupport
import org.joda.time._
import com.github.t3hnar.bcrypt._
import scala.slick.driver.JdbcDriver

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

  object PortableJodaSupport extends GenericJodaSupport(profile.asInstanceOf[JdbcDriver])
  import PortableJodaSupport._

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
    def columns = (id.?, username, email, crypt, created_at)
    override def * = columns <> (
      User.tupled,
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

  class GroupTable(tag: Tag) extends Table[Group](tag, "auth_groups") with HasId[Long] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[String]("description")

    def * = (id.?, name, description) <> (Group.tupled, Group.unapply)
  }

  val Groups = TableQuery[GroupTable]

  class UserPermissionTable(tag: Tag) extends Table[(Long, Long)](tag, "auth_user_permissions") {
    def userId = column[Long]("userId")
    def permissionId = column[Long]("permissionId")

    def key = index(s"${tableName}__unique", (userId, permissionId), unique=true)
    def user = foreignKey(s"${tableName}__user_fk", userId, Users)(_.id)
    def permission = foreignKey(s"${tableName}__permission_fk", permissionId, Permissions)(_.id)

    def * = (userId, permissionId)
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
    def permissionId = column[Long]("permissionId")

    def key = index(s"${tableName}__unique", (groupId, permissionId), unique=true)
    def group = foreignKey(s"${tableName}__group_fk", groupId, Groups)(_.id)
    def permission = foreignKey(s"${tableName}__permission_fk", permissionId, Permissions)(_.id)

    def * = (groupId, permissionId)
  }

  val GroupPermissions = TableQuery[GroupPermissionTable]

  implicit class UsersExt(query: Query[UserTable, User]) {
    def fromLogin(username: Column[String], key: String)(implicit session: Session): Option[User] = {
      query
        .filter(_.username === username)
        .firstOption
        .filter(u => key.isBcrypted(u.crypt))
    }

    def withGroups = for {
      u <- query
      ug <- UserGroups if u.id === ug.userId
      g <- Groups if ug.groupId === g.id
    } yield (u, g)

    def groups = withGroups.map(_._2).groupBy(x => x).map(_._1)

    def withPermissions = {
      val userperms = for {
        u <- query
        up <- UserPermissions if u.id === up.userId
        p <- up.permission
      } yield (u, p)

      val groupperms = for {
        (u, g) <- query.withGroups
        gp <- GroupPermissions.filter(_.groupId === g.id)
        p <- gp.permission
      } yield (u, p)

      (userperms ++ groupperms).groupBy(x => x).map(_._1)
    }

    def permissions = withPermissions.map(_._2)
  }

  implicit class GroupsExt(query: Query[GroupTable, Group]) {
    def withPermissions = for {
      g <- query
      gp <- GroupPermissions if g.id === gp.groupId
      p <- Permissions if p.id === gp.permissionId
    } yield (g, p)

    def permissions = withPermissions.map(_._2)
  }
}
