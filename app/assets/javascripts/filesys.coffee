
# Since images can be much larger than what is supported by websocket frames,
# images need to be broken up in pieces.

packageImage = (base64Image, maxLength, room) ->
	imageId = [1..20].map( -> Math.floor(Math.random() * 10)).join("") + "_id"
	parts = Math.floor(base64Image.length / maxLength) +
		(if base64Image.length % maxLength != 0 then 1 else 0)

	chunks = new Array(parts)
	i = 0
	offset = 0

	while i < parts
		offset = i * maxLength
		chunks[i] =
			data: base64Image.substring(offset, offset + maxLength)
			id: imageId
			part: i
			resource: "image"
			room: room
		i++

	chunks

$('#chat-out-container').on('drop', '.chat-out', (ev) ->
	ev.preventDefault()
	files = ev.originalEvent.dataTransfer.files

	Array.prototype.forEach.call(files, (file) ->
		if file.type.match("image.*")
			reader = new FileReader()

			reader.onload = (e) ->
				room = ninja.rooms.active()
				packages = packageImage(e.target.result, 6000, room)
				ninja.socket.send(
					resource: "image-init"
					id: packages[0].id
					parts: packages.length
					room: room
				)
				###
				submit = (i) ->
					ninja.socket.send(packages[i])
					if i < packages.length
						setTimeout ( -> submit(i + 1)), 200
				submit(0)
				###
				packages.forEach (pkg) -> ninja.socket.send(pkg)

			reader.readAsDataURL(file)
	)
).on('dragover', '.chat-out', (ev) ->
	ev.preventDefault()
	ev.originalEvent.dataTransfer.dropEffect = 'copy'
)
