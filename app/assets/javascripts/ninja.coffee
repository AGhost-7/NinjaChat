

ninja = window.ninja = {}

###
	~~ WebSocket Abstractions ~~
###

sock = new WebSocket("wss://" + location.host + "/socket")

lastMessageSent = undefined

socketUnload = false

# Primitive socket handlers
sockHandlers =
	"onopen": [],
	"onclose":[],
	"onerror": [],
	"onmessage":[]

# message handlers
callbacks = {}

ninja.socket =
	on: (resource, code, callback) ->
		callbacks[resource] = callbacks[resource] || {}
		callbacks[resource][code] = callbacks[resource][code] || []
		callbacks[resource][code].push(callback)

	send: (obj) ->
		lastMessageSent = obj
		sock.send(JSON.stringify(obj))

for key of sockHandlers
	do (key) ->
		ninja.socket[key] = (callback) ->
			sockHandlers[key].push(callback)

# TODO: get a proper GUI message added to this.
sock.onerror = (error) ->
	alert('Oh noes! A colony of monkeys just ran away with the socket connection.')
	callback(error) for callback in sockHandlers.onerror

sock.onclose = (event) ->
	if !socketUnload
		alert('Oh noes! A colony of monkeys just ran away with the socket connection.')
	callback(event) for callback in sockHandlers.onclose

sock.onmessage = (event) ->
	obj = JSON.parse(event.data)
	#console.log('socket message received')
	#console.log(obj)
	if obj.resource && callbacks[obj.resource]
		resourceCBs = callbacks[obj.resource]
		if obj.code && resourceCBs[obj.code]
			callback(obj) for callback in resourceCBs[obj.code]

	# and now the primitive event handlers.
	callback(event) for callback in sockHandlers.onmessage

sock.onopen = (e) ->
	callback(e) for callback in sockHandlers.onopen

window.onbeforeunload = (e) ->
	# Don't want the error message popping up if its intentionally
	# closed by the client.
	socketUnload = true
	sock.close()

###
	~~ Misc. Abstractions ~~
###

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

naughtyChars =
	"&": "&amp;"
	"<": "&lt;"
	">": "&gt"
	'"': "&quot;"
	"'": "&#39;"
	"/": "&#x2F"

ninja.escape = (text) ->
	String(text).replace(/[&<>"'\/]/g, (c) ->
		naughtyChars[c]
	)
