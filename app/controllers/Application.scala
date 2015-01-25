package controllers

import scala.concurrent.Future

import akka.actor.{Props, ActorRef, Actor}

import play.api._
import play.api.mvc._
import play.api.Play.current

import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


import reactivemongo.api._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection


import models._
import actors._

object Application extends Controller with MongoController {
	
	import models.Implicits._
	
	val (out, channel) = Concurrent.broadcast[JsValue]
	
  def index = Action { 
		val js = Json.toJson(Notification("room 5", "This is a test"))
		println(js.toString)
  	Ok(views.html.index())
  }
	
	def socket = WebSocket.acceptWithActor[ProtocolMsg, ProtocolMsg] { _ => upstream =>
		Props(new ClientConnection(upstream))
	}
	
	def jsRoutes = Action { implicit request =>
		import routes.javascript._
		Ok(Routes.javascriptRouter("routes")(
			routes.javascript.Users.register,
			routes.javascript.Users.login,
			routes.javascript.Users.logout,
			routes.javascript.Users.name
		))
	}
	
}


