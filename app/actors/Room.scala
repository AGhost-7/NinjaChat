package actors

import akka.actor._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


//case class LoginNotification(upstream: ActorRef, name: String)
//case class LogoutNotification(upstream: ActorRef, name: String)

class Room(roomName: String)
		extends Actor
		with ActorLogging
		with WebsocketProcessing {

	import Room._

	// All users currently in the room
	//var refs = List[ActorRef]()
	// all client connections in the room
	var cons = List[ActorRef]()

	override def preStart = {
		log.info(s"Room $roomName created at path ${self.path}")
		println("Room path: " + self.path)
	}

	val websocketReceive = new WebsocketProcessor {
		val process: PartialFunction[ProtoReq, Unit] = {

			/** User sends a message directed at the users in a certain room. */
//			case ChatReq(roomName, content) =>
//				val msg = UserMessage(connection.path.name, name, roomName, content)
//				refs.foreach { _ ! msg }

			/** User requests to receive messages */
			case RoomReq(roomName) =>
				//val msg = Notification(roomName, s"User $name has just joined.")
				val msg = RoomUserAdd(roomName, connection)
				// notify all users that someone has logged in.
				cons.foreach { _.tell(msg, connection) }
				// the client connection needs a listing of all of the connected clients
				// to start communicating.
				connection ! RoomUsersList(roomName, cons)
				cons = connection :: cons
				context.watch(connection)

				//refs = upstream :: refs
				//context.watch(upstream)
				//upstream ! ProtocolOk("room", roomName)

			/** Disconnect gracefully when possible. the disconnect needs to be sent
				* to the room actor since it needs to track all of the active
				* connections inside of the room for new connections to gaint the
				* correct references.
				*/

			case DisconnectReq(tokens, rooms) =>
				cons = cons.filterNot { _ == connection }
				context.unwatch(connection)
				if(cons.isEmpty) context.stop(self)

			/** Broadcast image to all in room. */
//			case ImageReq(id, part, room, data) =>
//				val msg = ImageSubmission(name, id, part, room, data)
//				refs.foreach { _ ! msg }

			/** Send data to initialize client buffer. */
//			case ImageReqInit(id, room, parts) =>
//				val msg = ImageSubmissionInit(name, id, room, parts)
//				refs.foreach { _ ! msg }
		}
	}

	val _receive: Receive = {

		/** Whenever the user logs in, a notification to all rooms is sent to check
			* if the user who just logged in is in in that room. A notification is
			* sent to all clients in the room if the user exists in the room.
			*/
//		case LoginNotification(upstream, userName) =>
//			if(refs.contains(upstream)) {
//				val notif = Notification(roomName, s"User $userName has just joined.")
//				refs
//					.filterNot { _ == upstream }
//					.foreach { _ ! notif }
//			}
//
//		/** Same as above, but instead is for tracking logouts */
//		case LogoutNotification(upstream, userName) =>
//			if(refs.contains(upstream)){
//				val notif = Notification(roomName, s"User $userName has just left the room.")
//				refs
//					.filterNot { _ == upstream }
//					.foreach { _ ! notif }
//			}
		/** Tracking death of the client connections will permit the other
			* connections to be able to receive notification to remove it from their
			* recipients list.
			*/
		case Terminated(connection) =>
			cons = cons.filter { _ != connection }
			val msg = RoomUserRemove(roomName, connection)
			cons.foreach { _ ! msg }
			if(cons.isEmpty) context.stop(self)

	}

	val receive = websocketReceive.orElse(_receive)

}


object Room {
	def props(name: String) = Props(new Room(name))
	case class RoomUserAdd(roomName: String, con: ActorRef)
	case class RoomUserRemove(roomName: String, con: ActorRef)
	case class RoomUsersList(roomName: String, users: List[ActorRef])
}