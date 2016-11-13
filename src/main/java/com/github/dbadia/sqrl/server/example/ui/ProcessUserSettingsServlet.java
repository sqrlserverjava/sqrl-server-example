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

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.data.SqrlIdentity;
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.ErrorId;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.example.data.AppDatastore;
import com.github.dbadia.sqrl.server.example.data.AppUser;
import com.github.dbadia.sqrl.server.util.SqrlConfigHelper;
import com.github.dbadia.sqrl.server.util.SqrlUtil;

@WebServlet(urlPatterns = { "/usersettings" })
public class ProcessUserSettingsServlet extends HttpServlet {
	private static final long			serialVersionUID		= 7534356830225738651L;
	private static final Logger			logger					= LoggerFactory
			.getLogger(ProcessUserSettingsServlet.class);
	private final SqrlServerOperations	sqrlServerOperations	= new SqrlServerOperations(
			SqrlConfigHelper.loadFromClasspath());

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		logger.info(SqrlUtil.logEnterServlet(request));
		try {
			final HttpSession session = request.getSession();
			final String givenName = Util.sanitizeString(request.getParameter("givenname"),
					Constants.MAX_LENGTH_GIVEN_NAME);
			final String welcomePhrase = Util.sanitizeString(request.getParameter("phrase"),
					Constants.MAX_LENGTH_WELCOME_PHRASE);

			if (Util.isBlank(givenName) || Util.isBlank(welcomePhrase)) {
				RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.MISSING_PARAM_FOR_NEW_USER);
				return;
			}

			// appUser and sqrlIdentity may or may not exist yet depending on how the user authenticated
			AppUser appUser = (AppUser) session.getAttribute(Constants.SESSION_NATIVE_APP_USER);
			final SqrlIdentity sqrlIdentity = (SqrlIdentity) session.getAttribute(Constants.SESSION_SQRL_IDENTITY);

			if (appUser == null && sqrlIdentity == null) {
				RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.INVALID_USERNAME_OR_PASSWORD);
				return;
			} else if (sqrlIdentity != null && appUser == null) {
				appUser = enrollSqrlOnlyUser(sqrlIdentity, givenName, welcomePhrase);
				session.setAttribute(Constants.SESSION_NATIVE_APP_USER, appUser);
			} else {
				if (sqrlIdentity != null && appUser != null) {
					logger.warn("Both sqrlIdentity and appUser are non null for enrollment");
				}
				enrollUsernameOnlyUserOrModify(appUser, givenName, welcomePhrase);
			}
		} catch (final Exception e) {
			logger.error("Error processing user settings", e);
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
		}
		// Send them to the app screen
		response.setHeader("Location", "app");
		response.setStatus(302);
	}

	private void enrollUsernameOnlyUserOrModify(final AppUser appUser, final String givenName,
			final String welcomePhrase) throws SQLException {
		// Username / password only user: sqrlIdentity == null && appUser != null
		// OR both sqrlIdentity and authUser exist, which shouldn't happen

		appUser.setGivenName(givenName);
		appUser.setWelcomePhrase(welcomePhrase);
		AppDatastore.getInstance().updateUser(appUser);
	}

	private AppUser enrollSqrlOnlyUser(final SqrlIdentity sqrlIdentity, final String givenName,
			final String welcomePhrase) throws SQLException {
		// This is a SQRL only user so we will create a new app user with a null username (since there
		// is no username/password authentication)
		final AppUser appUser = new AppUser(givenName, welcomePhrase);
		AppDatastore.getInstance().createUser(appUser);

		// Link our sqrlIdentity to the new Appuser
		sqrlServerOperations.updateNativeUserXref(sqrlIdentity, Long.toString(appUser.getId()));

		return appUser;
	}
}