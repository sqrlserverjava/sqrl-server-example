<!DOCTYPE html>
<%@ page session="false" %>
<%@page import="com.github.dbadia.sqrl.server.example.Constants"%>
<html lang="en">
<head>
<title>SQRL Java Server Demo</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"
	href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css">
<script
	src="//ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js"></script>
<script
	src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>
</head>
<body>

	<div class="container">
		<div class="jumbotron">
			<h1>SQRL Java Server Demo</h1>
			<%=request.getAttribute(com.github.dbadia.sqrl.server.example.Constants.JSP_SUBTITLE)%>
		</div>
		<div class="row">
			<div class="col-sm-4">
				<h3><%=(String) request.getAttribute("sqrlqrdesc")%></h3>
				<p>
					<a href="<%=(String) request.getAttribute("sqrlurl")%>" tabindex="-1">
					<img src="<%=(String) request.getAttribute("sqrlqr64")%>"
						alt="<%=(String) request.getAttribute("sqrlqrdesc")%>" /></a>
				</p>
				<%
					String imageString = (String) request.getAttribute("sqrlqr64");
					// Lame hack.  Our spinner happens to be gif while the QR code is a jpg so use that
					if (imageString != null && imageString.contains("gif")) { // SQRL auth in progress
				%>
				<a href="logout">Cancel SQRL authentication</a>
			</div>
			<%
				} else { // Default login screen display
			%>
		</div>
		<div class="col-sm-4 ">
			<br />
			<p>
			<h3 class="text-center">OR</h3>
			</p>
		</div>
		<div class="col-sm-4">
			<h3>Username / password login</h3>
			<p>
				Username: (alphanumeric)<br />
			<form action="login" method="post">
				<input type="text" name="username" pattern="[a-zA-Z0-9]+"
					maxlength="10" required><br> Password:<br> <input
					type="password" name="password" pattern="[a-zA-Z]+" value="sqrl"
					maxlength="10" required><br> <input type="hidden"
					name="type" value="up"><br> <input type="submit"
					value="Submit">
			</form>
			<br /> <br />
			<p>
				Note: password is "sqrl" for all users.<br />
			<p>You can create a new user with either authentication method</p>
			<br />
		</div>
		<%
			}
		%>
	</div>
	<!-- use atmosphere to figure out when to refresh the page -->
	<!-- TODO: put localhost:port/sqrlexmaple in an attribute -->
	<script>
		var socket = atmosphere;
	    //var request = { url: document.location.toString() + '/update',
	    var urlpath = window.location.pathname.substring(0, window.location.pathname.lastIndexOf("/")) +'/update';
	    var request = { url: urlpath,
	        contentType: "application/json",
	        logLevel: 'debug',
	        transport: 'sse',
	        reconnectInterval: 5000,
	
	        fallbackTransport: 'long-polling'};

	    request.onOpen = function (response) {
	    	console.log('atmosphere onOpen');
	    };

	    request.onReconnect = function (request, response) {
	    	console.log('atmosphere onReconnect');
	    };

	    request.onReopen = function (response) {
	    	console.log('atmosphere onReopen');
	    };

	    request.onMessage = function (response) {
	        var message = response.responseBody;
	        console.log(message);
	        try {
	            var json = atmosphere.util.parseJSON(message);
	        } catch (e) {
	            console.log('This doesn\'t look like a valid JSON: ', message);
	            return;
	        }

	        input.removeAttr('disabled').focus();
	        if (!logged) {
	            logged = true;
	            status.text(myName + ': ').css('color', 'blue');
	        } else {
	            var color = json.author === author ? 'blue' : 'black';
	          	var datetime = new Date();
	            content.append('<p><span style="color:' + color + '">' + json.author + '</span> @ ' + +(datetime.getHours() < 10 ? '0' + datetime.getHours() : datetime.getHours()) + ':'
	                + (datetime.getMinutes() < 10 ? '0' + datetime.getMinutes() : datetime.getMinutes())
	                + ': ' + json.message + '</p>');
	            if(json.message == 'AUTH_COMPLETE') {
	            	subSocket.push(atmosphere.util.stringifyJSON({ author: author, message: 'redirect' }));
	            	window.location.replace('done.html');
	            }
	            
	        }
	    };

	    request.onClose = function (response) {
	    	console.log('atmosphere onClose');
	    };

	    request.onError = function (response) {
	    	console.log('atmosphere onError');
	    };
	    var subSocket = socket.subscribe(request);
	    subSocket.push(atmosphere.util.stringifyJSON({ 'correlator': <%=(String) request.getAttribute("correlator")%>, 'status': 'CORRELATOR_ISSUED'}));
	    
		// Log messages from the server
		webSocket.onmessage = function(e) {
			console.log('Server: ' + e.data);
			if(e.data.startsWith('ERROR_')) {
				window.location.replace("login?error=true");
			} else if(e.data == 'AUTH_COMPLETE') {
            	subSocket.push(atmosphere.util.stringifyJSON({  'correlator': <%=(String) request.getAttribute("correlator")%>, 'status': 'redirect' }));
            	window.location.replace('app');
			} else {
				// Reload the page TODO: update the page instead of refresh if not done
				location.reload(true);
			}
		};
	</script>
</body>
</html>