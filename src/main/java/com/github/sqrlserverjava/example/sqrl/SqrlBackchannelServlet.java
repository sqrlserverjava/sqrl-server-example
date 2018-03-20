package com.github.sqrlserverjava.example.sqrl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlClientFacingOperations;
import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.util.SqrlConfigHelper;

/**
 * The backchannel servlet will handle SQRL client calls only. No user side html is served from here.
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/sqrlbc" })
public class SqrlBackchannelServlet extends HttpServlet {
	private static final long serialVersionUID = -5867534423636409159L;
	private static final Logger logger = LoggerFactory.getLogger(SqrlBackchannelServlet.class);
	private static final AtomicBoolean initialized = new AtomicBoolean(false);

	private static SqrlClientFacingOperations sqrlClientFacingOps = null;

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if (!initialized.get()) {
				initialize();
			}
			sqrlClientFacingOps.handleSqrlClientRequest(request, response);
		} catch (final SqrlException e) {
			logger.error("Error occured trying to process SQRL client request", e);
		}
	}

	private synchronized void initialize() throws SqrlException {
		if (!initialized.get()) {
			final SqrlConfig sqrlConfig = SqrlConfigHelper.loadFromClasspath();
			sqrlClientFacingOps = new SqrlServerOperations(sqrlConfig).clientFacingOperations();
			initialized.set(true);
		}
	}

}
