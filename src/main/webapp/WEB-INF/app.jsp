<!DOCTYPE html>
<%@page import="com.github.sqrlserverjava.example.Constants"%>
<html lang="en">
<head>
<title>SQRL Java Server Demo - build #<%=(String) request.getAttribute("build")%></title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"
	href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css">
</head>
<body>
<%
	if(session.getAttribute("phrase") == null || session.getAttribute("givenname") == null) {
		request.getRequestDispatcher("login?error=0").forward(request, response);
		return;
	}
%>

	<div class="container">
		<div class="jumbotron">
			<h1>SQRL Java Server Demo</h1>
			<p>You are logged in</p>
			
		</div>
		<div class="row">
			<div class="col-sm-4">
				<h3>Hello <%=(String) session.getAttribute("givenname")%>
				</h3>
				<p>Your welcome phrase is:  <%=(String) session.getAttribute("phrase")%></p>
				<br/>
				<p>Account type:  <%=(String) session.getAttribute("accounttype")%></p>
				<br/>
				<p><a href="logout">Logout</a></p>
			</div>
		</div>
	</div>
</body>
</html>