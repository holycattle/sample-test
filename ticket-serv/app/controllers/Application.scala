package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsNull

import play.api.db.slick._
import slick.driver.JdbcProfile
import play.api.db.slick.HasDatabaseConfig

abstract class MyController extends Controller {
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile]("vagrant")(Play.current)
}

class Application extends Controller {

  //sample json
  /*
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
  Ok(x).as("application/json")*/


  def index = Action {
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
}
