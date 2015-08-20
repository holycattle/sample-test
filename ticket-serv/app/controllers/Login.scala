package controllers

import javax.inject.Inject

import play.api.Play
import play.api.mvc._

import play.api.libs.json.Json
import play.api.libs.json.JsNull

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import play.api.db.slick._
import slick.driver.JdbcProfile

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models._

import org.sedis.Pool

class Login @Inject() (users: Users, sedisPool: Pool) extends MyController with HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  case class UserAuth(email: String, password: String)
  val userLoginForm: Form[UserAuth] = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(UserAuth.apply)(UserAuth.unapply)
  )

  def get = Action.async { implicit request =>
    users.all().map { r =>
      Ok(Json.toJson(r))
    }
  }

  //uses Redis; refer to Models.scala
  def login = Action.async { implicit request =>
    userLoginForm.bindFromRequest().fold(
      //had to make formWithErrors async beause this Action is async
      formWithErrors => Future {
        //InternalServerError(Json.obj( "code" -> play.mvc.Http.Status.INTERNAL_SERVER_ERROR ))
        Ok(Json.obj( "code" -> play.mvc.Http.Status.INTERNAL_SERVER_ERROR ))
      },

      user => {
        val deferredUser = for {
          u <- users.getByEmailAndPassword(user.email, user.password)
        } yield u

        deferredUser.map { case u =>
          u match {
            case Some(x) => {
              Ok(Json.toJson(users.authenticateSession(x))).as("application/json")
            }
            case None =>
              //InternalServerError(Json.obj( "code" -> play.mvc.Http.Status.INTERNAL_SERVER_ERROR ))
              Ok(Json.obj( "code" -> play.mvc.Http.Status.INTERNAL_SERVER_ERROR )).as("application/json")
          }
        }
      }
    )
  }
}
