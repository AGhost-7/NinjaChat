###
	Decouple using data events.
###

do(ninja) ->
	roomNames = []
	listeners = 
		'remove': []
		'add': []
		'active': []
		'append': []
	
	activeRoom = ''
		
	ninja.rooms = 
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
		append: (room, html) ->
			callback(room, html) for callback in listeners.append
	
	
