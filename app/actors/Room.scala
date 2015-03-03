package actors

import akka.actor._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class Room() extends Actor {
	
	var refs = List[ActorRef]()
	
	override def preStart = {
		println("Room created")
		println(this.self.path)
	}
	
	def receive = {
			
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
		case(upstream: ActorRef, _: String, DisconnectReq(_)) =>
			refs = refs.filterNot { _ == upstream }
			if(refs.isEmpty) context.stop(self)
			
	}

}


object Room {
	def props() = Props(new Room())
}