package aj.slick.auth

import com.github.tototoshi.slick.MySQLJodaSupport._
import io.strongtyped.active.slick.ActiveSlick
import io.strongtyped.active.slick.models.Identifiable
import org.joda.time._

case class User(
  id: Option[Int],
  username: String,
  email: String,
  password: String,
  salt: String,
  created_at: DateTime = new DateTime()
) extends Identifiable[User] {
  override type Id = Int
  override def withId(id: Id): User = copy(id = Some(id))
}

trait Auth { this: ActiveSlick =>
  import jdbcDriver.simple._

  class UserTable(tag: Tag) extends IdTable[User, Int](tag, "users") {
    // fields
    def id = column[Int]("uid", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def email = column[String]("email")
    def password = column[String]("password")
    def salt = column[String]("salt")
    def created_at = column[DateTime]("created_at")

    // constraints
    def unique_email = index("unique_email", email, unique=true)

    // mappings
    override def * = (id.?, username, email, password, salt, created_at) <> (User.tupled, User.unapply)
  }

  val Users = TableQuery[UserTable]

  implicit class UserQueryExt(query: TableQuery[UserTable]) extends IdTableExt[User](query)

  implicit object UserModel extends Model(Users)
}

