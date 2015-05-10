

$cont = $('#chat-out-container')
$room = (room) ->
	$cont
		.find('.chat-out')
		.filter () -> $(this).data('room') == room

ninja.rooms.on('active', (roomName) ->
	$cont.find('.chat-out').addClass("hidden")
	$room(roomName).removeClass('hidden')
)

ninja.rooms.on('append', (room, html) ->
	$out = $room(room)
	isScrolledToBottom = $out[0].scrollHeight - $out[0].clientHeight <= $out[0].scrollTop + 1
	$out.append(html)
	if isScrolledToBottom
		$out[0].scrollTop = $out[0].scrollHeight - $out[0].clientHeight
)

ninja.rooms.on('add', (roomName) ->
	html = '<div class="well chat-out" data-room="' + roomName + '" ></div>'
	$cont.append(html)
)
