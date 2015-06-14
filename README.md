*NinjaChat*

Chat like a ninja.

This version uses Akka Actors to enable users to create as many chatrooms as they 
wish. Account logic is all done through websockets instead of ajax as well.



TODO:
* Change from a room-based to connection-based approach. Rooms are just going to
be filters for the clients.
	* Room only tracks client connections to forward room requests.
	* client connection maintains a list of users to send messages to based on which
	room the message is for.
* Decentralize actor system.
	