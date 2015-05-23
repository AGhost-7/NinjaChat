package actors

import akka.actor.{Props, ActorRef, Actor, ActorLogging}

import play.api.Play.current

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.concurrent.Akka

import org.mindrot.jbcrypt.BCrypt


import models._

class ClientConnection(upstream: ActorRef, ip: String)
		extends Actor
		with ActorLogging {

	var username = User.anon

	/** Receptionist actor is responsible for tracking all rooms in the app.
	 */ 
	def receptionist = 
		Akka.system.actorSelection("akka://application/user/receptionist")

	override def preStart = {
		println(s"println - Connection for ip $ip initialized.")
		log.info(s"Connection for ip $ip initialized.")
	}
	
	def receive = {
		
		/** Client has requested a registration for a new account. */
		case RegistrationReq(name, password) => 
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
									username = user.name
									upstream ! ProtocolOk("registration", token._id)
									receptionist ! LoginNotification(upstream, name)
								}
							case Some(_) =>
								val msg = "A user with your given account name already exists."
								upstream ! ProtocolError("registration", msg)
					}
			}
		
		/** Client has requested a login token. */
		case LoginReq(name, password) =>
			User.collection.find(Json.obj("name" -> name)).one[User].onSuccess {
				case Some(user) => 
					if(BCrypt.checkpw(password, user.password)){
						// TODO :: add notification
						Token.generate(user, ip).foreach { tkn =>
							username = user.name
							upstream ! ProtocolOk("login", tkn._id) 
							receptionist ! LoginNotification(upstream, name)
						}
					} else {
						upstream ! ProtocolError("login", "Password is incorrect.")
					}
				case None =>
					upstream ! ProtocolError("login", 
							"User name does not exist in database.")
			}
			
		/** Client has requested a token wipe for its account. */
		case LogoutReq(tokens) =>
			User.fromTokens(tokens, ip).onSuccess {
				case Some(user) =>
					Token.collection.remove(Json.obj("userId" -> user._id)).foreach { _ =>
						username = User.anon
						upstream ! ProtocolOk("logout", "Logout successful.")
						receptionist ! LogoutNotification(upstream, user.name)
					}
				case None =>
					val msg = "Are you sure you're logged in properly?"
					upstream ! ProtocolError("logout", msg)
			}
		
		/** Client wishes to determine what name it will display to the user. */
		case IdentityReq(tokens, _) =>
			User.fromTokens(tokens, ip).onSuccess {
				case Some(user) =>
					username = user.name
					upstream ! UserIdentity(user.name)
				case None =>
					username = User.anon
					val msg = "Looks like you don't exist!"
					upstream ! ProtocolError("identity", msg)
			}

		/** Any actor who sends a response to this actor back will then be forwarded
			* to the upstream actor.
			*/
		//case res: ProtoRes => upstream ! res

		/** Other requests are to be forwarded to the receptionist actor. */
		case req: ProtoReq =>
			receptionist ! WebsocketRequest(req, upstream, username)
			//receptionist ! (upstream, username, req)

	}

}

/**
 * Recommended practice seen here: 
 * http://doc.akka.io/docs/akka/2.3.9/scala/actors.html
 */
object ClientConnection {
	def props(upstream: ActorRef, ip: String) = 
		Props(new ClientConnection(upstream, ip))
}