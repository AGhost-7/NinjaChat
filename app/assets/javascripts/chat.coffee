do ->
	
	$('#chat-in').keydown (e) ->
		if e.keyCode == 13
			ninja.socket.send(
				resource: "chat-message"
				tokens: ninja.tokens.get()
				content: $(this).val()
				room: ninja.rooms.active()
			)
			
			$(this).val('')
			
			e.preventDefault()
	
	###
		~~ Socket Event Handlers ~~
	###
	
	ninja.socket.onopen( ->
		console.log('sending chat room request')
		ninja.socket.send(
			resource: "room",
			room: "Global",
			tokens: ninja.tokens.get()
		)
	)
	
	ninja.socket.on('room', 'ok', (obj) ->
		html = 
			'<p class="server-msg">' +
				'Room joined.' +
			'</p>'
		

		ninja.rooms.add(obj.content)
		ninja.rooms.active(obj.content)
		ninja.rooms.append(obj.content, html)
		
	)
	
	ninja.socket.on('room', 'error', (msg)->
		html = '<p class="chat-error">' + msg.reason + '</p>'
		alert(msg.reason)
	)
	
	ninja.socket.on('user-message', 'ok', (obj)->
		html = 
			'<p>' +
				'<u>' + obj.userName + ':</u>&nbsp;' +
				ninja.escape(obj.content) +
			'</p>'
		ninja.rooms.append(obj.room, html)
	)
	
	ninja.socket.on('chat-message', 'error', (msg)->
		html = 
			'<p class="text-danger">' +
				msg.reason +
			'</p>'
		ninja.rooms.append(msg.room, html)
	)
	
	ninja.socket.on('notification', 'ok', (msg)->
		html = 
			'<p class="server-msg">' +
				msg.content +
			'</p>'
		ninja.rooms.append(msg.room, html)
	)
	

	
