package com.github.dbadia.sqrl.server.example.ui;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfigHelper;
import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.ErrorId;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.example.data.AppUser;

@WebServlet(urlPatterns = { "/app" })
public class AppPageServlet extends HttpServlet {
	private static final long	serialVersionUID	= 2614632407392158693L;
	private static final Logger	logger				= LoggerFactory.getLogger(AppPageServlet.class);

	private final SqrlServerOperations sqrlServerOperations = new SqrlServerOperations(
			SqrlConfigHelper.loadFromClasspath());

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		AppUser user = null;
		if (request.getSession(false) != null) {
			user = (AppUser) request.getSession(false).getAttribute(Constants.SESSION_NATIVE_APP_USER);
		}
		if (user == null) {
			logger.error("user is not in session, redirecting to login page");
			LoginPageServlet.redirectToLoginPageWithError(response, ErrorId.ATTRIBUTES_NOT_FOUND);
			return;
		}

		if (Util.isBlank(user.getGivenName()) || Util.isBlank(user.getWelcomePhrase())) {
			// New user got here by accident, send them to the new user enroll page
			// request.getRequestDispatcher("WEB-INF/usersettings.jsp").forward(request, response);
			// TODO: do we need the logic above?

			// sqrlIdentity exists but NativeAppUser doesn't. Send them to enrollment page to see if they have a
			// user name and password or are completely new
			logger.error("first time SQRL user, redirecting to account link page");
			request.getRequestDispatcher("WEB-INF/linkaccountoption.jsp").forward(request, response);
			return;
		}

		String accountType = "Username/password only";
		if (user.getUsername() == null) {
			accountType = "SQRL only";
		} else if (sqrlServerOperations.fetchSqrlIdentityByUserXref(Long.toString(user.getId())) != null) {
			accountType = "Both SQRL and username/password";
		}
		final HttpSession session = request.getSession(false);
		session.setAttribute("givenname", user.getGivenName());
		session.setAttribute("phrase", user.getWelcomePhrase());
		session.setAttribute("accounttype", accountType);
		request.getRequestDispatcher("WEB-INF/app.jsp").forward(request, response);
	}

}
