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

import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.example.Constants;
import com.github.sqrlserverjava.example.ErrorId;
import com.github.sqrlserverjava.example.Util;
import com.github.sqrlserverjava.example.data.AppDatastore;
import com.github.sqrlserverjava.example.data.AppUser;
import com.github.sqrlserverjava.persistence.SqrlIdentity;
import com.github.sqrlserverjava.util.SqrlConfigHelper;
import com.github.sqrlserverjava.util.SqrlUtil;

@WebServlet(urlPatterns = { "/linkaccount" })
public class ProcessLinkAccountServlet extends HttpServlet {
	private static final long serialVersionUID = 5609899766821704630L;

	private static final Logger			logger					= LoggerFactory.getLogger(ProcessLinkAccountServlet.class);
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
		request.setAttribute(Constants.JSP_SUBTITLE, "Link Account Option");
		final HttpSession session = request.getSession(true);
		try {
			final AppUser user = validateRequestAndAuthenticateAppUser(request, response);
			if (user == null) {
				// validateRequestAndAuthenticateAppUser set error text and forwarded as needed
				return;
			} else {
				// We have a valid user to link
				final SqrlIdentity sqrlIdentity = (SqrlIdentity) session.getAttribute(Constants.SESSION_SQRL_IDENTITY);
				sqrlServerOperations.updateNativeUserXref(sqrlIdentity, Long.toString(user.getId()));
				session.setAttribute(Constants.SESSION_NATIVE_APP_USER, user);
				// All done, send them to the app page
				response.setHeader("Location", "app");
				response.setStatus(302); // we use 302 to make it easy to understand what the example app is doing, but
				// a real app might do a server side redirect instead
				return;
			}
		} catch (final RuntimeException | SQLException e) {
			logger.error("Error in LinkAccountServlet", e);
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
		}
	}

	private AppUser validateRequestAndAuthenticateAppUser(final HttpServletRequest request,
			final HttpServletResponse response) throws SQLException, ServletException, IOException {
		if (Util.isBlank(request.getParameter("username")) && Util.isBlank(request.getParameter("password"))) {
			request.setAttribute(Constants.JSP_SUBTITLE, Util.wrapErrorInRed("Invalid username or password"));
			request.getRequestDispatcher("WEB-INF/linkaccountoption.jsp").forward(request, response);
			return null;
		}
		// Check for login credentials
		final String username = Util.sanitizeString(request.getParameter("username"), Constants.MAX_LENGTH_GIVEN_NAME);
		final String password = Util.sanitizeString(request.getParameter("password"), Constants.MAX_LENGTH_GIVEN_NAME);

		if (!password.equals(Constants.PASSWORD_FOR_ALL_USERS)) {
			request.setAttribute(Constants.JSP_SUBTITLE, Util.wrapErrorInRed("Invalid username or password"));
			request.getRequestDispatcher("WEB-INF/linkaccountoption.jsp").forward(request, response);
			return null;
		}

		final AppUser appUser = AppDatastore.getInstance().fetchUserByUsername(username);
		if(appUser == null) {
			// No such user
			request.setAttribute(Constants.JSP_SUBTITLE, Util.wrapErrorInRed("Invalid username or password"));
			request.getRequestDispatcher("WEB-INF/linkaccountoption.jsp").forward(request, response);
			return null;
		} else if (sqrlServerOperations.fetchSqrlIdentityByUserXref(Long.toString(appUser.getId())) != null) {
			request.setAttribute(Constants.JSP_SUBTITLE,
					Util.wrapErrorInRed(" Another SQRL ID has already been linked to this username"));
			request.getRequestDispatcher("WEB-INF/linkaccountoption.jsp").forward(request, response);
			return null;
		} else {
			// All good, let the user by linked
			return appUser;
		}
	}

}
