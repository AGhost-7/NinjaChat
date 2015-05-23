package actors
import akka.actor._
import models._

/** Some information should be added to the "context" instead of having to pass
	* it around with tuples.
	*/
case class WebsocketRequest[+R <: ProtoReq](req: R, upstream: ActorRef, name: String)

trait WebsocketProcessing { act: Actor =>

	val websocketReceive: WebsocketProcessor

	abstract class WebsocketProcessor
			extends Receive {
		processor =>

		protected var upstream: ActorRef = _
		protected var name: String = _
		protected var request: WebsocketRequest[_] = _

		protected val process: PartialFunction[ProtoReq, Unit]

		// pretty ugly, but there's no way around this afaik.
		def isDefinedAt(a : Any) = {
			a match {
				case r: WebsocketRequest[_] => process.isDefinedAt(r.req)
				case _ => false
			}
		}

		def apply(a: Any) = {
			// already know its type, so just cast it.
			val cr = a.asInstanceOf[WebsocketRequest[ProtoReq]]

			processor.upstream = cr.upstream
			processor.name = cr.name
			processor.request = cr
			process(cr.req)
		}

	}

}
