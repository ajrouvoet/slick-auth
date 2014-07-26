package aj.slick.auth

import com.github.tototoshi.slick.MySQLJodaSupport._
import io.strongtyped.active.slick.ActiveSlick
import io.strongtyped.active.slick.models.Identifiable
import org.joda.time._

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

trait AuthBackend

trait Auth { this: ActiveSlick =>

  val authBackend: AuthBackend

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

  // table query
  val Users = TableQuery[UserTable]

  // active-slick table extensions
  implicit class UserQueryExt(query: TableQuery[UserTable]) extends IdTableExt[User](query)

  // model extensions
  implicit class UserModel(user: User) extends
    RichModel[User, UserQueryExt](user, new UserQueryExt(Users))
}

