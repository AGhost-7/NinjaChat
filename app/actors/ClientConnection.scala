package actors
import akka.actor.{Props, ActorRef, Actor}
import models._

class ClientConnection(upstream: ActorRef) extends Actor {
	def receive = {
		case RegistrationReq(name, password) => 
			println("requesting registration")
		case LoginReq(name, password) =>
			println("requesting login")
		case LogoutReq(tokens) =>
			println("requesting logout")
		case RoomReq(tokens, name: String) =>
			println("requesting access to room")
		case ChatReq(tokens, room: String, content: String) =>
			println(s"requesting to send chat message to room $room")
	}

}