do ->
	maxSize = 40 * 1024
	$('#chat-out-container').on('drop', '.chat-out', (ev) ->
		ev.preventDefault()
		files = ev.originalEvent.dataTransfer.files
		
		Array.prototype.forEach.call(files, (file) ->
			if file.type.match("image.*")
				if file.size > maxSize
					html = 
						'<p class="text-danger">' +
							'File ' + file.name + ' exceeds the 40kb limit for image transfers.' +
						'</p>'
					
					ninja.rooms.append(ninja.rooms.active(), html)
				else
					reader = new FileReader()
					
					reader.onload = do(file) ->
						(e) ->
							console.log('file content',e.target.result)
							
							ninja.socket.send(
								resource: "image"
								content: e.target.result
								room: ninja.rooms.active()
								tokens: ninja.tokens.get()
							)
							
					reader.readAsDataURL(file)
		)
	).on('dragover', '.chat-out', (ev) ->
		ev.preventDefault()
		ev.originalEvent.dataTransfer.dropEffect = 'copy'
	)
	