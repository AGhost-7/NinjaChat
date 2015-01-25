package actors
import akka.actor.{ActorRef}

/**
 * This is used to submit a add(>>+) or remove (>>-) directive on 
 * ClientConnnection to the room handling actor.
 * e.g. roomActor >>- connectionActor
 */
case class >>+(actor: ActorRef)
case class >>-(actor:ActorRef)

package object dsl {

	implicit class actorOperators(self: ActorRef) {
		def >>+(actor:ActorRef) = self ! new >>+(actor)
		def >>-(actor:ActorRef) = self ! new >>-(actor)
	}
	
}