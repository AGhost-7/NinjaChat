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
	
	/**
	 * Part 2 of the Reads. This will return a reads object which
	 * checks if the code is correct.
	 */
	
	def readCode(code: String) = Reads[JsValue] { js =>
		(js \ "code").asOpt[String] match {
			case Some(jsCode) => 
				if(code == jsCode) JsSuccess(js)
				else JsError("Protocol code mismatch.")
			case None => 
				JsError("Protocol requires a code field.")
		}
	}
	
	/**
	 * Part 2 of the Writes. Adds the code to the json object for the client.
	 */
	
	def writeCode(code: String): JsValue => JsValue = { js => 
		js match {
			case JsObject(seq) => JsObject(seq :+ ("code", JsString(code)))
			case _ => js
		}
	}
		
	/**
	 * Server-to-client writes
	 */
	
	val userMessageWrite = 
		Json.writes[UserMessage].transform(writeCode("chat-message"))
		
	val notificationWrite = 
		Json.writes[Notification].transform(writeCode("notification"))
		
	val protocolErrorWrite = //Json.writes[ProtocolError]
		Json.writes[ProtocolError].transform(writeCode("error"))
		
	val protocolOkWrite = //Json.writes[ProtocolOk]
		Json.writes[ProtocolOk].transform(writeCode("ok"))
		
	val userIdentityWrite =
		Json.writes[UserIdentity].transform(writeCode("identity"))
	
	/**
	 * Client-to-server reads
	 */
	
	val registrationReqRead =
		Json.reads[RegistrationReq].compose(readCode("registration"))
		
	val loginReqRead = 
		Json.reads[LoginReq].compose(readCode("login"))
		
	val logoutReqRead = 
		Json.reads[LogoutReq].compose(readCode("logout"))
		
	val chatReqRead = 
		Json.reads[ChatReq].compose(readCode("chat-message"))
		
	val roomReqRead = 
		Json.reads[RoomReq].compose(readCode("room"))
	
	val identityReqRead = 
		Json.reads[IdentityReq].compose(readCode("identity"))
	
	/**
	 * This is the final formatter which will be executed automatically at the
	 * end/beginning of the pipe.
	 */
		
	implicit def protocolMsgFormat: Format[ProtocolMsg] = Format(
			(__ \ "code").read[String].flatMap {
			case "registration" => registrationReqRead.map(identity)
			case "login" => loginReqRead.map(identity)
			case "logout" => logoutReqRead.map(identity)
			case "chat-message" => chatReqRead.map(identity)
			case "room" => roomReqRead.map(identity)
			case "identity" => identityReqRead.map(identity)
			case _ => Reads { _ => JsError("Format is invalid.") }
		},
		Writes {
			case nt: Notification => notificationWrite.writes(nt)
			case msg: UserMessage => userMessageWrite.writes(msg)
			case err: ProtocolError => protocolErrorWrite.writes(err)
			case ok: ProtocolOk => println("Writing ok...");protocolOkWrite.writes(ok)
			case id: UserIdentity => userIdentityWrite.writes(id)
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

/**
 * Client-to-server definitions.
 * 
 * Implicit formats are stored in the Implicits package object in the Models 
 * file.
 */

case class RegistrationReq(name: String, password: String) extends ProtocolMsg 

case class LoginReq(name: String, password: String) extends ProtocolMsg

case class LogoutReq(tokens: List[String]) extends ProtocolMsg 

case class RoomReq(tokens: List[String], name: String) extends ProtocolMsg 

case class ChatReq(tokens: List[String], room: String, content: String) extends ProtocolMsg 

case class IdentityReq(tokens: List[String], withAllTokens: Option[Boolean] = None) extends ProtocolMsg

/**
 * Server-to-client definitions.
 */

case class UserMessage(userName: String, room: String, content: String) extends ProtocolMsg

// e.g., User "x" logs into the room.
case class Notification(room: String, content: String) extends ProtocolMsg 

//Resource will correspond to the code the client sent. Reason
//is the exact GUI response that the user will see.
case class ProtocolError(resource: String, reason: String) extends ProtocolMsg 

//Ok response will return a result, for example a token for the login.
case class ProtocolOk(resource: String, content: String) extends ProtocolMsg

case class UserIdentity(name: String, tokens: Option[List[String]] = None) extends ProtocolMsg
