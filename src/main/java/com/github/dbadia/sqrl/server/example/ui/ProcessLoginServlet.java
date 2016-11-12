package com.github.dbadia.sqrl.server.example.ui;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.ErrorId;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.example.data.AppDatastore;
import com.github.dbadia.sqrl.server.example.data.AppUser;
import com.github.dbadia.sqrl.server.util.SqrlConfigHelper;
import com.github.dbadia.sqrl.server.util.SqrlException;
import com.github.dbadia.sqrl.server.util.SqrlUtil;

/**
 * Servlet which is called when the browser submits the username and password for user authentication
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/auth" })
public class ProcessLoginServlet extends HttpServlet {
	private static final long serialVersionUID = 3182250009216737995L;

	private static final Logger			logger					= LoggerFactory.getLogger(ProcessLoginServlet.class);
	private final SqrlConfig			sqrlConfig				= SqrlConfigHelper.loadFromClasspath();
	private final SqrlServerOperations	sqrlServerOperations	= new SqrlServerOperations(sqrlConfig);

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: move this whole logic to util, pass string
		if (logger.isInfoEnabled()) {
			logger.info("In do post for /auth with params: {}.  cookies: {}", request.getParameterMap(), // TODO: add
					// method to
					// show map
					// ropertly
					// and add
					// to others
					SqrlUtil.cookiesToString(request.getCookies()));
		}
		// Even though we aren't using SQRL auth, we should still cleanup the data
		sqrlServerOperations.cleanSqrlAuthData(request, response);

		try {
			handleUsernamePasswordAuthentication(request, response);
		} catch (final Exception e) {
			logger.error("Error rendering login page", e);
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
		}
	}

	private void sendUserToAppPage(final HttpServletResponse response) {
		response.setHeader("Location", "app");
		response.setStatus(302);
	}

	private void handleUsernamePasswordAuthentication(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException, IOException, SqrlException, SQLException {
		if (Util.isBlank(request.getParameter("username")) && Util.isBlank(request.getParameter("password"))) {
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.ERROR_BAD_REQUEST);
			return;
		}
		// Check for login credentials
		final String username = Util.sanitizeString(request.getParameter("username"), Constants.MAX_LENGTH_GIVEN_NAME);
		final String password = Util.sanitizeString(request.getParameter("password"), Constants.MAX_LENGTH_GIVEN_NAME);

		if (!password.equals(Constants.PASSWORD_FOR_ALL_USERS)) {
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.INVALID_USERNAME_OR_PASSWORD);
			return;
		}
		AppUser user = AppDatastore.getInstance().fetchUserByUsername(username);
		final HttpSession session = request.getSession(true);
		if (user == null) {
			// This is a new user, create the user object, then send them to the enrollment page
			user = new AppUser(username);
			AppDatastore.getInstance().createUser(user);
			session.setAttribute(Constants.SESSION_NATIVE_APP_USER, user);
			request.getRequestDispatcher("usersettings.jsp").forward(request, response);
			return;
		} else {
			session.setAttribute(Constants.SESSION_NATIVE_APP_USER, user);
			sendUserToAppPage(response);
		}
	}

}
