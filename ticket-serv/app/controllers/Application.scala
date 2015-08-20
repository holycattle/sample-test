package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json

import play.api.libs.json._
import play.api.libs.json.{JsNull, JsString, JsNumber, Json, JsArray}
import play.api.libs.functional.syntax._

import javax.inject.Inject

import play.api.data.validation._

import play.api.db.slick._
import slick.driver.JdbcProfile
import play.api.db.slick.HasDatabaseConfig

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import org.joda.time.DateTime

import models._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

abstract class MyController extends Controller {
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile]("vagrant")(Play.current)
}

class Application @Inject() (users: Users, events: Events) extends MyController with HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  case class UserEvent(from: String, offset: Option[Int], limit: Option[Int])

  val GroupStudent = 1
  val GroupCompany = 2

  val userEventForm: Form[UserEvent] = Form(
    mapping(
      //"from" -> jodaDate("yyyy-MM-dd"),
      "from" -> nonEmptyText,
      "offset" -> optional(number(min = 0)),
      "limit" -> optional(number(min = 1))
    )(UserEvent.apply)(UserEvent.unapply)
  )

  implicit object EventWrites extends Writes[Event] {
    def writes(u: Event) = 
      Json.obj(
        "id" -> JsNumber(u.id),
        "user_id" -> JsNumber(u.user_id),
        "name" -> JsString(u.name),
        "start_date" -> JsString(u.start_date.toString)
      )
  }

  implicit object SeqEventWrites extends Writes[Seq[Event]] {
    def writes(u: Seq[Event]) = {
      u.foldLeft(Json.arr()) {
        (acc, i) => {
          acc :+ Json.obj(
            "id" -> JsNumber(i.id),
            "user_id" -> JsNumber(i.user_id),
            "name" -> JsString(i.name),
            "start_date" -> JsString(i.start_date.toString)
          )
        }
      }
    }
  }

  implicit object SeqEventUserWrites extends Writes[Seq[(Event, User)]] {
    def writes(eu: Seq[(Event, User)]) = {
      val arr = eu.foldLeft(Json.arr()) {
        (acc, i) => {
          acc :+ Json.obj(
            //there might be a better way to do this?
            "id" -> JsNumber(i._1.id),
            "user_id" -> JsNumber(i._1.user_id),
            "name" -> JsString(i._1.name),
            "start_date" -> JsString(i._1.start_date.toString),
            "user" -> Json.obj(
              "id" -> JsNumber(i._2.id),
              "name" -> JsString(i._2.name)
            )
          )
        }
      }

      Json.obj( "code" -> JsNumber(200), "events" -> arr )
    }
  }


  def getStudentEvents = Action.async { implicit request =>
    userEventForm.bindFromRequest().fold(
      formWithErrors => Future {
        BadRequest(Json.obj( "code" -> play.mvc.Http.Status.INTERNAL_SERVER_ERROR ))
      },

      eventForm => {
        val deferredEvent = for {
          e <- events.getPresentAndFutureEvents(eventForm.from, eventForm.offset, eventForm.limit)
        } yield e
        
        deferredEvent.map(
          e => Ok(Json.toJson(e)).as("application/json")
        )
      }
    )
  }
}
