<!DOCTYPE html>
<%@ page session="false"%>
<%@page import="com.github.sqrlserverjava.example.Constants"%>
<html lang="en">
<head>
<title>SQRL Java Server Demo</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="refresh" content="<%=request.getAttribute(com.github.sqrlserverjava.example.Constants.JSP_PAGE_REFRESH_SECONDS)%>">
<link rel="stylesheet"	href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" >
<!--[if lt IE 9]><script src="//ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script><![endif]-->
<!--[if IE 9]><!--><script src="//ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js"></script><!--<![endif]-->

<script	type="text/javascript" src="//cdnjs.cloudflare.com/ajax/libs/atmosphere/2.2.12/atmosphere.js"></script>
	<!-- Include javascript here for readability. Real apps would move it to the server -->
	<script>
	<%--  http://stackoverflow.com/a/11663507/2863942 --%>
	<%--  Avoid `console` errors in browsers that lack a console. --%>
	(function() {
	    var method;
	    var noop = function () {};
	    var methods = [
	        'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error',
	        'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log',
	        'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd',
	        'timeStamp', 'trace', 'warn'
	    ];
	    var length = methods.length;
	    var console = (window.console = window.console || {});
	
	    while (length--) {
	        method = methods[length];
	
	        <%-- Only stub undefined methods. --%>
	        if (!console[method]) {
	            console[method] = noop;
	        }
	    }
	}());
	
	var localhostRoot = 'http://localhost:25519/';	<%-- the SQRL client listener  --%>
	var cpsGifProbe = new Image(); 					<%-- create an instance of a memory-based probe image --%>
	
	<%-- Taken from https://www.grc.com/sqrl/demo/pagesync.js and renamed --%>
	cpsGifProbe.onload = function() {  <%-- if the SQRL localhost listener is present --%>
		document.location.href = localhostRoot + "<%=(String) request.getAttribute("sqrlurlwithcan64")%>";
	};

	<%-- Taken from https://www.grc.com/sqrl/demo/pagesync.js and renamed --%>
	cpsGifProbe.onerror = function() {
		setTimeout( function(){ cpsGifProbe.src = localhostRoot + Date.now() + '.gif';	}, 200 );
	}
	
	function sqrlInProgress() {
		var sqrlButtonSrc = $("#sqrlButton").attr("src");
		var showingSqrlQr = sqrlButtonSrc != "spinner.gif";
    	if(!showingSqrlQr) {
    		return;
    	}
    	$("#sqrlButton").attr("src", "spinner.gif");
    	$("#cancel").hide();
    	$("#uplogin").hide();
    	$("#or").hide();
    	$("#sqrlImg").hide();
        instruction.innerText = "Waiting for SQRL client";
		$("#cancel").show();
    	if(subtitle.innerText.indexOf("error") >=0 ) {
    		subtitle.innerText = "";
    	}
    	if(<%=(String) request.getAttribute("cpsEnabled")%>) {
    		cpsGifProbe.onerror();	<%-- try to connect to the SQRL client on localhost if possible (CPS) --%>
    	}
   		window.location.replace("<%=(String) request.getAttribute("sqrlurl")%>");
	}
	
	function stopPolling(socket, subsocket, request) {
		subsocket.push("done");
		socket.close();
	}
	
    $(document).ready(function() {
    $("#cancel").hide();
	<%-- Atmosphere stuff for auto refresh --%>
	var socket = atmosphere;
	var subsocket;
	var atmosphereurl = window.location.pathname.substring(0, window.location.pathname.lastIndexOf("/")) +"/sqrlauthpolling";
    var request = { url: atmosphereurl,
            contentType: "application/json",
            logLevel: "debug",
            transport: "sse",
            reconnectInterval: 5000,
            fallbackTransport: "long-polling"};

        request.onOpen = function (response) {
        	console.debug("Atmosphere connected using " + response.transport );
        };
        
        request.onReconnect = function (request, response) {
            console.info("Atmosphere connection lost, trying to reconnect " + request.reconnectInterval);
        };

        request.onReopen = function (response) {
        	console.info("Atmosphere re-connected using " + response.transport );
        };

        request.onMessage = function (response) {
        	var status = response.responseBody;
			console.info("received from server: " + status);
			if (status.indexOf("ERROR_") > -1) {
				subsocket.close();
				window.location.replace("login?error="+status);
			} else if (status.indexOf("AUTHENTICATED_CPS") > -1) {
				<%-- Stop polling and wait for the SQRL client to provide the URL	--%>	
				subsocket.close();
			} else if(status == "AUTHENTICATED_BROWSER") {
				subsocket.push(atmosphere.util.stringifyJSON({ state: "AUTHENTICATED_BROWSER" }));
				subsocket.close();
				window.location.replace("sqrllogin");
			} else if(status == "COMMUNICATING"){
				<%-- The user scanned the QR code and sqrl auth is in progress --%>
				instruction.innerText = "Communicating with SQRL client";
				subsocket.push(atmosphere.util.stringifyJSON({ state: "COMMUNICATING" }));
				sqrlInProgress();
			} else {
				console.error("received unknown state from server: " + status);
				subsocket.close();
				window.location.replace("login?error=ERROR_SQRL_INTERNAL");
			}
        };

        request.onClose = function (response) {
        	console.info("Server closed the connection after a timeout");
        };

        request.onError = function (response) {
            console.error("Error, there\'s some problem with your " 
                + "socket or the server is down");
        };
      
        subsocket = socket.subscribe(request);
        subsocket.push(atmosphere.util.stringifyJSON({ state: "CORRELATOR_ISSUED" }));
	});
	</script>
