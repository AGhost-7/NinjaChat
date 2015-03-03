package models

import scala.concurrent.Future
import play.api.Play.current

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._



import play.modules.reactivemongo._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.bson._

import org.mindrot.jbcrypt.BCrypt

case class User(_id: String, name: String, password: String)

object User {
	
	/**
	 * This is the name used if the user isn't logged in.
	 */
	val anon = "*Anonymous*"
		
	/**
	 * JSON formatter
	 */
	implicit val jsonFormat = Json.format[User]

	/**
	 * Returns an option of string, where the Some instance contains the 
	 * validation message describing what wasn't properly entered.
	 */
	def findError(name: String, password: String): Option[String] = {
		if(name.length < 4) 
			Some("Your name is too short. It is required that your name be at " +
					"least four characters of long.")
		else if(password.length < 5) 
			Some("Your password is too short. Passwords must be at least five " +
					"characters of long.")
		else if(!name.matches("""[A-z0-9,_`'"$%#@!^&*()-]+""")) 
			Some("Your name is using illegal characters.")
		else if(!password.matches("""[A-z0-9,_`'"$%#@!^&*()-]+"""))
			Some("You password is using illegal characters.")
		else if(name == anon)
			Some(s"You cannot use the reserved name $anon.")
		else
			None
	}
	
	/**
	 * Returns the MongoDB collection object corresponding the the User class
	 */
	def collection = ReactiveMongoPlugin.db.collection[JSONCollection]("users")
	
	/**
	 * Just need to feed this thing the token strings and it'll spew out the 
	 * corresponding User instance if there is one. Checks for ip, and the whole
	 * process is done in a non-blocking IO manner. Strings should be the token
	 * strings given by the clients.
	 */
	def fromTokens(strings: List[String], ip: String): Future[Option[User]] = {
		if(strings.isEmpty) {
			Future.successful(None)
		} else {
			val ftUserIds = Token.forIp(ip).map { tokens: List[Token] =>
				// now I need all of the tokens which the requester has which match the
				// ip in the database
				tokens.filter { token =>
					strings.contains(token._id)
				}.map { _.userId }
			}
			
			// Now I just need to turn this from a Future[List[userId: Int]] to a 
			// Future[Option[User]]
			val ftUsers: Future[List[Option[User]]] = ftUserIds.flatMap { ids: List[String] => 
				Future.traverse(ids) { id =>
					collection.find(Json.obj("_id" -> id)).one[User]
				}
			}
			
			val ftUser:Future[Option[User]] = ftUsers.map { users: List[Option[User]] =>
				val noOption = users.flatten
				if(noOption.isEmpty)
					None
				else
					Some(noOption(0))
			}
			
			ftUser
		}
	}
	
	/**
	 * Returns the name of the user or fallbacks to the current designated 
	 * "anonymous" default name.
	 */
	def nameOrAnon(strings: List[String], ip: String): Future[String] = {
		fromTokens(strings, ip)
			.map { optUser =>
				optUser.map{ _.name }.getOrElse(anon)
			}
	}
	
	/**
	 * Takes the sanitized name and password and hashes the password then inserts
	 * the entry into the mongoDB database.
	 */
	def insert(name: String, password: String): Future[User] = {
		val salt = BCrypt.gensalt
		val hashed = BCrypt.hashpw(password, salt)
		val id = BSONObjectID.generate.stringify
		val user = User(id, name, hashed)
		User.collection.insert(user).map { lastError =>
			user
		}
	}
	
}