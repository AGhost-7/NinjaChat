package actors

import akka.actor._

import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import scala.concurrent.Future
import models._

import scala.collection.mutable.{Map => MMap}

/** Room supervisor */
class Receptionist() extends Actor with ActorLogging with WebsocketProcessing {
		
	val rooms = MMap[String, ActorRef]()
	
	override def preStart = {
		log.info(s"Receptionist created as: ${self.path}")
		println(self.path)
	}

	val websocketReceive = new WebsocketProcessor {

		val process: PartialFunction[ProtoReq, Unit] = {

			/** User requests to receive messages from a certain room. */
			case RoomReq(roomName) =>
				val room = rooms.get(roomName).getOrElse {
					val room: ActorRef = context.actorOf(Room.props(roomName))
					rooms += roomName -> room
					context.watch(room)
					room
				}
				room ! request

			/** User sends a message directed at the users in a certain room. */
			case ChatReq(roomName, _) =>
				rooms.get(roomName).fold {
					upstream ! ProtocolError("chat", "Room does not exist.")
				} { room =>
					log.info(s"sending chat message to room $roomName for processing.")
					room ! request
				}

			/** Graceful upstream removal for window close and/or unlisten to room. */
			case DisconnectReq(tokens, optRoom) =>
				optRoom.fold[Unit] {
					rooms.values.foreach { _ ! request }
				} { roomName =>
					rooms.get(roomName).fold {
						upstream ! ProtocolError("disconnect", "Room does not exist.")
					} { room =>
						room ! request
					}
				}

			case img: ImageReq =>
				rooms.get(img.room).fold {
					upstream ! ProtocolError("image", "Image could not be sent to non-existent room.")
				} { room =>
					room ! request
				}

			case img: ImageReqInit =>
				rooms.get(img.room).fold {
					upstream ! ProtocolError("image-init", "Room does not exist!")
				}	{ room =>
					room ! request
				}
		}

	}

	val _receive: Receive = {
//		/** Nofity all rooms that user has logged out. */
//		case notif: LogoutNotification =>
//			rooms.values.foreach { _ ! notif }
//
//		/** Notify all rooms that a user has just logged in. */
//		case notif: LoginNotification =>
//			rooms.values.foreach { _ ! notif }

		case Terminated(room) =>
			rooms
				.find { case(name, ref) => ref == room }
				.foreach { case (name, ref) => rooms.remove(name) }
	}

	val receive: Receive = websocketReceive.orElse(_receive)

}

object Receptionist {
	def props() = Props(new Receptionist())
}