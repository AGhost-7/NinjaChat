package actors

import akka.actor._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


case class LoginNotification(upstream: ActorRef, name: String)
case class LogoutNotification(upstream: ActorRef, name: String)

class Room(roomName: String) extends Actor with ActorLogging {

	// All users currently in the room
	var refs = List[ActorRef]()
	
	override def preStart = {
		log.info(s"Room $roomName created at path ${self.path}")
	}
	
	def receive = {
		
		/** Whenever the user logs in, a notification to all rooms is sent to check
		 *  if the user who just logged in is in in that room. A notification is sent to
		 *  all clients in the room if the user exists in the room.
		 */
		case LoginNotification(upstream, userName) =>
			if(refs.contains(upstream)) {
				val notif = Notification(roomName, s"User $userName has just joined.")
				refs
					.filterNot { _ == upstream }
					.foreach { _ ! notif }
			}
			
		/** Same as above, but instead is for tracking logouts */
		case LogoutNotification(upstream, userName) =>
			if(refs.contains(upstream)){
				val notif = Notification(roomName, s"User $userName has just left the room.")
				refs
					.filterNot { _ == upstream }
					.foreach { _ ! notif }
			}
		
		case Terminated(upstream) =>
			refs = refs.filter { _ != upstream }
			if(refs.isEmpty) context.stop(self)
			
		/** User sends a message directed at the users in a certain room. */
		case (upstream: ActorRef, username: String, ChatReq(roomName, content)) =>
			val msg = UserMessage(username, roomName, content)
			refs.foreach { _ ! msg }
			
		/** User requests to receive messages */
		case (upstream: ActorRef, username: String, RoomReq(roomName)) =>
			val msg = Notification(roomName, s"User $username has just joined.")
			refs.foreach { _ ! msg }
			refs = upstream :: refs
			context.watch(upstream)
			upstream ! ProtocolOk("room", roomName)
		
		/** Disconnect gracefully when possible */
		case(upstream: ActorRef, _: String, DisconnectReq(tokens, rooms)) =>
			refs = refs.filterNot { _ == upstream }
			if(refs.isEmpty) context.stop(self)
		
		/** Broadcast image to all in room. */
		case (upstream: ActorRef, username: String, ImageReq(id, part, room, data)) =>
			val msg = ImageSubmission(username, id, part, room, data)
			refs.foreach { _ ! msg }

		/** Send data to initialize client buffer. */
		case (upstream: ActorRef, username: String, ImageReqInit(id, room, parts)) =>
			val msg = ImageSubmissionInit(username, id, room, parts)
			refs.foreach { _ ! msg }

		case huh =>
			log.error("Un-tracked message received " + huh.toString())
	}

}


object Room {
	def props(name: String) = Props(new Room(name))
}