package com.github.sqrlserverjava.example.ui;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlBrowserFacingOperations;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.example.ErrorId;
import com.github.sqrlserverjava.example.Util;
import com.github.sqrlserverjava.util.SqrlConfigHelper;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * Servlet which handles logout requests
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/logout" })
public class ProcessLogoutServlet extends HttpServlet {
	private static final long serialVersionUID = 2107859031515432927L;
	private static final Logger logger = LoggerFactory.getLogger(ProcessLogoutServlet.class);
	private final SqrlBrowserFacingOperations sqrlbrowserFacingOperations = new SqrlServerOperations(
			SqrlConfigHelper.loadFromClasspath()).browserFacingOperations();

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
			final HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			sqrlbrowserFacingOperations.deleteSqrlAuthCookies(request, response);
			Util.deleteAllCookies(request, response);
			response.setStatus(302);
			response.setHeader("Location", "login");
		} catch (final Exception e) {
			logger.error("Error in LinkAccountServlet", e);
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
		}
	}
}
