package actors

import akka.actor.{Props, ActorRef, Actor, Terminated, Kill}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka

import models._


import scala.collection.mutable.{Map => MMap}



class Receptionist() extends Actor {
		
	val rooms = MMap[String, ActorRef]()
	
	override def preStart = {
		println(this.self.path)
	}
	
	def receive = {
		
		/** Nofity all rooms that user has logged out. */
		case notif: LogoutNotification =>
			rooms.values.foreach { _ ! notif }
			
		/** Notify all rooms that a user has just logged in. */
		case notif: LoginNotification =>
			rooms.values.foreach { _ ! notif }
			
		/** User requests to receive messages from a certain room. */
		case msg @ (upstream: ActorRef, ip: String, RoomReq(tokens, roomName)) =>
			rooms.get(roomName).getOrElse {
				val room: ActorRef = Akka.system.actorOf(Room.props(roomName))
				rooms += roomName -> room
				context.watch(room)
				room
			} ! msg
			
		/** User sends a message directed at the users in a certain room. */
		case msg @ (upstream: ActorRef, ip: String, ChatReq(_, roomName, _)) =>
			rooms.get(roomName).fold {
				upstream ! ProtocolError("chat", "Room does not exist.")
			} { room =>
				println(s"sending chat message to room $roomName for processing.")
				room ! msg
			}
		
		/** Graceful upstream removal for window close and/or unlisten to room. */
		case msg @ (upstream: ActorRef, ip: String, DisconnectReq(tokens, optRoom)) =>
			optRoom.fold[Unit] {
				rooms.values.foreach { _ ! msg }
			} { roomName =>
				rooms.get(roomName).fold {
					upstream ! ProtocolError("disconnect", "Room does not exist.")
				} { room =>
					room ! msg
					upstream ! ProtocolOk("disconnect", "Room exited successfully.")
				}
			}
			
		case Terminated(room) =>
			rooms
				.find { case(name, ref) => ref == room }
				.foreach { case (name, ref) => rooms.remove(name) }
		
		case _ => println("failed to catch!")
	}

}

object Receptionist {
	def props() = Props(new Receptionist())
}