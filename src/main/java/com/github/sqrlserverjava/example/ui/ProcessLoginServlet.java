package com.github.sqrlserverjava.example.ui;

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

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.example.Constants;
import com.github.sqrlserverjava.example.ErrorId;
import com.github.sqrlserverjava.example.Util;
import com.github.sqrlserverjava.example.data.AppDatastore;
import com.github.sqrlserverjava.example.data.AppUser;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.util.SqrlConfigHelper;
import com.github.sqrlserverjava.util.SqrlUtil;

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
		logger.info(SqrlUtil.logEnterServlet(request));
		// Set X-UA-Compatible for bootstrap IE compatibility:
		// http://v4-alpha.getbootstrap.com/getting-started/browsers-devices/#ie-compatibility-modes -->
		// http://stackoverflow.com/questions/11095319/how-to-fix-document-mode-restart-in-ie-9
		response.setHeader("X-UA-Compatible", "IE=edge");

		// Even though we aren't using SQRL auth, we should still cleanup the data
		sqrlServerOperations.cleanSqrlAuthData(request, response);

		try {
			handleUsernamePasswordAuthentication(request, response);
		} catch (final RuntimeException | SqrlException | SQLException e) {
			logger.error("Error processing login", e);
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