</head>
<body>

	<div class="container">
		<div class="jumbotron">
			<h1>SQRL Java Server Demo</h1>
			<p id="subtitle"><%=request.getAttribute(com.github.sqrlserverjava.example.Constants.JSP_SUBTITLE)%></p>
		</div>
		<div>
		  <div class="row vcenter">
			<h4 id="instruction"> </h4>
			<div class="col-sm-3 vcenter" >
				<!--  <img id="sqrlButton" onclick="sqrlInProgress()" src="signInSqrl.png" /> -->  
				<a href="<%=(String) request.getAttribute("sqrlurl")%>"  onclick="sqrlInProgress();return false;" >
					<img id="sqrlButton" src="signInSqrl.png" alt="Click to sign in with SQRL" /></a>
				<br/>
				<a id="cancel"  href="logout">Cancel SQRL authentication</a>
				<br/>
			</div>
			<div class="col-sm-4 vcenter" >
-               <img id="sqrlImg"
-                     src="<%=(String) request.getAttribute("sqrlqr64")%>"
-                     alt="<%=(String) request.getAttribute("sqrlqrdesc")%>" />
			</div>
		  </div>
		  <div id="or" >
		  		<p/>
				<h4 >OR</h4>
		  </div>
		  <div id="uplogin" >
			  <div>
				<p>
					Username: (alphanumeric)<br>
				</p><form action="auth" method="post">  <!-- TODO; what is this background-image -->
					<!--  <input name="username" pattern="[a-zA-Z0-9]+" maxlength="10" required="" style="background-image: url(&quot;data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAASCAYAAABSO15qAAAAAXNSR0IArs4c6QAAAPhJREFUOBHlU70KgzAQPlMhEvoQTg6OPoOjT+JWOnRqkUKHgqWP4OQbOPokTk6OTkVULNSLVc62oJmbIdzd95NcuGjX2/3YVI/Ts+t0WLE2ut5xsQ0O+90F6UxFjAI8qNcEGONia08e6MNONYwCS7EQAizLmtGUDEzTBNd1fxsYhjEBnHPQNG3KKTYV34F8ec/zwHEciOMYyrIE3/ehKAqIoggo9inGXKmFXwbyBkmSQJqmUNe15IRhCG3byphitm1/eUzDM4qR0TTNjEixGdAnSi3keS5vSk2UDKqqgizLqB4YzvassiKhGtZ/jDMtLOnHz7TE+yf8BaDZXA509yeBAAAAAElFTkSuQmCC&quot;); background-repeat: no-repeat; background-attachment: scroll; background-size: 16px 18px; background-position: 98% 50%;" autocomplete="off" type="text">-->
					<input name="username" pattern="[a-zA-Z0-9]+" maxlength="10" required="" autocomplete="off" type="text">
					<br> Password:<br> 
					<input name="password" pattern="[a-zA-Z]+" value="sqrl" maxlength="10" required=""  autocomplete="off" type="password">
					<br> 
					<input name="type" value="up" type="hidden"><br> <input value="Submit" type="submit">
				</form>
				<br> <br>
				<p>
					Note: password is "sqrl" for all users.<br>
				</p><p>You can create a new user with either authentication method</p>
				<br>
		    </div>				
		  </div>
		</div>
	</div>
</body>
</html>