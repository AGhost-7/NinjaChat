package models

import scala.concurrent.Future

import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.modules.reactivemongo._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.bson._


case class Token(_id: String, userId: String, ip: String)

object Token {
	
	/**
	 * JSON formatter
	 */
	implicit val jsonFormat = Json.format[Token]
	
	/**
	 * Returns the MongoDB collection object corresponding the the Token class
	 */
	def collection: JSONCollection = 
		ReactiveMongoPlugin.db.collection[JSONCollection]("tokens")
	
	/**
	 * Queries to find a Future[List[Token]] for the corresponding ip address.
	 */
	def forIp(ip: String) = 
		collection.find(Json.obj("ip" -> ip)).cursor[Token].collect[List]()
	
	/**
	 * Generates a new unique token for the user.
	 */
	def generate(user: User, ip: String): Future[Token] = {
		val _id = BSONObjectID.generate.stringify
		val token = Token(_id, user._id, ip)
		collection.insert(token).map { lastError =>
			token
		}
	}
	
}
