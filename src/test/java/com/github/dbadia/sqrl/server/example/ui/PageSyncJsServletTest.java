package com.github.dbadia.sqrl.server.example.ui;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.github.dbadia.sqrl.server.data.SqrlJpaPersistenceProvider;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.example.ui.PageSyncJsServlet;

import junitx.framework.ObjectAssert;

public class PageSyncJsServletTest {
	private static final String	IDK_VALUE			= "xyz";
	private static final String	CORRELATOR_VALUE	= "abcd";
	private static final String	FIRST_NUT_VALUE		= "QwJJFrvH1jBXakjOh_vVqg";

	private static final Date	FIVE_MINUTES_FROM_NOW	= new Date(
			System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
	private static final String	COOKIE_CORRELATOR		= "sqrlcorrelator";
	private static final String	COOKIE_FIRST_NUT		= "sqrlfirstnut";

	private final SqrlJpaPersistenceProvider	persistence	= new SqrlJpaPersistenceProvider();
	private MockHttpServletRequest				request;
	private MockHttpServletResponse				response;
	private PageSyncJsServlet					servlet;

	@Before
	public void setup() throws Exception {
		request = new MockHttpServletRequest();
		final Cookie correlatorCookie = Util.createCookie(request, COOKIE_CORRELATOR, CORRELATOR_VALUE);
		final Cookie firstNutCookie = Util.createCookie(request, COOKIE_FIRST_NUT, FIRST_NUT_VALUE);
		request.setCookies(correlatorCookie, firstNutCookie);
		response = new MockHttpServletResponse();

		TCUtil.resetDatabase();
		servlet = new PageSyncJsServlet();
	}

	@Test
	@Ignore
	public void testNothingHappeningYet() throws Exception {
		// Login page rendered, session inited in setUp, then trigger the call to pagesync.js
		servlet.doGet(request, response);
		final String result = response.getContentAsString();
		assertNotNull(result);

		// Make another request. It should match the last one since nothing has changed
		response = new MockHttpServletResponse();
		servlet.doGet(request, response);
		final String result2 = response.getContentAsString();
		// Nothing has changed so the result should be the same
		assertNotNull(result2);
		assertEquals(result, result2);
	}

	@Test
	@Ignore
	public void testAutheticationInProgress() throws Exception {
		// Login page rendered, session inited in setUp, then trigger the call to pagesync.js
		servlet.doGet(request, response);
		final String result = response.getContentAsString();
		assertNotNull(result);

		// Simulate an authentication in progress
		persistence.markTokenAsUsed(CORRELATOR_VALUE, FIRST_NUT_VALUE, FIVE_MINUTES_FROM_NOW);

		// Make another request
		response = new MockHttpServletResponse();
		servlet.doGet(request, response);
		final String result2 = response.getContentAsString();
		assertNotNull(result2);
		// Return string should change since the user is authenticated
		ObjectAssert.assertNotSame(result, result2);
	}

	@Test
	@Ignore
	public void testAutheticated() throws Exception {
		// Login page rendered, session inited in setUp, then trigger the call to pagesync.js
		// Simulate an authentication in progress
		persistence.markTokenAsUsed(CORRELATOR_VALUE, FIRST_NUT_VALUE, FIVE_MINUTES_FROM_NOW);
		servlet.doGet(request, response);
		final String result = response.getContentAsString();
		assertNotNull(result);

		// Simulate the authentication completing
		persistence.storeSqrlDataForSqrlIdentity(IDK_VALUE, Collections.EMPTY_MAP);
		persistence.userAuthenticatedViaSqrl(IDK_VALUE, CORRELATOR_VALUE);

		// Make another request. It should match the last one since nothing has changed
		response = new MockHttpServletResponse();
		servlet.doGet(request, response);
		final String result2 = response.getContentAsString();
		// Nothing has changed so the result should be the same
		assertNotNull(result2);
		ObjectAssert.assertNotSame(result, result2);
	}

	@Test
	@Ignore
	public void testNothingHappeningYet_TimeToRerenderLoginPage() throws Exception {
		// Login page rendered, session inited in setUp, then trigger the call to pagesync.js
		servlet.doGet(request, response);
		final String result = response.getContentAsString();
		assertNotNull(result);

		// Simulate us getting near session expiration
		// final MockHttpSession session = (MockHttpSession) request.getSession();
		// session.setAttribute(Constants.SESSION_SQRL_FIRST_NUT_EXPIRY,
		// System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));

		// Make another request. It should trigger the generation of a new login page and kill the session
		response = new MockHttpServletResponse();
		servlet.doGet(request, response);
		final String result2 = response.getContentAsString();
		// Nothing has changed so the result should be the same
		assertNotNull(result2);
		ObjectAssert.assertNotSame(result, result2);
		// assertTrue(session.isInvalid());
		// assertOnWhat
	}

}