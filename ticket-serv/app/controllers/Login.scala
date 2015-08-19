package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json.Json
import play.api.libs.json.JsNull

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

class Login extends Controller {
  case class User(email: String, password: String)

  val userForm: Form[User] = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  def get = Action {
    var x = Json.obj(
      "users" -> Json.arr(
        Json.obj(
          "name" -> "bob",
          "age" -> 31,
          "email" -> "bob@gmail.com"  	  
        ),
        Json.obj(
          "name" -> "kiki",
          "age" -> 25,
          "email" -> JsNull  	  
        )
      )
    )
    Ok(x).as("application/json")
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
