package models

import play.api.libs.json._

import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.functional.syntax._

// Notes...
// User can request:
// - registration
// - login
// - logout
// - access to a chat room (server will forward messages directed to chat room
// at socket).
// - leave chat room.
// - send a message to a chat room

//  server can request:
// - notification at chat room.
// - message from user.
// - reply with new token.
// - deny access(for login and registration), with reason why being given.

sealed trait ProtocolMsg

object ProtocolMsg {
	
	implicit class readsAdhocks[A](rd: Reads[A]){
		def typeHint(kv: (String, String)) = {
			val (field, value) = kv
			
			val rd2 = Reads[JsValue] { js =>
				(js \ field).asOpt[String] match {
					case Some(v) =>
						if(v == value) JsSuccess(js)
						else JsError("Type hint mismatch.")
					case None =>
						JsError("Type hint not defined.")
				}	
			}
		
			rd.compose(rd2)
		}
	}
	
	implicit class writesAdhocks[A](wr: OWrites[A]){
		def typeHint(kvs: (String, String)*) = wr.transform { js: JsValue =>
			js match {
				case JsObject(seq) => 
					val merged = seq ++ kvs.map { case(k, v) => (k, JsString(v)) }
					
					JsObject(merged)
				case other => other
			}
		}
	}
	
	/** Server-to-client writes
	 */
	
	val userMessageWrite = Json
			.writes[UserMessage]
			.typeHint("resource" -> "user-message", "code" -> "ok")
		
	val notificationWrite = Json
			.writes[Notification]
			.typeHint("resource" -> "notification", "code" -> "ok")
		
	val protocolErrorWrite = Json
			.writes[ProtocolError]
			.typeHint("code" -> "error")
		
	val protocolOkWrite = Json
			.writes[ProtocolOk]
			.typeHint("code" -> "ok")
		
	val userIdentityWrite = Json
			.writes[UserIdentity]
			.typeHint("resource" -> "identity", "code" -> "ok")
	
	val imageSubmissionWrite = Json
			.writes[ImageSubmission]
			.typeHint("resource" -> "image", "code" -> "ok")
			
	/** Client-to-server reads
	 */
	
	val registrationReqRead = Json
			.reads[RegistrationReq]
			.typeHint("resource" -> "registration")
		
	val loginReqRead = Json
			.reads[LoginReq]
			.typeHint("resource" -> "login")
		
	val logoutReqRead = Json
			.reads[LogoutReq]
			.typeHint("resource" -> "logout")
		
	val chatReqRead = Json
			.reads[ChatReq]
			.typeHint("resource" -> "chat-message")
		
	val roomReqRead = Json
			.reads[RoomReq]
			.typeHint("resource" -> "room")
	
	val identityReqRead = Json
			.reads[IdentityReq]
			.typeHint("resource" -> "identity")
			
	val disconnectReqRead = Json
		.reads[DisconnectReq]
		.typeHint("resource" -> "disconnect")
	
	val imageReqRead = Json
		.reads[ImageReq]
		.typeHint("resource" -> "image")
	
	/**
	 * This is the final formatter which will be executed automatically at the
	 * end/beginning of the pipe.
	 */
		
	implicit def protocolMsgFormat: Format[ProtocolMsg] = Format(
			(__ \ "resource").read[String].flatMap {
				case "registration" => registrationReqRead.map(identity)
				case "login" => loginReqRead.map(identity)
				case "logout" => logoutReqRead.map(identity)
				case "chat-message" => chatReqRead.map(identity)
				case "room" => roomReqRead.map(identity)
				case "identity" => identityReqRead.map(identity)
				case "ping" => Reads[ProtocolMsg] { _ => JsSuccess(Ping) }
				case "disconnect" => disconnectReqRead.map(identity)
				case "image" => imageReqRead.map(identity)
				case _ => Reads { _ => JsError("Format is invalid.") }
		},
		Writes {
			case nt: Notification => notificationWrite.writes(nt)
			case msg: UserMessage => userMessageWrite.writes(msg)
			case err: ProtocolError => protocolErrorWrite.writes(err)
			case ok: ProtocolOk => protocolOkWrite.writes(ok)
			case id: UserIdentity => userIdentityWrite.writes(id)
			case Ping => 
				val seq = Seq(
					("resource", JsString("ping")),
					("code", JsString("ok"))
				)
				JsObject(seq)
			case img: ImageSubmission => imageSubmissionWrite.writes(img)
			case _ => Json.obj("error" -> "Json writes not implemented.")
			
		}
	)
	
	implicit def protocolMsgFrameFormatter: FrameFormatter[ProtocolMsg] = 
		FrameFormatter.jsonFrame.transform(
      protocolMsg => Json.toJson(protocolMsg),
      json => Json.fromJson[ProtocolMsg](json).fold(
        invalid => throw new RuntimeException("Bad client event on WebSocket: " + invalid),
        valid => valid
      )
    )
	
}

/** Two way objects...
 */
case object Ping extends ProtocolMsg

/** Client-to-server definitions.
 * 
 *  Implicit formats are stored in the Implicits package object in the Models 
 *  file.
 */

// If Option is None, then disconnect from all rooms.
case class DisconnectReq(tokens: List[String], room: Option[String]) extends ProtocolMsg

case class RegistrationReq(name: String, password: String) extends ProtocolMsg 

case class LoginReq(name: String, password: String) extends ProtocolMsg

case class LogoutReq(tokens: List[String]) extends ProtocolMsg 

case class RoomReq(tokens: List[String], room: String) extends ProtocolMsg 

case class ChatReq(tokens: List[String], room: String, content: String) extends ProtocolMsg 

case class IdentityReq(tokens: List[String], withAllTokens: Option[Boolean] = None) extends ProtocolMsg

/** Image is submitted using base64 format */
case class ImageReq(tokens: List[String], room: String, content: String) extends ProtocolMsg

/** Server-to-client definitions. */

case class UserMessage(userName: String, room: String, content: String) extends ProtocolMsg

// e.g., User "x" logs into the room.
case class Notification(room: String, content: String) extends ProtocolMsg 

//Resource will correspond to the code the client sent. Reason
//is the exact GUI response that the user will see.
case class ProtocolError(resource: String, reason: String) extends ProtocolMsg 

//Ok response will return a result, for example a token for the login.
case class ProtocolOk(resource: String, content: String) extends ProtocolMsg

case class UserIdentity(name: String, tokens: Option[List[String]] = None) extends ProtocolMsg

case class ImageSubmission(name: String, room: String, content: String) extends ProtocolMsg
