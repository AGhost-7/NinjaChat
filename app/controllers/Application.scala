package controllers

import scala.concurrent.Future

import akka.actor.{Props, ActorRef, Actor}

import play.api._
import play.api.mvc._
import play.api.Play.current

import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka

import reactivemongo.api._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

import models._
import actors._

object Application extends Controller with MongoController {
	
	import models.Implicits._
	
	Akka.system.actorOf(Props(new Receptionist()), name = "receptionist")
	
  def index = Action { 
  	Ok(views.html.index())
  }
	
	def socket = WebSocket.acceptWithActor[ProtocolMsg, ProtocolMsg] { req => upstream =>
		ClientConnection.props(upstream, req.remoteAddress)
	}
	
}


