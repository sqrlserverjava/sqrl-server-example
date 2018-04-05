package com.github.sqrlserverjava.example.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlAuthPageData;
import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.example.Constants;
import com.github.sqrlserverjava.example.ErrorId;
import com.github.sqrlserverjava.example.Util;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.util.SqrlConfigHelper;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * Servlet renders the login page by perparing data then forwarding to login.jsp
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/login" })
public class RenderLoginPageServlet extends HttpServlet {
	private static final long serialVersionUID = -849809318695746441L;

	private static final Logger		logger					= LoggerFactory.getLogger(RenderLoginPageServlet.class);
	private SqrlConfig				sqrlConfig				= null;
	private SqrlServerOperations	sqrlServerOperations	= null;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		logger.info(SqrlUtil.logEnterServlet(request));
		try {
			checkInit();
			displayLoginPage(request, response);
		} catch (final RuntimeException e) {
			logger.error("Error rendering login page", e);
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
		}
	}

	public static void redirectToLoginPageWithError(final HttpServletResponse response, ErrorId errorId) {
		if (errorId == null) {
			errorId = ErrorId.GENERIC;
		}
		response.setHeader("Location", "login?error=" + errorId.getId());
		response.setStatus(302);
	}

	private void displayLoginPage(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute(Constants.JSP_SUBTITLE, "Login Page");
		// Default action, show the login page with a new SQRL QR code
		try {
			final SqrlAuthPageData pageData = sqrlServerOperations.browserFacingOperations()
					.prepareSqrlAuthPageData(request, response, 175);
			final ByteArrayOutputStream baos = pageData.getQrCodeOutputStream();
			baos.flush();
			final byte[] imageInByteArray = baos.toByteArray();
			baos.close();
			// Since this is being passed to the browser, we use regular Base64 encoding, NOT SQRL specific
			// Base64URL encoding
			final String b64 = new StringBuilder("data:image/").append(pageData.getHtmlFileType(sqrlConfig))
					.append(";base64, ").append(Base64.getEncoder().encodeToString(imageInByteArray)).toString();
			// TODO_DOC add doc FAQ link
			final int pageRefreshSeconds = sqrlConfig.getNutValidityInSeconds() / 2;
			request.setAttribute(Constants.JSP_PAGE_REFRESH_SECONDS, Integer.toString(pageRefreshSeconds));
			request.setAttribute("sqrlqr64", b64);
			final String sqrlUrl = pageData.getUrl().toString();
			request.setAttribute("sqrlurl", sqrlUrl);
			request.setAttribute("cpsEnabled", Boolean.toString(sqrlConfig.isEnableCps()));
			// The url that will get sent to the SQRL client via CPS must include a cancel page (can) if case of failure
			final String sqrlurlWithCan = sqrlUrl;
			request.setAttribute("sqrlurlwithcan64", SqrlUtil.sqrlBase64UrlEncode(sqrlurlWithCan));
			request.setAttribute("sqrlqrdesc", "Mobile SQRL - scan QR code with SQRL app");
			request.setAttribute("correlator", pageData.getCorrelator());
			logger.info("Showing login page with correlator={}, sqrlurl={}", pageData.getCorrelator(),
					sqrlurlWithCan);
			checkForErrorState(request, response);
		} catch (final Throwable e) { // need to catch everything, NoClassDefError etc so we don't end up looping
			logger.error("Error rendering login page", e);
			displayErrorAndKillSession(request, "Rendering error", true);
		}
		request.getRequestDispatcher("WEB-INF/login.jsp").forward(request, response);
	}

	private void checkForErrorState(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException, SqrlException {
		final String errorParam = request.getParameter("error");
		if (Util.isBlank(errorParam)) {
			return;
		}
		ErrorId errorId = ErrorId.lookup(errorParam);
		// If we have access to the correlator, append the first 5 chars to the message in case it gets reported
		final String correlatorString = sqrlServerOperations.extractSqrlCorrelatorStringFromRequestCookie(request);
		String errorMessage = errorId.buildErrorMessage(correlatorString);

		displayErrorAndKillSession(request, errorMessage, errorId.isDisplayInRed());
	}

	private void displayErrorAndKillSession(final HttpServletRequest request, final String errorText,
			boolean displayInRed) {
		// Set it so it gets displayed
		String content = errorText;
		if (displayInRed) {
			content = Util.wrapErrorInRed(errorText);
		}
		request.setAttribute(Constants.JSP_SUBTITLE, content);
		// Since we are in an error state, kill the session
		final HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}

	private void checkInit() {
		if (sqrlConfig == null) {
			sqrlConfig = SqrlConfigHelper.loadFromClasspath();
		}
		if (sqrlServerOperations == null && sqrlConfig != null) {
			sqrlServerOperations = new SqrlServerOperations(sqrlConfig);
		}
	}
}
