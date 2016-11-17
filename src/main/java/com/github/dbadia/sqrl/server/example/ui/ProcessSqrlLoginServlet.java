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
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;
import com.github.dbadia.sqrl.server.data.SqrlIdentity;
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.ErrorId;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.example.data.AppDatastore;
import com.github.dbadia.sqrl.server.example.data.AppUser;
import com.github.dbadia.sqrl.server.util.SqrlConfigHelper;
import com.github.dbadia.sqrl.server.util.SqrlException;
import com.github.dbadia.sqrl.server.util.SqrlUtil;

/**
 * Once SQRL auth is initiated, the browser polls the server to understand when SQRL auth is complete; once the browser
 * receives that message, it sends the user here so we can setup the app session based on the SQRL ID.
 * </p>
 * If this is the first time the user has authenticated via SQRL, the user will be sent to the linkaccountoption.jsp
 * where they can optionally link their SQRL account to an existing username/password account.
 * </p>
 * If the user has previously authenticated via SQRL, then the user is sent to the app page
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/sqrllogin" }, loadOnStartup = 1)
public class ProcessSqrlLoginServlet extends HttpServlet {
	private static final long serialVersionUID = 5609899766821704630L;

	private static final Logger			logger					= LoggerFactory
			.getLogger(ProcessSqrlLoginServlet.class);
	private final SqrlConfig			sqrlConfig				= SqrlConfigHelper.loadFromClasspath();
	private final SqrlServerOperations	sqrlServerOperations	= new SqrlServerOperations(sqrlConfig);

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
			final boolean requestContainsCorrelatorCookie = sqrlServerOperations
					.extractSqrlCorrelatorStringFromRequestCookie(request) != null;
			if (requestContainsCorrelatorCookie && checkForSqrlAuthComplete(request, response)) {
				// Nothing else to do, just fall through and return
			} else {
				RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
			}
		} catch (final Exception e) {
			logger.error("Error processing username/password login ", e);
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
		}
	}

	private boolean checkForSqrlAuthComplete(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException, SQLException, SqrlException {
		// Check for SQRL auth complete
		final SqrlCorrelator sqrlCorrelator = sqrlServerOperations.fetchSqrlCorrelator(request);
		sqrlServerOperations.cleanSqrlAuthData(request, response);
		if (sqrlCorrelator == null) {
			return false;
		}
		final SqrlAuthenticationStatus authStatus = sqrlCorrelator.getAuthenticationStatus();
		if (authStatus.isErrorStatus()) {
			// Error state
			sqrlServerOperations.deleteSqrlCorrelator(sqrlCorrelator);
			showLoginPage(request, response, "<font color='red'>SQRL protocol error: " + authStatus + "</font>");
			return true;
		} else if (SqrlAuthenticationStatus.AUTH_COMPLETE == authStatus) {
			final SqrlIdentity sqrlIdentity = sqrlCorrelator.getAuthenticatedIdentity();
			if (sqrlIdentity == null) {
				logger.warn("Correaltor status return AUTH_COMPLETE but user isn't authenticated");
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
			if (existingAppUser) {
				session.setAttribute(Constants.SESSION_NATIVE_APP_USER, nativeAppUser);
				sendUserToAppPage(response);
				return true;
			} else {
				// sqrlIdentity exists but NativeAppUser doesn't. Send them to enrollment page to see if they have a
				// user name and password or are completely new
				request.setAttribute(Constants.JSP_SUBTITLE, "Link SQRL to existing username?");
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
			final SqrlAuthPageData pageData = sqrlServerOperations.prepareSqrlAuthPageData(request, response,
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
			request.setAttribute("correlator", pageData.getCorrelator());
			logger.debug("Showing login page with correlator={}, sqrlurl={}", pageData.getCorrelator(),
					pageData.getUrl().toString());
			request.getRequestDispatcher("WEB-INF/login.jsp").forward(request, response);
		} catch (final SqrlException e) {
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
		}
	}
}
