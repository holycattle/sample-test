package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsNull

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
