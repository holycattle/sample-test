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
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import java.sql.Timestamp

import play.api.libs.concurrent.Execution.Implicits.defaultContext 

import scala.slick.jdbc
import com.github.tototoshi.slick.MySQLJodaSupport._

import play.api.cache.Cache

import org.sedis.Pool

import play.api.libs.Codecs.sha1

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
class Users @Inject()
(@NamedDatabase("vagrant") protected val dbConfigProvider: DatabaseConfigProvider, sedisPool: Pool)
extends UserTable with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = dbConfigProvider.get[JdbcProfile]  
  import driver.api._

  val users = TableQuery[UserModel]

  def all(): Future[Seq[User]] = {
    db.run(users.result)
  }

  def getByEmailAndPassword(email: String, password: String): Future[Option[User]] = {    
    db.run(users.filter(_.email === email).filter(_.password === sha1(password)).result.headOption)
  }

  def authenticateSession(user: User): JsObject = {
    //TODO: figure out how to integrate the plugin with Play's Cache
    //import play.api.Play.current
    //Cache.set("key", "val")
    //println(Cache.getAs[String]("key"))

    //just for prototyping purposes, obviously LOL
    //expires after 25 minutes
    def generateToken(e: String): String = {
      val SECRET = "PCJ!)#B7" + System.currentTimeMillis.toString
      sha1(SECRET + e)
    }

    def jsonifyUser(token: String) = {
      //HACK -- there has to be a better way to do this ;_;
      val u = Json.toJson(user).transform((__ \ 'password).json.prune)
      Json.obj(
        "code" -> play.mvc.Http.Status.OK,
        "token" -> token,
        "user" -> u.get
      )
    }

    val key = user.email+"__password"
    sedisPool.withClient(client => {
      val currentPassword = client.get(key)
      currentPassword match {
        case None => {
          val token = generateToken(user.email)
          client.set(key, token)
          client.expire(key, 1500)
          jsonifyUser(token)
        }
        case Some(token) => {
          jsonifyUser(token)
        }
      }
    })
  }
}

/*
Event model
*/
//TODO: wrap in Option()
case class Event(id: Long, user_id: Long, name: String, start_date: DateTime)

trait EventTable {
  protected val driver: JdbcProfile
  import driver.api._
  
  class EventModel(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[Long]("id", O.PrimaryKey)
    def user_id = column[Long]("user_id")
    def name = column[String]("name")
    def start_date = column[DateTime]("start_date")
    
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
    val query = for {
      u <- users if u.group_id === g
      l <- events if l.user_id === u.id
    } yield l
    
    db.run(query.result).map(rows => rows.map { r => r })
  }

  def getPresentAndFutureEvents(dateString: String, offset: Option[Int], limit: Option[Int]): Future[Seq[(Event, User)]] = {
    val dateTime = DateTime.parse(dateString, DateTimeFormat.forPattern("yyyy-MM-dd"))

    //TODO: make a convenience function for resolving this kind of query
    val query = for {
      e <- events if e.start_date >= dateTime
      u <- users if u.id === e.user_id
    } yield (e, u)

    //check if there's an offset
    val o = offset match {
      case Some(x) => x
      case None => 0
    }

    //check if there's a limit
    //I guess I could nest matches, but that would be a pain to read lol
    limit match {
      case Some(x) => db.run(query.drop(o).take(x)
        .sortBy(_._1.start_date.asc).result).map(rows => rows)
      case None => db.run(query.drop(o).sortBy(_._1.start_date.asc).result).map(rows => rows)
    }
  }
}

