<!DOCTYPE html>
<%@ page session="false"%>
<%@page import="com.github.dbadia.sqrl.server.example.Constants"%>
<html lang="en">
<head>
<title>SQRL Java Server Demo</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"	href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" >
<script	src="//ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js"></script>
<script	src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>
</head>
<script	src="//cdnjs.cloudflare.com/ajax/libs/atmosphere/2.2.9/atmosphere.js"></script>
</head>
<body>

	<div class="container">
		<div class="jumbotron">
			<h1>SQRL Java Server Demo</h1>
			<p><%=request.getAttribute(com.github.dbadia.sqrl.server.example.Constants.JSP_SUBTITLE)%></p>
		</div>
		<div class="row">
			<div class="col-sm-4">
				<h4 id="instruction"><%=(String) request.getAttribute("sqrlqrdesc")%></h4>
				<p>
					<img id="sqrlImg"
						src="<%=(String) request.getAttribute("sqrlqr64")%>"
						alt="<%=(String) request.getAttribute("sqrlqrdesc")%>" />
				</p>
				<a id="cancel" href="logout">Cancel SQRL authentication</a>
			</div>
			<div id="or" class="col-sm-4 ">
				<br />
				<p>
				<h3 class="text-center">OR</h3>
				</p>
			</div>
			<div id="uplogin" class="col-sm-4">
				<h3>Username / password login</h3>
				<p>
					Username: (alphanumeric)<br />
				<form action="login" method="post">
					<input type="text" name="username" pattern="[a-zA-Z0-9]+"maxlength="10" required>
					<br> Password:<br> 
					<input type="password" name="password" pattern="[a-zA-Z]+" value="sqrl" maxlength="10" required>
					<br> 
					<input type="hidden" name="type" value="up"><br> <input type="submit" value="Submit">
				</form>
				<br /> <br />
				<p>
					Note: password is "sqrl" for all users.<br />
				<p>You can create a new user with either authentication method</p>
				<br />
			</div>
		</div>
	</div>

	<!-- TODO: move to outside of body tag -->
	<!-- Include javascript here for readability. Real apps would move it to the server -->
	<script>
    $(document).ready(function() {
	// Atmosphere stuff for auto refresh
	var socket = atmosphere;
	var atmosphereurl = window.location.pathname.substring(0, window.location.pathname.lastIndexOf("/")) +'/sqrlauthpolling';
    var request = { url: atmosphereurl,
            contentType: "application/json",
            logLevel: 'debug',
            transport: 'sse',
            reconnectInterval: 5000,
            fallbackTransport: 'long-polling'};

        request.onOpen = function (response) {
        	console.log('Atmosphere connected using ' + response.transport );
        };
        
        request.onReconnect = function (request, response) {
            console.log('Connection lost, trying to reconnect. Trying to reconnect ' + request.reconnectInterval);
        };

        request.onReopen = function (response) {
            console.log('Atmosphere re-connected using ' + response.transport );
        };

        request.onMessage = function (response) {
            status = response.responseBody;
			console.log('received from server: ' + event.data);
			if (status.startsWith('ERROR_')) {
				window.location.replace('login?error='+status);
			} else if(status == 'AUTH_COMPLETE') {
		    	var myObject = new Object();
		    	myObject.status = 'redirect';
            	window.location.replace('sqrllogin');
			} else {
				// Not sure what else to do so just reload
				window.location.replace('login');
			}
        };

        request.onClose = function (response) {
        	console.log('Server closed the connection after a timeout');
        };

        request.onError = function (response) {
            console.log('Sorry, but there\'s some problem with your '
                + 'socket or the server is down');
        };
      
        var subSocket = socket.subscribe(request);
        subSocket.push('<%=(String) request.getAttribute("sqrlstate")%>');
        
		var sqrlImgSrc = $("#sqrlImg").attr('src');
		var showingSqrlQr = sqrlImgSrc != 'spinner.gif';

		if(showingSqrlQr) {
			$("#cancel").hide();
		}
		
	    $("#sqrlImg").on('click', function() {
	    	$("#uplogin").hide();
	    	$("#or").hide();
	        $("#sqrlImg").attr("src", "spinner.gif");
	        instruction.innerText = "Waiting for SQRL client";
			$("#cancel").show();
	    	if(showingSqrlQr) {
	    		window.location.replace("<%=(String) request.getAttribute("sqrlurl")%>");
	    	}
	    });

	});
	

	</script>
</body>
</html>