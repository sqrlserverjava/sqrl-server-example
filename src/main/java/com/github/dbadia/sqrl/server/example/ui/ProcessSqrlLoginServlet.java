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
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.enums.SqrlAuthenticationStatus;
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.ErrorId;
import com.github.dbadia.sqrl.server.example.data.AppDatastore;
import com.github.dbadia.sqrl.server.example.data.AppUser;
import com.github.dbadia.sqrl.server.exception.SqrlException;
import com.github.dbadia.sqrl.server.persistence.SqrlCorrelator;
import com.github.dbadia.sqrl.server.persistence.SqrlIdentity;
import com.github.dbadia.sqrl.server.util.SqrlConfigHelper;
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
			// TODO: all requests that get here must contain a correlator, right? requestContainsCorrelatorCookie

			// Web polling SQRL auth (non-CPS)
			final boolean requestContainsCorrelatorCookie = sqrlServerOperations
					.extractSqrlCorrelatorStringFromRequestCookie(request) != null;
			if (requestContainsCorrelatorCookie && isSqrlWebRefreshAuthComplete(request, response)) {
				// Nothing else to do, just fall through and return
				// TODO: why fall through if we are using long polling now? Just throw error do same below for
				// return false
			} else {
				RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
			}
		} catch (final Exception e) {
			logger.error("Error processing username/password login ", e);
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
		}
	}

	private boolean isSqrlWebRefreshAuthComplete(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException, SQLException, SqrlException {
		final SqrlCorrelator sqrlCorrelator = sqrlServerOperations.fetchSqrlCorrelator(request);
		sqrlServerOperations.cleanSqrlAuthData(request, response);
		if (sqrlCorrelator == null) {
			return false;
		}
		final SqrlAuthenticationStatus authStatus = sqrlCorrelator.getAuthenticationStatus();
		if (authStatus.isUpdatesForThisCorrelatorComplete()) {
			// Now that we are done using the correlator, we can delete the correlator
			// note that If we didn't it would still get cleaned up later
			sqrlServerOperations.deleteSqrlCorrelator(sqrlCorrelator);
		}
		if (!authStatus.isHappyPath()) {
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
			return true;
		} else if (authStatus.isAuthComplete()) {
			return completeSqrlAuthentication(sqrlCorrelator, request, response);
		}
		return false;
	}

	private boolean completeSqrlAuthentication(final SqrlCorrelator sqrlCorrelator, final HttpServletRequest request,
			final HttpServletResponse response) throws SqrlException, ServletException, IOException {
		sqrlServerOperations.valiateCpsParamIfNecessary(sqrlCorrelator, request);
		final SqrlIdentity sqrlIdentity = sqrlCorrelator.getAuthenticatedIdentity();
		if (sqrlIdentity == null) {
			logger.warn("Correlator status return AUTH_COMPLETE but user isn't authenticated");
			return false;
		}
		final HttpSession session = request.getSession(true); // TODO: shoult thsi be true?
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


	private void sendUserToAppPage(final HttpServletResponse response) {
		response.setHeader("Location", "app");
		response.setStatus(302);
	}

}
