package controllers

import play.api.Play
import play.api.mvc._

import play.api.libs.json.Json
import play.api.libs.json.JsNull

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import play.api.db.slick._
import slick.driver.JdbcProfile

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models._

class Login extends MyController with UserTable with HasDatabaseConfig[JdbcProfile] {
  import driver.api._

  case class User(email: String, password: String)

  val users = TableQuery[Users]

  val userForm: Form[User] = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  def get = Action.async { implicit request =>
    db.run(users.result).map { r =>
      Ok(Json.toJson(r))
    }
  }

  def post = Action { implicit request =>
    //form constraints
    userForm.bindFromRequest().fold(
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
  }
}
