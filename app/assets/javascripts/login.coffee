
$inName = $('#nav-in-name')
$inPw = $('#nav-in-pw')
$aLogin = $('#login-nav')
$aLogout = $('#logout-nav')
$aRegister = $('#register-nav')

formState = ''

###
	~~ Model change event handlers ~~
###

ninja.userName.listen (newName) ->
	if newName == undefined
		$('#name-span').text('newcomer')
		$aLogout.parent().addClass('hidden')
		$aLogin.parent().removeClass('hidden')
		$aRegister.parent().removeClass('hidden')
	else
		$('#name-span').text(newName)
		$aLogout.parent().removeClass('hidden')
		$aLogin.parent().addClass('hidden')
		$aRegister.parent().addClass('hidden')

###
	~~ UI event handlers ~~
###

$aLogin.click (e) ->
	navbarAlert.clear()
	formState = 'login'

$aRegister.click (e) ->
	navbarAlert.clear()
	formState = 'registration'

$aLogout.click (e) ->
	ninja.socket.send(
		resource: "logout",
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
		resource: formState,
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

ninja.socket.on('identity','ok', (obj) ->
	ninja.userName.set(obj.name)
)

ninja.socket.onopen( ->
	ninja.socket.send(
		resource: "identity",
		tokens: ninja.tokens.get()
	)
)
