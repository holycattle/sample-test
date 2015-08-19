package controllers

import javax.inject.Inject

import play.api.Play
import play.api.mvc._
import play.api.libs.Codecs.sha1

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

class Login @Inject() (users: Users) extends MyController with UserTable with HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  case class UserAuth(email: String, password: String)

  /*
  val userSignupForm: Form[User] = Form(
    mapping(
      "name" -> nonEmptyText,
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )*/

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

  def login = Action.async { implicit request =>
    userLoginForm.bindFromRequest().fold(
      formWithErrors => Future {
        InternalServerError(
          Json.obj("response" -> "Invalid login.")
        )
      },

      user => {
        val deferredUser = for {
          u <- users.getByEmailAndPassword(user.email, sha1(user.password))
        } yield u

        deferredUser.map { case u =>
          u match {
            case Some(x) => {
              Ok(Json.toJson(users.authenticateSession(x))).as("application/json")
            }
            case None =>
              Status(500)
          }
        }
      }
    )
  }

  /*
  def signup = Action { implicit request =>
    //form constraints
    userSignupForm.bindFromRequest().fold(
      formWithErrors => Status(500)(
        Json.obj("response" -> "Registration failed.")
      ).as("application/json"),
      
      user => Ok(
        Json.obj(
          "code" -> 0,
          "token" -> "hello_world",
          "user" -> Json.obj(
            "id" -> 0,
            "name" -> user.email,
            "group_id" -> 1
          )
        )  
      ).as("application/json")
    )
  }*/
}
