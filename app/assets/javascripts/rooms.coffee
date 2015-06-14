###
	Decouple using data events.
###

roomNames = []
listeners =
	'remove': []
	'add': []
	'active': []
	'append': []

activeRoom = ''

window.ninja.rooms =
	on: (event, callback)->
		listeners[event].push(callback)
	remove: (room) ->
		roomNames = roomNames.filter (elem) -> elem != room
		callback(room) for callback in listeners.remove
	active: (room) ->
		if room == undefined
			activeRoom
		else if room != activeRoom
			activeRoom = room
			callback(room) for callback in listeners.active
	add: (room) ->
		roomNames.push(room)
		callback(room) for callback in listeners.add
	roomNames: () ->
	    roomNames
	append: (room, arg) ->
		callback(room, arg) for callback in listeners.append
