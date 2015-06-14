package actors

import akka.actor.Actor.Receive

import collection.mutable.{Map => MMap, ListBuffer => MList}

import akka.actor.{Props, ActorRef, Actor, ActorLogging}

import play.api.Play.current

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.concurrent.Akka

import org.mindrot.jbcrypt.BCrypt


import models._

trait ComposableActor { self: Actor =>
	val receive: Receive = Map.empty[Any, Unit]
	def compose(part: Receive) = receive.orElse(part)
}

trait IdentifiedConnection { t: Actor =>
	import java.security.MessageDigest

	var username: String
	type RoomName = String
	val rooms: MMap[RoomName, MList[ActorRef]]
	val upstream: ActorRef
	val ip: String


	val clientId: String = {
		val digest = MessageDigest.getInstance("SHA-256")
		digest.update(t.ip.getBytes("UTF-8"))
		val hash = digest.digest().map { _.toChar }.mkString("")
		println(hash)
		hash
	}
}

trait WebsocketConnection { self: ComposableActor with IdentifiedConnection =>
	// initial connection
	// then you have the room request which will
	override val receive = compose {
		case _ =>
	}
}

trait RoomPlugged { self: ComposableActor with IdentifiedConnection =>
	import Room._
	// - user sends a chat message
	// - Client disconnects/leaves room
	// - user enters a chat room

	type UserName = String
	type RoomName = String
	val rooms: MMap[RoomName, MList[ActorRef]]

	override val receive = compose {
		/** The room actor sends a list of current connections */
		case RoomUsersList(roomName, cons) =>
			// just got a room add message
			rooms.+=(roomName -> MList(cons: _*))
		case RoomUserAdd(roomName, con) =>
			rooms(roomName).+=(con)
			upstream ! Notification(roomName, "A user enters the room.")
		case RoomUserRemove(roomName, con) =>
			rooms(roomName).-=(con)
			upstream ! Notification(roomName, "A user leaves the room.")

		case _ =>
	}
}
trait MessagingConnection { self: ComposableActor with IdentifiedConnection =>
	override val receive = compose {

		case _ =>
	}
}
/** This trait contains all session logic, which is largely just for naming the
	* user. Public keys are given on a connection basis, so this isn't tied to it.
	*/
trait SessionConnectable { self: ComposableActor with IdentifiedConnection =>

	override val receive = compose {

		/** Client wishes to determine what name it will display to the user. The
			* client uses this to validate its tokens on the initial startup.
			*/
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

		/** Client has requested a registration for a new account. All users in the
			* rooms which the
			*/
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
								rooms.foreach { case(roomName, users) =>
									val msg = Notification(roomName, s"User ${user.name} has just joined.")
									users.foreach { _ ! msg }
								}

								//receptionist ! LoginNotification(upstream, name)
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
					if(BCrypt.checkpw(password, user.password)) {
						Token.generate(user, ip).foreach { tkn =>
							username = user.name
							upstream ! ProtocolOk("login", tkn._id)
							rooms.foreach { case(roomName, users) =>
								val msg = Notification(roomName,
										s"User $username has just logged in.")
								users.foreach { _ ! msg }
							}
							//receptionist ! LoginNotification(upstream, name)
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
						upstream ! ProtocolOk("logout", "Logout successful.")
						rooms.foreach { case (roomName, users) =>
							val msg = Notification(roomName, s"User $username has just left the room.")
							users.foreach { _ ! msg }
						}
						//receptionist ! LogoutNotification(upstream, user.name)
					}
					username = User.anon
				case None =>
					val msg = "Are you sure you're logged in properly?"
					upstream ! ProtocolError("logout", msg)
			}

	}


}


class ClientConnection(val upstream: ActorRef, val ip: String)
		extends Actor
		with ActorLogging
		with ComposableActor
		with IdentifiedConnection
		with MessagingConnection {

	var username = User.anon
	val rooms = MMap.empty[String, MList[ActorRef]]

	// Maintain a list of the connections for each user in the rooms.
//	type UserName = String
//	type RoomName = String
//	var rooms = MMap.empty[RoomName, MMap[UserName, ActorRef]]

	//def allCons : Seq[ActorRef] = rooms.flatten.toSeq.distinct

	/** Receptionist actor is responsible for tracking all rooms in the app. */
	def receptionist = 
		Akka.system.actorSelection("akka://application/user/receptionist")

	override def preStart = {
		println(s"println - Connection for ip $ip initialized.")
		log.info(s"Connection for ip $ip initialized.")
	}
	
	override val receive = compose {

		case ChatReq(roomName, content) =>
			rooms.get(roomName).fold {
				upstream ! ProtocolError("chat-message", "")
			} { users =>

			}

		/** Other requests are to be forwarded to the receptionist actor. */
		case req: ProtoReq =>
			receptionist ! WebsocketRequest(req, self, upstream, username)
			//receptionist ! (upstream, username, req)

		case res: ProtoRes =>
			upstream ! res

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