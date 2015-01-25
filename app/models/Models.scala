package models


import play.api.libs.json._




/**
 * Json serializers all in one import.
 */
package object Implicits {
	
	/**
	 * Formats for MongoDB, etc.
	 */
	
	implicit def userFormat = User.jsonFormat
	implicit def tokenFormat = Token.jsonFormat
	
	/**
	 * Implicit format for the websockets.
	 */
	implicit def protocolMsgFormat = ProtocolMsg.protocolMsgFormat
	
}






