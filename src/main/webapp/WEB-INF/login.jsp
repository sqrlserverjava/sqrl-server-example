<!DOCTYPE html>
<%@ page session="false"%>
<%@page import="com.github.dbadia.sqrl.server.example.Constants"%>
<html lang="en">
<head>
<title>SQRL Java Server Demo</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="refresh" content="<%=request.getAttribute(com.github.dbadia.sqrl.server.example.Constants.JSP_PAGE_REFRESH_SECONDS)%>">
<link rel="stylesheet"	href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" >
<script	src="//ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js"></script>
<script	src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>
<script	src="//cdnjs.cloudflare.com/ajax/libs/atmosphere/2.2.9/atmosphere.js"></script>
</head>
<body>

	<div class="container">
		<div class="jumbotron">
			<h1>SQRL Java Server Demo</h1>
			<p id="subtitle"><%=request.getAttribute(com.github.dbadia.sqrl.server.example.Constants.JSP_SUBTITLE)%></p>
		</div>
		<div class="row">
			<div class="col-sm-4">
				<h4 id="instruction"><%=(String) request.getAttribute("sqrlqrdesc")%></h4>
				<p>
					<img id="sqrlImg" onclick="sqrlInProgress()"
						src="<%=(String) request.getAttribute("sqrlqr64")%>"
						alt="<%=(String) request.getAttribute("sqrlqrdesc")%>" />
				</p>
				<a id="cancel"  href="logout">Cancel SQRL authentication</a>
			</div>
			<div id="or" class="col-sm-4 ">
				<br />
				<h3 class="text-center">OR</h3>
			</div>
			<div id="uplogin" class="col-sm-4">
				<h3>Username / password login</h3>
				<p>
					Username: (alphanumeric)<br />
				<form action="auth" method="post">
					<input type="text" name="username" pattern="[a-zA-Z0-9]+" maxlength="10" required>
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
</body>

	<!-- TODO: move to outside of body tag -->
	<!-- Include javascript here for readability. Real apps would move it to the server -->
	<script>
	// http://stackoverflow.com/a/11663507/2863942
	// if(!window.console){ window.console = {log: function(){} }; } 
	
	function sqrlInProgress() {
		var sqrlImgSrc = $("#sqrlImg").attr('src');
		var showingSqrlQr = sqrlImgSrc != 'spinner.gif';
    	if(!showingSqrlQr) {
    		return;
    	}
		$("#cancel").hide();
   		window.location.replace("<%=(String) request.getAttribute("sqrlurl")%>");
    	$("#uplogin").hide();
    	$("#or").hide();
        $("#sqrlImg").attr("src", "spinner.gif");
        instruction.innerText = "Waiting for SQRL client";
		$("#cancel").show();
    	if(subtitle.innerText.indexOf("error") >=0 ) {
    		subtitle.innerText = "";
    	}
	}
	
	function stopPolling(socket, subsocket, request) {
		subsocket.push('done');
		socket.close();
	}
	
    $(document).ready(function() {
    $("#cancel").hide();
	// Atmosphere stuff for auto refresh
	var socket = atmosphere;
	var subsocket;
	var atmosphereurl = window.location.pathname.substring(0, window.location.pathname.lastIndexOf("/")) +'/sqrlauthpolling';
    var request = { url: atmosphereurl,
            contentType: "application/json",
            logLevel: 'debug',
            transport: 'sse',
            reconnectInterval: 5000,
            fallbackTransport: 'long-polling'};

        request.onOpen = function (response) {
        	console.debug('Atmosphere connected using ' + response.transport );
        };
        
        request.onReconnect = function (request, response) {
            console.info('Atmosphere connection lost, trying to reconnect ' + request.reconnectInterval);
        };

        request.onReopen = function (response) {
        	console.info('Atmosphere re-connected using ' + response.transport );
        };

        request.onMessage = function (response) {
            status = response.responseBody;
			console.debug('received from server: ' + status);
			if (status.indexOf('ERROR_') > 0) {
				window.location.replace('login?error='+status);
			} else if(status == 'AUTH_COMPLETE') {
				subsocket.push('done');
				subsocket.close();
            	window.location.replace('sqrllogin');
			} else if(status == 'COMMUNICATING'){
				// The user scanned the QR code and sqrl auth is in progress
				subsocket.push('COMMUNICATING');
				sqrlInProgress();
			} else {
				console.error('recevied unknown state from server: ' + event.data);
			}
        };

        request.onClose = function (response) {
        	console.info('Server closed the connection after a timeout');
        };

        request.onError = function (response) {
            console.error('Error, there\'s some problem with your ' 
                + 'socket or the server is down');
        };
      
        subsocket = socket.subscribe(request);
        subsocket.push('<%=(String) request.getAttribute("sqrlstate")%>');
        
	});
	</script>
</html>