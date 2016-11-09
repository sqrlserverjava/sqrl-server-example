package com.github.dbadia.sqrl.server.example.ui;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// TODO: remove
// @WebServlet(urlPatterns = { "/usersettingsrd" })
public class UserSettingsRdServlet extends HttpServlet {

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.getRequestDispatcher("usersettings.jsp").forward(request, response);
	}
}