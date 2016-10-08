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
</head>
<body>

	<div class="container">
		<div class="jumbotron">
			<h1>SQRL Java Server Demo</h1>
			<p><%=request.getAttribute(com.github.dbadia.sqrl.server.example.Constants.JSP_SUBTITLE)%></p>
		</div>
		<div class="row">
			<div class="col-sm-6">
				<p>You have been authenticated via SQRL. </p>
				<p>If you have an existing username and password for this site that you would like to link your SQRL ID to, then login. <br/></p>
				<p><a href="usersettingsrd">I don't have a username/password.  This is my first time logging in to this site</a><p/>				
			</div>
			<div class="col-sm-6">
				<p>Username: (alphanumeric)<br/>
				<form action="linkaccount"  method="post">
				  <input type="text" name="username" pattern="[a-zA-Z0-9]+"  maxlength="10" required><br>
				  Password:<br>
				  <input type="password" name="password" pattern="[a-zA-Z]+" value="sqrl" maxlength="10" required><br><br>
				  <input type="hidden" name="type" value="up"><br>
				  <input type="submit" value="Link Account">
				 </form>
				  <br/>
				  <br/>
				  <p>Note: password is "sqrl" for all users.  You can create a new user with either authentication method (SQRL or username/password)</p>

			</div>
		</div>
	</div>
</body>
</html>