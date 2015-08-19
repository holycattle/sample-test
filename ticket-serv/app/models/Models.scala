package models

import play.api._
import play.api.libs.json.Json
import slick.driver.JdbcProfile

case class User(id: Long, name: String, password: String, email: String, groupId: Int)

object User {
  implicit val userFormat = Json.format[User]
}

trait UserTable {
  protected val driver: JdbcProfile
  import driver.api._
  
  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey)
    def name = column[String]("name", O.PrimaryKey)
    def password = column[String]("password")
    def email = column[String]("email")
    def groupId = column[Int]("group_id")

    def * = (id, name, password, email, groupId) <> ((User.apply _).tupled, User.unapply _)
  }
}
