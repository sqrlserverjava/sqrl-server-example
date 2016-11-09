package com.github.dbadia.sqrl.server.example.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlAuthPageData;
import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;
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
 * Servlet which is called when the broser transmits the username and password for authentication
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/auth" })
public class ProcessLoginServlet extends HttpServlet {
	private static final long serialVersionUID = -7055677482775924249L;
	private static final Logger logger = LoggerFactory.getLogger(ProcessLoginServlet.class);
	private final SqrlConfig sqrlConfig = SqrlConfigHelper.loadFromClasspath();
	private final SqrlServerOperations sqrlServerOperations = new SqrlServerOperations(sqrlConfig);


	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		if (logger.isInfoEnabled()) {
			logger.info("In do post for /auth with params: {}.  cookies: {}", request.getParameterMap(),
					SqrlUtil.cookiesToString(request.getCookies()));
		}
		try {
			handleUsernamePasswordAuthentication(request, response);
		} catch (final Exception e) {
			throw new ServletException("Error rendering login page", e);
		}
	}

	private void sendUserToAppPage(final HttpServletResponse response) {
		response.setHeader("Location", "app");
		response.setStatus(302);
	}

	private void handleUsernamePasswordAuthentication(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException, IOException, SqrlException, SQLException {
		// username / password auth?
		if (Util.isBlank(request.getParameter("username")) && Util.isBlank(request.getParameter("password"))) {
			showLoginPage(request, response, "<font color='red'>System error: missing parameter</font>");
			return;
		}
		// Check for login credentials
		final String username = Util.sanitizeString(request.getParameter("username"), Constants.MAX_LENGTH_GIVEN_NAME);
		final String password = Util.sanitizeString(request.getParameter("password"), Constants.MAX_LENGTH_GIVEN_NAME);

		if (!password.equals(Constants.PASSWORD_FOR_ALL_USERS)) {
			redirectToLoginPageWithError(response, ErrorId.INVALID_USERNAME_OR_PASSWORD);
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

	void showLoginPage(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException, SqrlException {
		showLoginPage(request, response, null);
	}

	void showLoginPage(final HttpServletRequest request, final HttpServletResponse response, final String subtitle)
			throws ServletException, IOException {
		if (Util.isBlank(subtitle)) {
			request.setAttribute(Constants.JSP_SUBTITLE, "Login Page");
		} else {
			request.setAttribute(Constants.JSP_SUBTITLE, subtitle);
		}
		// Default action, show the login page with a new SQRL QR code
		try {
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
		} catch (final SqrlException e) {
			redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
		}
	}

	public static void redirectToLoginPageWithError(final HttpServletResponse response, final ErrorId errorId) {
		response.setHeader("Location", "login?error=" + errorId.getId());
		response.setStatus(302);
	}

}
