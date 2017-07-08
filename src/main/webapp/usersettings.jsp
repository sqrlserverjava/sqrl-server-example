<!DOCTYPE html>
<%@ page session="false" %>
<%@page import="com.github.sqrlserverjava.example.Constants"%>
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
			<p>User settings</p>
		</div>
		<div class="row">
			<div class="col-sm-4">
				<p>You have been authenticated but this is the first time you have logged in. </p>
				<p>Please enter your given name and a welcome phrase.  You will be greeted with this phrase each time you login</p>
				
			</div>
			<div class="col-sm-4">
				<form action="usersettings"  method="post">
				  Given name:  <br>
				  <input type="text" name="givenname" pattern="[a-zA-Z0-9]+"  maxlength="10" required><br>
				  Welcome phrase:  (letters and numbers only)<br>
				  <input type="text" name="phrase" pattern="[a-zA-Z0-9 ]+"  maxlength="40" required><br>
				  <input type="submit" value="Submit">
				</form>
				<br/>
				<br/>
				<p><a href="logout">Cancel</a><p/>
			</div>
		</div>
	</div>
</body>
</html>