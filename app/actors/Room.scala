package actors

import akka.actor._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


case class LoginNotification(upstream: ActorRef, name: String)
case class LogoutNotification(upstream: ActorRef, name: String)

class Room(name: String) extends Actor {
	
	var refs = List[ActorRef]()
	
	override def preStart = {
		println("Room created")
		println(this.self.path)
	}
	
	def receive = {
		
		/** Whenever the user logs in, a notification to all rooms is sent to check
		 *  if the user who just logged in is in in that room. A notification is sent to
		 *  all clients in the room if the user exists in the room.
		 */
		case LoginNotification(upstream, userName) =>
			if(refs.contains(upstream)) {
				val notif = Notification(name, s"User $userName has just joined.")
				refs
					.filterNot { _ == upstream }
					.foreach { _ ! notif }
			}
			
		/** Same as above, but instead is for tracking logouts */
		case LogoutNotification(upstream, userName) =>
			if(refs.contains(upstream)){
				val notif = Notification(name, s"User $userName has just left the room.")
				refs
					.filterNot { _ == upstream }
					.foreach { _ ! notif }
			}
		
		case Terminated(upstream) =>
			refs = refs.filter { _ != upstream }
			if(refs.isEmpty) context.stop(self)
			
		/** User sends a message directed at the users in a certain room. */
		case (upstream: ActorRef, ip: String, ChatReq(tokens, roomName, content)) =>
			User.nameOrAnon(tokens, ip).foreach { name =>
				val msg = UserMessage(name, roomName, content)
				refs.foreach { _ ! msg }
			}
			
		/** User requests to receive messages */
		case (upstream: ActorRef, ip: String, RoomReq(tokens, roomName)) =>
			User.nameOrAnon(tokens, ip).foreach { name =>
				println("name: " + name)
				val msg = Notification(roomName, s"User $name has just joined.")
				refs.foreach { _ ! msg }
				refs = upstream :: refs
				context.watch(upstream)
				upstream ! ProtocolOk("room", roomName)
			}
		
		/** Disconnect gracefully when possible */
		case(upstream: ActorRef, _: String, DisconnectReq(tokens, rooms)) =>
			refs = refs.filterNot { _ == upstream }
			if(refs.isEmpty) context.stop(self)
			
	}

}


object Room {
	def props(name: String) = Props(new Room(name))
}