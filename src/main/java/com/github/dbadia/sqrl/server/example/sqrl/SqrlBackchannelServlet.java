package com.github.dbadia.sqrl.server.example.sqrl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.util.SqrlConfigHelper;
import com.github.dbadia.sqrl.server.util.SqrlException;
import com.github.dbadia.sqrl.server.util.SqrlUtil;

/**
 * The backchannel servlet will handle SQRL client calls only. No user side html is served from here.
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/sqrlbc" })
public class SqrlBackchannelServlet extends HttpServlet {
	private static final long			serialVersionUID	= -5867534423636409159L;
	private static final Logger			logger				= LoggerFactory.getLogger(SqrlBackchannelServlet.class);
	private static final AtomicBoolean	initialized			= new AtomicBoolean(false);

	private static SqrlServerOperations	sqrlServerOps	= null;
	private static SqrlConfig			sqrlConfig;

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		try {
			initializeIfNecessary();
			logger.info(SqrlUtil.logEnterServlet(request));
			sqrlServerOps.handleSqrlClientRequest(request, response);
		} catch (final SqrlException e) {
			logger.error("Error occured trying to process SQRL client request", e);
		}
	}

	private void initializeIfNecessary() throws SqrlException {
		if (!initialized.get()) {
			sqrlConfig = SqrlConfigHelper.loadFromClasspath();
			sqrlServerOps = new SqrlServerOperations(sqrlConfig);
			initialized.set(true);
		}
	}

}
