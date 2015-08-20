package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsNull

import play.api.data.validation._

import play.api.db.slick._
import slick.driver.JdbcProfile
import play.api.db.slick.HasDatabaseConfig

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import org.joda.time.DateTime

abstract class MyController extends Controller {
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile]("vagrant")(Play.current)
}

class Application extends MyController  {

  case class UserEvent(from: DateTime, offset: Option[Int], limit: Option[Int])

  val userEventForm: Form[UserEvent] = Form(
    mapping(
      "from" -> jodaDate("yyyy-MM-dd"),
      "offset" -> optional(number(min = 0)),
      "limit" -> optional(number(min = 1))
    )(UserEvent.apply)(UserEvent.unapply)
  )

  /*def getUserEvents = Action.async {
    
  }*/
}
