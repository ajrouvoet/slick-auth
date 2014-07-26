package aj.slick.auth.bin

import aj.slick.auth.{Auth, User}
import com.github.t3hnar.bcrypt._
import io.strongtyped.active.slick.ActiveSlick

import scala.Console._

class CreateUserComponent { this: ActiveSlick with App with Auth =>

  import jdbcDriver.simple._

  def main()(implicit session: Session) = {
    print("username: ")
    val username = readPattern("^[a-zA-Z0-9_-]{3,20}$")
    print("email: ")
    val email = readPattern("^[^@]+@[^@]+\\.[^@]+$")
    print("password: ")
    val password = readPattern(
      "(?=^.{8,}$)((?=.*\\d)|(?=.*\\W+))(?=.*[A-Z])(?=.*[a-z]).*$",
      () => System.console().readPassword().toString
    )

    User(username, email, password.bcrypt).save()
  }

  def readPattern(regex: String, reader: (() => String) = readLine): String =
    Some(reader())
      .filter(_.matches(regex))
      .getOrElse(readPattern(regex))
}
