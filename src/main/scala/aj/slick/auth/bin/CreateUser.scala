package aj.slick.auth.bin

import aj.slick.Profile
import aj.slick.auth.User
import aj.scalatra.auth.AuthComponent
import com.github.t3hnar.bcrypt._

import scala.Console._

trait CreateUserComponent { this: Profile with App with AuthComponent =>

  import profile.simple._

  def main()(implicit session: Session) = {
    val username = readPattern("username", "^[a-zA-Z0-9_-]{3,20}$")
    val email = readPattern("email", "^[^@]+@[^@]+\\.[^@]+$")
    val password = readPattern(
      "password",
      "(?=^.{8,}$)((?=.*\\d)|(?=.*\\W+))(?=.*[A-Z])(?=.*[a-z]).*$",
      () => System.console().readPassword().mkString
    )

    val user = User(None, username, email, password.bcrypt)
    val uid = user.copy(id=Some(Users.insertReturnId(user)))
    println("Created new user ${WHITE}$uid${RESET}")
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
