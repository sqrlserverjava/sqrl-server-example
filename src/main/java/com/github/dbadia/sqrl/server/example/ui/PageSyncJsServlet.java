package com.github.dbadia.sqrl.server.example.ui;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;
import com.github.dbadia.sqrl.server.SqrlException;
import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.data.SqrlCorrelator;
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.example.sqrl.SqrlSettings;

/**
 * Invoked by the pagesync.js script running on the users browser. This script will typically return the same value over
 * and over until a state change occurs. When the pagesync.js script sees the value change, it will refresh the page so
 * the new state can be shown
 *
 * These values returned by this servlet (no, inprogress, sqrlauth) have NO actual meaning to the auth page; but anytime
 * the value changes, the page will refresh to update it's state
 *
 * @author Dave Badia
 *
 */
// TODO: remove this since it is no longer in use
@WebServlet(urlPatterns = { "/sqrlauto" })
public class PageSyncJsServlet extends HttpServlet {
	enum SqrlLoginPageStatus {
		NOT_AUTHENTICATED, SQRL_IN_PROGRESS, CLOSE_TO_SESSION_TIMEOUT, SQRL_AUTHENTICATED, SQRL_AUTHENTICATED_ALT
	}

	/**
	 * We generate a new SQRL QR login code when the Nut token expiry time drops below this value
	 *
	 * @see #checkForCloseToNutExpiry(HttpSession)
	 */
	private static final long QR_REFERSH_THRESHOLD = TimeUnit.MINUTES.toMillis(5);

	private static final Logger			logger					= LoggerFactory.getLogger(PageSyncJsServlet.class);
	private final SqrlServerOperations	sqrlServerOperations	= new SqrlServerOperations(
			SqrlSettings.getSqrlConfig());

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		SqrlLoginPageStatus newStatus = null;
		String toSend = "";

		try {
			SqrlLoginPageStatus previousStatus = null;
			final String previousStateValue = Util.getCookieValue(request,
					Constants.COOKIE_PREVIOUS_PAGE_SYNC_PREVIOUS_STATUS);
			if (previousStateValue != null) {
				previousStatus = SqrlLoginPageStatus.valueOf(previousStateValue);
			}
			newStatus = checkForAuthenticatedButNeedsRefresh(previousStatus);
			if (newStatus == null) {
				newStatus = checkForCloseToNutExpiry(request, response);
			}
			final SqrlCorrelator sqrlCorrelator = sqrlServerOperations.fetchSqrlCorrelator(request);
			if (newStatus == null) {
				if (sqrlCorrelator.getAuthenticationStatus() == SqrlAuthenticationStatus.AUTH_COMPLETE) {
					// The user just got authenticated
					newStatus = SqrlLoginPageStatus.SQRL_AUTHENTICATED;
				} else if (sqrlCorrelator.getAuthenticationStatus() != SqrlAuthenticationStatus.COMMUNICATING) {
					// The SQRL client has contacted us
					newStatus = SqrlLoginPageStatus.SQRL_IN_PROGRESS;
				} else {
					newStatus = SqrlLoginPageStatus.NOT_AUTHENTICATED;
				}
			}
			if (newStatus != SqrlLoginPageStatus.CLOSE_TO_SESSION_TIMEOUT) {
				Util.createCookie(request, Constants.COOKIE_PREVIOUS_PAGE_SYNC_PREVIOUS_STATUS, newStatus.toString());
			}

			if (previousStatus != newStatus) {
				logger.trace("PageSyncJsServlet  correlator={}, previousStatus={}, newStatus={}",
						sqrlCorrelator.getCorrelatorString(), previousStatus, newStatus);
			}
			toSend = newStatus.toString();
		} catch (final Exception e) {
			logger.error("Caught error in PageSyncJsServlet", e);
		}
		response.setHeader("Content-Type", "text/plain");
		try (Writer writer = response.getWriter()) {
			writer.write(toSend);
			writer.close();
		}
	}

	/**
	 * As with any new technology, correct functionality is a key to adoption. If a user tries SQRL for the first time,
	 * but it doesn't work, there is a likelyhood they will never try it again.
	 *
	 * Being that SQRL is a new technology, there is a good chance the user may be seeing it for the first time. They
	 * may see the SQRL QR code, go read about SQRL and install a client. By the time they get back to the login page,
	 * the Nut token may have expired. So we will proactively reload the login page with a fresh QR code periodically.
	 *
	 * The 2nd scenario is that a user already installed the SQRL client, but has not used it in some time as SQRL may
	 * not be widespread. If the user has trouble remembering their password, there may be a significant delay (minute
	 * or two) from the time the user clicks/scans the QR code and time the SQRL client sends us the request. So, we
	 * should also reload the login page a few minutes <b>before</b> the SQRL nut will timeout. This will ensure there
	 * is a buffer between the time a code can be scanned/clicked and the Nut token timeout
	 *
	 * @throws SqrlException
	 *
	 */
	private SqrlLoginPageStatus checkForCloseToNutExpiry(final HttpServletRequest request,
			final HttpServletResponse response) throws SqrlException {
		// Determine how much time is left before FIRST_NUT expires
		final long nutExpiry = sqrlServerOperations.determineNutExpiry(request);
		final long timeLeftBeforeNutExpires = nutExpiry - System.currentTimeMillis();
		if (logger.isTraceEnabled()) {
			final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
			logger.trace("nutExpiry={}, now={}, timeLeftBeforeNutExpires={}", dateFormatter.format(new Date(nutExpiry)),
					dateFormatter.format(new Date()), timeLeftBeforeNutExpires);
		}
		if (timeLeftBeforeNutExpires < QR_REFERSH_THRESHOLD) {
			logger.info(
					"checkForCloseToNutExpiry triggered, generating new login page because timeLeftBeforeNutExpires={}",
					timeLeftBeforeNutExpires);
			// Kill our session so the next request will generate a new one
			Util.deleteAllCookies(request, response);
			return SqrlLoginPageStatus.CLOSE_TO_SESSION_TIMEOUT;
		} else {
			return null;
		}
	}

	/**
	 * Sometimes, when the time between SQRL clients query and ident messages very fast, the users browser gets stuck on
	 * the "authenticating" spinner. Once we get the SQRL_AUTHENTICATED status, there should be no more requests here.
	 * If there are, send an alternate value so the page will refresh
	 *
	 * @return an authenticated status if the user is authenticated, or null if they aren't
	 */
	private SqrlLoginPageStatus checkForAuthenticatedButNeedsRefresh(final SqrlLoginPageStatus previousStatus) {
		if (previousStatus == null) {
			return null;
		} else if (previousStatus.equals(SqrlLoginPageStatus.SQRL_AUTHENTICATED_ALT)) {
			return SqrlLoginPageStatus.SQRL_AUTHENTICATED;
		} else if (previousStatus.equals(SqrlLoginPageStatus.SQRL_AUTHENTICATED)) {
			return SqrlLoginPageStatus.SQRL_AUTHENTICATED_ALT;
		}
		// Neither
		return null;
	}
}
