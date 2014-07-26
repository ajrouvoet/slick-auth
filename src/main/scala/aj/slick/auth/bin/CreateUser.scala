package aj.slick.auth.bin

import aj.slick.auth.{Auth, User}
import com.github.t3hnar.bcrypt._
import io.strongtyped.active.slick.ActiveSlick

import scala.Console._

trait CreateUserComponent { this: ActiveSlick with App with Auth =>

  import jdbcDriver.simple._
  import userimplicits._

  def main()(implicit session: Session) = {
    val username = readPattern("username", "^[a-zA-Z0-9_-]{3,20}$")
    val email = readPattern("email", "^[^@]+@[^@]+\\.[^@]+$")
    val password = readPattern(
      "password",
      "(?=^.{8,}$)((?=.*\\d)|(?=.*\\W+))(?=.*[A-Z])(?=.*[a-z]).*$",
      () => System.console().readPassword().mkString
    )

    User(username, email, password.bcrypt).save()
  }

  def readPattern(field: String, regex: String, reader: (() => String) = readLine): String = {
    print(s"$WHITE$field: $RESET")
    Some(reader())
    .filter(_.matches (regex) )
    .getOrElse({
      println(s"${RED}Error$RESET: should match: $regex")
      readPattern(field, regex, reader)
    })
  }
}
