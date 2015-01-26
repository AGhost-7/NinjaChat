package actors

import akka.actor.{Props, ActorRef, Actor}

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import org.mindrot.jbcrypt.BCrypt

import models._

class ClientConnection(upstream: ActorRef, ip: String) extends Actor {
	def receive = {
		
		/**
		 * Client has requested a registration for a new account.
		 */
		case RegistrationReq(name, password) => 
			println("requesting registration")
			User.findError(name, password) match {
				case Some(error) => 
					upstream ! ProtocolError("registration", error)
				case None =>
					User.collection.find(Json.obj("name" -> name)).one[User].onSuccess { 
							case None =>
								for {
									user <- User.insert(name, password)
									token <- Token.generate(user, ip)
								} yield {
									// TODO: add the socket broadcast for notification of user login.
									upstream ! ProtocolOk("registration", token._id)
								}
							case Some(_) =>
								val msg = "A user with your given account name already exists."
								upstream ! ProtocolError("registration", msg)
					}
			}
		
		/**
		 * Client has requested a login token.
		 */
		case LoginReq(name, password) =>
			println("requesting login")
			User.collection.find(Json.obj("name" -> name)).one[User].onSuccess {
				case Some(user) => 
					if(BCrypt.checkpw(password, user.password)){
						Token.generate(user, ip).foreach { tkn =>
							upstream ! ProtocolOk("login", tkn._id) 
						}
					} else {
						upstream ! ProtocolError("login", "Password is incorrect.")
					}
				case None =>
					upstream ! ProtocolError("login", 
							"User name does not exist in database.")
			}
			
		/**
		 * Client has requested a token wipe for its account.
		 */
		case LogoutReq(tokens) =>
			println("requesting logout")
			User.fromTokens(tokens, ip).onSuccess {
				case Some(user) =>
					Token.collection.remove(Json.obj("userId" -> user._id)).foreach { _ =>
						upstream ! ProtocolOk("logout", "Logout successful.")
					}
				case None =>
					val msg = "Are you sure you're logged in properly?"
					upstream ! ProtocolError("logout", msg)
			}
		
		/**
		 * Client wishes to determine what name it will display to the user.
		 */
		case IdentityReq(tokens, _) =>
			User.fromTokens(tokens, ip).onSuccess {
				case Some(user) =>
					upstream ! UserIdentity(user.name)
				case None =>
					val msg = "Looks like you don't exist!"
					upstream ! ProtocolError("identity", msg)
			}
			
		case RoomReq(tokens, name: String) =>
			println("requesting access to room")
		case ChatReq(tokens, room: String, content: String) =>
			println(s"requesting to send chat message to room $room")
	}

}