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

import org.joda.time.DateTime
import java.sql.Timestamp

/*
User model
*/
case class User(id: Long, name: String, password: String, email: String, group_id: Int)

object User {
  implicit val userFormat = Json.format[User]
}

trait UserTable {
  protected val driver: JdbcProfile
  import driver.api._
  
  class UserModel(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def password = column[String]("password")
    def email = column[String]("email")
    def group_id = column[Int]("group_id")

    def * = (id, name, password, email, group_id) <> ((User.apply _).tupled, User.unapply _)
  }
}

@Singleton()
//oops there's a magic string here;
//TODO: refactor 'vagrant' to a config thing
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
    Json.obj(
      "code" -> play.mvc.Http.Status.OK,
      "token" -> "code",
      "user" -> u.get
    )
  }
}

/*
Event model
*/
case class Event(id: Long, user_id: Int, name: String, start_date: Timestamp)

trait EventTable {
  protected val driver: JdbcProfile
  import driver.api._
  
  class EventModel(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[Long]("id", O.PrimaryKey)
    def user_id = column[Int]("user_id")
    def name = column[String]("name")
    def start_date = column[Timestamp]("start_date")

    /*implicit object EventWrites extends Writes[Event] {
      def writes(u: Event) = Json.obj(
        "id" -> JsNumber(u.id),
        "user_id" -> JsNumber(u.user_id),
        "name" -> JsString(u.name),
        "start_date" -> JsString(u.start_date.toString)
      )
    }*/

    def * = (id, user_id, name, start_date) <> (Event.tupled, Event.unapply _)
  }
}

//TODO: use generics to reduce boilerplate and repetition T_T
@Singleton()
class Events @Inject() (@NamedDatabase("vagrant") protected val dbConfigProvider: DatabaseConfigProvider) extends EventTable with UserTable with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = dbConfigProvider.get[JdbcProfile]  
  import driver.api._

  val events = TableQuery[EventModel]
  val users = TableQuery[UserModel]

  def all(): Future[Seq[User]] = {
    db.run(users.result)
  }

  def getByGroup(g: Int): Future[Seq[Event]] = {
    /*val query = for {
      u <- users if u.group_id === g,
      l <- events if l.user_id === u.id
    } yield u*/
    
    db.run(events.result)
  }
}

