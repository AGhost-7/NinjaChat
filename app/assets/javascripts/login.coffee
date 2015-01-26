do ->
	
	$inName = $('#nav-in-name')
	$inPw = $('#nav-in-pw')
	formState = ''
	
	###
		~~ Model change event handlers ~~
	###
	
	ninja.userName.listen (newName) ->
		if newName == undefined
			$('#name-span').text('newcomer')
			logout.nav(false)
			login.nav(true)
			register.nav(true)
		else
			$('#name-span').text(newName)
			logout.nav(true)
			login.nav(false)
			register.nav(false)
		
	###
		~~ UI event handlers ~~
	###
	
	class NavElem
		constructor: (@$a) ->
			
		nav: (bool) ->
			if(bool)
				@$a.parent().removeClass('hidden')
			else
				@$a.parent().addClass('hidden')
				
				
	login = new NavElem($('#login-nav'))
	
	register = new NavElem($('#register-nav'))
	
	logout = new NavElem($('#logout-nav'))
	
	login.$a.click (e) -> 
		navbarAlert.clear()
		formState = 'login'
		
	register.$a.click (e) -> 
		navbarAlert.clear()
		formState = 'registration'
	
	logout.$a.click (e) ->
		ninja.socket.send(
			code: "logout",
			tokens: ninja.tokens.get()
		)
		e.preventDefault()
		
	navbarAlert = (message) ->
		html = 
			'<div class="alert alert-danger" role="alert">' +
				'<button type="button" class="close" data-dismiss="alert" aria-label="Close">' +
					'<span aria-hidden="true">&times;</span>' +
				'</button>' +
				message +
			'</div>'
			
		navbarAlert.$cont.html(html)
		
	navbarAlert.$cont = $('#nav-alert-container')
	
	navbarAlert.clear = () ->
		navbarAlert.$cont.html('')
	
	$('#nav-ok-btn').click (e) ->
		ninja.socket.send(
			code: formState,
			name: $inName.val(),
			password: $inPw.val()
		)
		
	###
		~~ Socket event handlers ~~
	###
	
	ninja.socket.on('logout', 'ok', (obj) ->
		ninja.userName.set(undefined)
		ninja.tokens.clear()
	)
	
	ninja.socket.on('logout', 'error', (obj) ->
		alert(obj.reason)
	)
	
	onTokenAcquire = (obj) ->
		ninja.userName.set($inName.val())
		$inName.val('')
		$inPw.val('')
		$('#nav-input').collapse('hide')
		ninja.tokens.add(obj.content)
		
	ninja.socket.on('login', 'ok', onTokenAcquire)
	
	ninja.socket.on('login', 'error', (obj) ->
		navbarAlert(obj.reason)
	)
	
	ninja.socket.on('registration', 'ok', onTokenAcquire)
	
	ninja.socket.on('registration', 'error', (obj) ->
		navbarAlert(obj.reason)
	)
	
	# I'm going to need to check if the user is valid with the tokens
	# that it currently has. Client has to verify on its own what it can 
	# do with its stored state.
	
	ninja.socket.on("identity", (obj) -> 
		ninja.userName.set(obj.name)
	)
	
	ninja.socket.onopen( ->
		ninja.socket.send(
			code: "identity",
			tokens: ninja.tokens.get()
		)
	)
	
	