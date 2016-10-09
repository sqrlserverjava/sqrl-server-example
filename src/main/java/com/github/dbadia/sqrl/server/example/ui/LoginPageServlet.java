package com.github.dbadia.sqrl.server.example.ui;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Base64;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlAuthPageData;
import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;
import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlException;
import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;
import com.github.dbadia.sqrl.server.data.SqrlIdentity;
import com.github.dbadia.sqrl.server.data.SqrlPersistenceException;
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.ErrorId;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.example.data.AppDatastore;
import com.github.dbadia.sqrl.server.example.data.AppUser;
import com.github.dbadia.sqrl.server.example.sqrl.SqrlSettings;

/**
 * Servlet which is called during various login actions.
 *
 * Specifically:
 * <li>1) to present the login page (with SQRL option)
 * <li>2) when a user logs in via username password &type=up
 * <li>3) when the SQRL state changes, ie SQRL auth in progress or SQRL auth complete (see {@link PageSyncJsServlet}
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/login" }, loadOnStartup = 1)
public class LoginPageServlet extends HttpServlet {
	private static final long serialVersionUID = 5609899766821704630L;

	private static final Logger logger = LoggerFactory.getLogger(LoginPageServlet.class);
	private final SqrlConfig sqrlConfig = SqrlSettings.getSqrlConfig();
	private final SqrlServerOperations sqrlServerOperations = new SqrlServerOperations(sqrlConfig);

	private String spinnerB64Cache = null;

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		if (logger.isInfoEnabled()) {
			final StringBuilder buf = new StringBuilder();
			if (request.getCookies() != null) { // TODO: move to util cookies to string
				for (final Cookie cookie : request.getCookies()) {
					buf.append(cookie.getName()).append("=").append(cookie.getValue()).append("  ");
				}
			}
			logger.info("In do post for /login with params: {}.  cookies: {}", request.getParameterMap(),
					buf.toString());
		}
		try {
			final HttpSession session = request.getSession(false);
			AppUser appUser = null;
			if (session != null) {
				appUser = (AppUser) session.getAttribute(Constants.SESSION_NATIVE_APP_USER);
			}
			final boolean requestContainsCorrelatorCookie = sqrlServerOperations
					.extractSqrlCorrelatorStringFromRequestCookie(request) != null;
			if (displayingErrorMessage(request, response, session)) {
				// Nothing else to do, just fall through and return
			} else if ("up".equals(request.getParameter("type"))) {
				handleUsernamePasswordAuthentication(request, response);
			} else if (appUser != null) {
				// Is the user logged in but got here by mistake? If so, send them to the app page
				sendUserToAppPage(response);
			} else if (requestContainsCorrelatorCookie && checkForSqrlAuthComplete(request, response)) { // TODO: remove
				// Nothing else to do, just fall through and return
			} else if (requestContainsCorrelatorCookie && checkForSqrlAuthInProgress(request, response)) { // TODO:
																											// remove
				// Nothing else to do, just fall through and return
			} else {
				showLoginPage(request, response);
			}
		} catch (final Exception e) {
			throw new ServletException("Error rendering login page", e);
		}
	}

	private boolean checkForSqrlAuthInProgress(final HttpServletRequest request, final HttpServletResponse response)
			throws SqrlPersistenceException, IOException, ServletException {
		// Check for SQRL auth in progress so we can show spinner
		final SqrlCorrelator sqrlCorrelator = sqrlServerOperations.fetchSqrlCorrelator(request);
		if (sqrlCorrelator != null
				&& sqrlCorrelator.getAuthenticationStatus() == SqrlAuthenticationStatus.COMMUNICATING) {
			// Authentication is in progress since we've received a SQRL client request with the SQRL nut token
			// we embedded in the QR code/SQRL url. Show the spinner
			final String b64 = new StringBuilder("data:image/gif;base64, ").append(getSpinner()).toString();
			request.setAttribute(Constants.JSP_SUBTITLE, "SQRL authetication in progress...");
			request.setAttribute("sqrlqr64", b64);
			request.setAttribute("sqrlurl", "login");
			request.setAttribute("sqrlqrdesc", "SQRL authetication in progress...");
			request.setAttribute("sqrlstate", SqrlAuthenticationStatus.COMMUNICATING.toString());
			request.setAttribute("correlator", sqrlCorrelator.getCorrelatorString());
			request.getRequestDispatcher("WEB-INF/login.jsp").forward(request, response);
			return true;
		} else {
			return false;
		}
	}

	private boolean checkForSqrlAuthComplete(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException, SQLException, SqrlException {
		// Check for SQRL auth complete
		final SqrlCorrelator sqrlCorrelator = sqrlServerOperations.fetchSqrlCorrelator(request);
		if (sqrlCorrelator == null) {
			return false;
		}
		final SqrlAuthenticationStatus authStatus = sqrlCorrelator.getAuthenticationStatus();
		if (authStatus.isErrorStatus()) {
			// Error state
			showLoginPage(request, response, "<font color='red'>SQRL protocol error: " + authStatus + "</font>");
			return true;
		} else if (SqrlAuthenticationStatus.AUTH_COMPLETE == authStatus) {
			final SqrlIdentity sqrlIdentity = sqrlCorrelator.getAuthenticatedIdentity();
			if (sqrlIdentity == null) {
				// The cookie status was AUTH_COMPLETE but the user isn't authenticated
				return false;
			}
			final HttpSession session = request.getSession(true);
			session.setAttribute(Constants.SESSION_SQRL_IDENTITY, sqrlIdentity);
			// The user has been SQRL authenticated, is this an existing app user?
			final String nativeUserXref = sqrlIdentity.getNativeUserXref();
			AppUser nativeAppUser = null;
			if (nativeUserXref != null) {
				nativeAppUser = AppDatastore.getInstance().fetchUserById(Long.parseLong(nativeUserXref));
			}
			final boolean existingAppUser = nativeAppUser != null;
			// Clear our SQRL cookies and the correaltor since auth is complete
			sqrlServerOperations.deleteSqrlAuthCookies(request, response);
			sqrlServerOperations.deleteSqrlCorrelator(sqrlCorrelator);
			// Page sync cookie was specific to the example app, so delete it now
			Util.deleteCookies(request, response, Constants.COOKIE_PREVIOUS_PAGE_SYNC_PREVIOUS_STATUS);
			if (existingAppUser) {
				session.setAttribute(Constants.SESSION_NATIVE_APP_USER, nativeAppUser);
				sendUserToAppPage(response);
				return true;
			} else {
				// sqrlIdentity exists but NativeAppUser doesn't. Send them to enrollment page to see if they have a
				// user name and password or are completely new
				request.getRequestDispatcher("WEB-INF/linkaccountoption.jsp").forward(request, response);
				return true;
			}
		}
		return false;
	}

	private void sendUserToAppPage(final HttpServletResponse response) {
		response.setHeader("Location", "app");
		response.setStatus(302);
	}

	private void handleUsernamePasswordAuthentication(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException, IOException, SqrlException, SQLException {
		// username / password auth?
		if ("up".equals(request.getParameter("type"))) {
			if (Util.isBlank(request.getParameter("username")) && Util.isBlank(request.getParameter("password"))) {
				showLoginPage(request, response, "<font color='red'>System error: missing parameter</font>");
				return;
			}
			// Check for login credentials
			final String username = Util.sanitizeString(request.getParameter("username"),
					Constants.MAX_LENGTH_GIVEN_NAME);
			final String password = Util.sanitizeString(request.getParameter("password"),
					Constants.MAX_LENGTH_GIVEN_NAME);

			if (!password.equals(Constants.PASSWORD_FOR_ALL_USERS)) {
				showLoginPage(request, response, "Invalid password");
				return;
			}
			AppUser user = AppDatastore.getInstance().fetchUserByUsername(username);
			final HttpSession session = request.getSession(true);
			if (user == null) {
				// This is a new user, create the user object, then send them to the enrollment page
				user = new AppUser(username);
				AppDatastore.getInstance().createUser(user);
				session.setAttribute(Constants.SESSION_NATIVE_APP_USER, user);
				request.getRequestDispatcher("WEB-INF/usersettings.jsp").forward(request, response);
				return;
			} else {
				session.setAttribute(Constants.SESSION_NATIVE_APP_USER, user);
				sendUserToAppPage(response);
			}
		}
	}

	private boolean displayingErrorMessage(final HttpServletRequest request, final HttpServletResponse response,
			final HttpSession session) throws ServletException, IOException, SqrlException {
		final String errorParam = request.getParameter("error");
		if(Util.isBlank(errorParam)) {
			return false;
		}
		String errorMessage = null;
		if(errorParam.startsWith("ERROR_")) {
			try {
				errorMessage = ErrorId.valueOf(errorParam).getErrorMessage();
			} catch (final IllegalArgumentException e) {
				logger.error("Error translating errorParam '{}' to ErrorId", errorParam, e);
				errorMessage = ErrorId.GENERIC.getErrorMessage();
			}
		} else {
			errorMessage = ErrorId.byId(Integer.parseInt(errorParam)).getErrorMessage();
		}
		final StringBuilder buf = new StringBuilder("An error occurred").append(errorMessage).append(".");
		// If we have access to the correlator, append the first 5 chars to the message in case it gets reported
		final String correlatorString = sqrlServerOperations.extractSqrlCorrelatorStringFromRequestCookie(request);
		if(Util.isNotBlank(correlatorString)) {
			buf.append("    code="+correlatorString.substring(0, 5));
		}

		// Since we are in an error state, kill the session
		if(session != null) {
			session.invalidate();
		}
		// Show a new login page with the error message
		showLoginPage(request, response, Util.wrapErrorInRed(buf.toString()));
		return true;
	}

	void showLoginPage(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException, SqrlException {
		showLoginPage(request, response, null);
	}

	void showLoginPage(final HttpServletRequest request, final HttpServletResponse response, final String subtitle)
			throws ServletException, IOException, SqrlException {
		if (Util.isBlank(subtitle)) {
			request.setAttribute(Constants.JSP_SUBTITLE, "Login Page");
		} else {
			request.setAttribute(Constants.JSP_SUBTITLE, subtitle);
		}
		// Default action, show the login page with a new SQRL QR code
		final SqrlAuthPageData pageData = sqrlServerOperations.buildQrCodeForAuthPage(request, response,
				InetAddress.getByName(request.getRemoteAddr()), 250);
		final ByteArrayOutputStream baos = pageData.getQrCodeOutputStream();
		baos.flush();
		final byte[] imageInByteArray = baos.toByteArray();
		baos.close();
		// Since this is being passed to the browser, we use regular Base64 encoding, NOT SQRL specific
		// Base64URL encoding
		final String b64 = new StringBuilder("data:image/").append(pageData.getHtmlFileType(sqrlConfig))
				.append(";base64, ").append(Base64.getEncoder().encodeToString(imageInByteArray)).toString();
		request.setAttribute("sqrlqr64", b64);
		request.setAttribute("sqrlurl", pageData.getUrl().toString());
		request.setAttribute("sqrlqrdesc", "Click or scan to login with SQRL");
		request.setAttribute("sqrlstate", SqrlAuthenticationStatus.CORRELATOR_ISSUED.toString());
		request.setAttribute("correlator", pageData.getCorrelator());
		logger.debug("Showing login page with correlator={}, sqrlurl={}", pageData.getCorrelator(),
				pageData.getUrl().toString());
		request.getRequestDispatcher("WEB-INF/login.jsp").forward(request, response);
	}

	public static void redirectToLoginPageWithError(final HttpServletResponse response,
			final ErrorId errorId) {
		response.setHeader("Location", "login?error="+errorId.getId());
		response.setStatus(302);
	}

	private String getSpinner() throws IOException {
		if (spinnerB64Cache == null) {
			try (final InputStream is = new BufferedInputStream(
					getClass().getClassLoader().getResourceAsStream("spinner.gif"))) {
				final byte[] bytes = new byte[is.available()];
				is.read(bytes);
				// Since this is being passed to the browser, we use regular Base64 encoding, NOT SQRL specific
				spinnerB64Cache = Base64.getEncoder().encodeToString(bytes);
			}
		}
		return spinnerB64Cache;
	}

	@Override
	public void init(final ServletConfig servletConfig) throws ServletException {
		super.init();
	}
}
