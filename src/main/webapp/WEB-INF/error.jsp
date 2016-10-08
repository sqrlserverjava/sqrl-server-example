<!DOCTYPE html>
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
<%
	session.invalidate();
%>

	<div class="container">
		<div class="jumbotron">
			<h1>SQRL Java Server Demo</h1>
		</div>
		<div class="row">
			<div class="col-sm-4">
				<h3>System error </h3>
				<p>Oops, an error has occurred...</p>
				<p><a href="login">Return to the login page</a><p/>
			</div>
		</div>
	</div>
</body>
</html>