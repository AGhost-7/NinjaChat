###
	A couple of abstractions...
###

do ->
	sock = new WebSocket("ws://" + location.host + "/socket")
	
	# Primitive socket handlers
	sockHandlers = 
		"onopen": [],
		"onerror": [], 
		"onmessage":[]
	
	# message handlers
	callbacks = {}
	
	ninja = window.ninja = {}
	
	ninja.socket = 
		on: (resource, code, callback) ->
			if arguments.length == 2
				cde = arguments[0]
				cback = arguments[1]
				callbacks[cde] = callbacks[code] || []
				callbacks[cde].push(cback)
			else if arguments.length == 3
				callbacks[code] = callbacks[code] || {}
				callbacks[code][resource] = callbacks[code][resource] || []
				callbacks[code][resource].push(callback)
		send: (obj) ->
			sock.send(JSON.stringify(obj))
			
	for key of sockHandlers
		do (key) ->
			ninja.socket[key] = (callback) ->
				sockHandlers[key].push(callback)
			
	ninja.userName = do ->
		val = undefined
		funcs = []
		
		get: () ->
			val
		set: (newVal) ->
			val = newVal
			callback(newVal) for callback in funcs
		listen: (callback) ->
			funcs.push(callback)
	
	ninja.tokens = do ->
		self = {}
		str = localStorage.getItem("tokens")
		
		if str != "" && str != null
			arr = JSON.parse(str).filter (e) -> !!e
		else
			arr = []
		
		self.add = (t) ->
			arr = ([t].concat(arr)).slice(0,5)
			localStorage.setItem("tokens", JSON.stringify(arr))
		
		self.get = () ->
			arr
			
		self.clear = () ->
			arr = []
			localStorage.setItem("tokens", "[]")
		
		self
	
	# TODO: get a proper GUI message added to this.
	sock.onerror = (error) ->
		alert('Oh noes! A colony of monkeys just ran away with the socket connection.')
		callback(error) for callback in sockHandlers.onerror
		
	sock.onmessage = (event) ->
		obj = JSON.parse(event.data)
		if obj.code && callbacks[obj.code]
			atCode = callbacks[obj.code]
			if obj.resource && atCode[obj.resource]
				callback(obj) for callback in atCode[obj.resource]
			else if Array.isArray(atCode)
				callback(obj) for callback in atCode
			
		# and now the primitive event handlers.
		callback(event) for callback in sockHandlers.onmessage
		
	sock.onopen = (e) ->
		callback(e) for callback in sockHandlers.onopen
		
		
	window.onbeforeunload = (e) ->
		sock.close()
	