do(ninja) ->

	$plus = $('#add-room').parent()
	$ul = $('#room-nav')
	$room = (room) -> $ul.find('li[name="' + room + '"]')
		
		
	roomAddHtml =
		'<div class="input-group">' +
			'<input type="text" class="form-control" placeholder="room name" id="join-room-input"/>' +
			'<span class="input-group-btn">' +
				'<button class="btn btn-primary" id="join-room-ok">Ok</button>' +
			'</span>' +
		'</div>' 
		
	$('#add-room').popover(
		html: true
		placement: 'bottom'
		content: roomAddHtml
	).on('shown.bs.popover', () ->
		$('#join-room-ok').click () ->
				$in = $('#join-room-input')
				room = $in.val()
				rooms = ninja.rooms.roomNames()
				console.log(rooms)
				if room == undefined or room == '' or rooms.indexOf(room) > -1
					$in.parent().addClass('has-error')
				else
					$('#add-room').popover('hide')
					ninja.socket.send(
						resource: 'room'
						room: $('#join-room-input').val()
						tokens: ninja.tokens.get()
					)
			
	)
	
	ninja.rooms.on('add', (roomName) ->
		html = 
			'<li role="presentation" name="' + roomName + '">' + 
				'<a href="#">' + roomName + ' <span class="badge" data-unread=0></span></a>' + 
			'</li>'
		$plus.before(html)
		$room(roomName).click ()->
			ninja.rooms.active(roomName)
	)
	
	ninja.rooms.on('active', (roomName)->
		$ul.find('li').removeClass('active')
		$r = $room(roomName)
		$r.addClass('active')
		$r.find('span').text('').data('unread', 0)
	)
	
	ninja.rooms.on('append', (roomName, html) ->
		if roomName != ninja.rooms.active()
			$r = $room(roomName)
			$span = $r.find('span')
			count = $span.data('unread') + 1
			$span.text(count)
			$span.data('unread', count)
	)
	
	
	
	
	