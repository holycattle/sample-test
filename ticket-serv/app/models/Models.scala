package models

import javax.inject.{Singleton, Inject}
import scala.concurrent.Future

import play.api._
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue

import play.db.NamedDatabase
import play.api.db.slick._
import slick.driver.JdbcProfile

case class User(id: Long, name: String, password: String, email: String, group_id: Int)

object User {
  implicit val userFormat = Json.format[User]
}

trait UserTable {
  protected val driver: JdbcProfile
  import driver.api._
  
  class UserModel(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey)
    def name = column[String]("name", O.PrimaryKey)
    def password = column[String]("password")
    def email = column[String]("email")
    def group_id = column[Int]("group_id")

    def * = (id, name, password, email, group_id) <> ((User.apply _).tupled, User.unapply _)
  }
}

@Singleton()
class Users @Inject() (@NamedDatabase("vagrant") protected val dbConfigProvider: DatabaseConfigProvider) extends UserTable with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = dbConfigProvider.get[JdbcProfile]  
  import driver.api._

  val users = TableQuery[UserModel]

  def all(): Future[Seq[User]] = {
    db.run(users.result)
  }

  def getByEmailAndPassword(email: String, password: String): Future[Option[User]] = {
    /*val query = for {
      u <- users if u.email === email && u.password === password
    } yield u*/
    db.run(users.filter(_.email === email).filter(_.password === password).result.headOption)
    //db.run(query.result)
  }

  def authenticateSession(user: User): JsObject = {
    //HACK -- there has to be a better way to do this ;_;
    val u = Json.toJson(user).transform((__ \ 'password).json.prune)
    println(Json.obj(
      "code" -> play.mvc.Http.Status.OK,
      "token" -> "code",
      "user" -> u.get
      )
    )

    Json.obj(
      "code" -> play.mvc.Http.Status.OK,
      "token" -> "code",
      "user" -> u.get
    )
  }

}
