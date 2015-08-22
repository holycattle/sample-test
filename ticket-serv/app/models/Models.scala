package models

import javax.inject.{Singleton, Inject}
import scala.concurrent.Future
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Success, Failure}

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
        "user" -> u.get //since we're sure it exists; usually bad practice though
      )
    }

    //simply refresh token if user tries to login again
    //TODO: turns this into a method that can be curried
    sedisPool.withClient(client => {
      val token = generateToken(user.email)
      client.set(token, user.email)
      client.set(user.email+"__group_id", user.group_id.toString)
      client.set(user.email+"__user_id", user.id.toString)
      client.expire(token, 1500)
      client.expire(user.email+"__group_id", 1500)
      client.expire(user.email+"__user_id", 1500)
      jsonifyUser(token)
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
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def user_id = column[Long]("user_id")
    def name = column[String]("name")
    def start_date = column[DateTime]("start_date")
    
    def * = (id, user_id, name, start_date) <> (Event.tupled, Event.unapply _)
  }
}

//TODO: use generics to reduce boilerplate and repetition T_T
@Singleton()
class Events @Inject() (@NamedDatabase("vagrant") protected val dbConfigProvider: DatabaseConfigProvider, sedisPool: Pool) extends EventTable with UserTable with AttendsTable
with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = dbConfigProvider.get[JdbcProfile]  
  import driver.api._

  val events = TableQuery[EventModel]
  val users = TableQuery[UserModel]
  val attends = TableQuery[AttendsModel]

  def all(): Future[Seq[User]] = {
    db.run(users.result)
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
    limit match {
      case Some(x) => db.run(query.drop(o).take(x)
        .sortBy(_._1.start_date.asc).result).map(rows => rows)
      case None => db.run(query.drop(o).sortBy(_._1.start_date.asc).result).map(rows => rows)
    }
  }

  def getCompanyEvents(email: String, dateString: String, offset: Option[Int],
  limit: Option[Int]): Future[Seq[(Long, String, String, Int)]] = {

    //TODO: turns this into a method that can be curried
    sedisPool.withClient(client => {
      //TODO: use matching; for testing purposes, it's probably not needed
      val id = client.get(email+"__user_id")

      //DIRTY HACK
      if (!limit.isEmpty && offset.isEmpty) {
        val noOffsetSQL =
          sql"""
            SELECT e.id, e.name, e.start_date, count(a.user_id)
            as number_of_attendees FROM events e
            left outer join attends a
            ON e.id = a.event_id
            WHERE e.start_date >= ${dateString} and
            e.user_id = ${id}
            GROUP BY e.id;""".as[(Long, String, String, Int)]

        db.run(noOffsetSQL)
      } else if (!limit.isEmpty && !offset.isEmpty) {
        val limitOffsetSQL =
          sql"""
          SELECT e.id, e.name, e.start_date, count(a.user_id)
          as number_of_attendees FROM events e
          left outer join attends a
          ON e.id = a.event_id
          WHERE e.start_date >= ${dateString} and
          e.user_id = ${id}
          GROUP BY e.id
          LIMIT ${limit.get} OFFSET ${offset.get};""".as[(Long, String, String, Int)]

        db.run(limitOffsetSQL)
      } else if (limit.isEmpty && !offset.isEmpty) {
          val defaultLimitSQL =
            sql"""
              SELECT e.id, e.name, e.start_date, count(a.user_id)
              as number_of_attendees FROM events e
              left outer join attends a
              ON e.id = a.event_id
              WHERE e.start_date >= ${dateString} and
              e.user_id = ${id}
              GROUP BY e.id
              LIMIT 100 OFFSET ${offset.get};""".as[(Long, String, String, Int)]

        db.run(defaultLimitSQL) //arbitrary limit
      } else {
        val defaultSQL =
          sql"""
            SELECT e.id, e.name, e.start_date, count(a.user_id)
            as number_of_attendees FROM events e
            left outer join attends a
            ON e.id = a.event_id
            WHERE e.start_date >= ${dateString} and
            e.user_id = ${id}
            GROUP BY e.id;""".as[(Long, String, String, Int)]
        db.run(defaultSQL)
      }
    })
  }
}

/*
Attends model
*/
case class Attendance(user_id: Int, event_id: Int, reserved_at: DateTime)

trait AttendsTable {
  protected val driver: JdbcProfile
  import driver.api._
  
  class AttendsModel(tag: Tag) extends Table[Attendance](tag, "attends") {
    def user_id = column[Int]("user_id")
    def event_id = column[Int]("event_id")
    def reserved_at = column[DateTime]("reserved_at", O.Default(DateTime.now))
    
    def * = (user_id, event_id, reserved_at) <> (Attendance.tupled, Attendance.unapply _)
  }
}

//TODO: refactor all the extends to a super class for all my Tables
@Singleton()
class Attends @Inject()
(@NamedDatabase("vagrant") protected val dbConfigProvider: DatabaseConfigProvider, sedisPool: Pool)
extends EventTable with UserTable with AttendsTable with HasDatabaseConfig[JdbcProfile] {

  val dbConfig = dbConfigProvider.get[JdbcProfile]  
  import driver.api._

  val events = TableQuery[EventModel]
  val users = TableQuery[UserModel]
  val attends = TableQuery[AttendsModel]

  def reserve(email: String, eventId: Long, reserved: Boolean): Future[Int] = {
    //get user based on email and group_id
    
    val repLong: Rep[Long] = eventId
    val repInt: Rep[Int] = eventId.toInt
    
    //get user role
    val q = for {
      //TODO: get rid of magic numbers! evil, evil magic numbers
      u <- users if u.email === email && u.group_id === 1
    } yield u
    val res: Future[Option[User]] = db.run(q.result.headOption)

    val queryExistingEvent = for {
      u <- users if u.email === email
      e <- events if e.id === repLong && e.user_id === u.id
    } yield e

    res.map { case u =>
      u match {
        case Some(y) => {
          //TODO: REFACTOR THIS ABOMINATION LATER; it's as blocky (i.e. not async) as shit
          val exists = Await.result(db.run(attends.filter(_.user_id===y.id.toInt)
            .filter(_.event_id===eventId.toInt).exists.result), 1 seconds)
          println(exists)
          if (reserved && !exists) {
            Await.result(db.run(attends += Attendance(y.id.toInt, eventId.toInt, DateTime.now)), 1 seconds) //successfully inserted; returns non negative number
          } else if (!reserved) {
            //TODO: this is a terrible hack; will fix later
            val x = Await.result(db.run(attends.filter(_.event_id === repInt).delete), 1 seconds)
            if (x < 1) -1 else 1 //returns -1 if nothing was deleted
          } else {
            -2 //exists already; don't insert
          }
        }
        case None => 0
      }
    }
  }
}
