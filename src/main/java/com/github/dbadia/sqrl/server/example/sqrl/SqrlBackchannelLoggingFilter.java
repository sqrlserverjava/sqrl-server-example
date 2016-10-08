package com.github.dbadia.sqrl.server.example.sqrl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(filterName = "SqrlBackchannelLoggingFilter", urlPatterns = { "/sqrlbc" })
public class SqrlBackchannelLoggingFilter implements Filter {
	private final static Logger logger = LoggerFactory.getLogger(SqrlBackchannelLoggingFilter.class);

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		// Always log request params
		final StringBuilder buf = new StringBuilder("============ Received from SQRL client ");
		final String sqrlAgentString = ((HttpServletRequest) request).getHeader("user-agent");
		buf.append("'").append(sqrlAgentString).append("'   ");
		for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			buf.append(entry.getKey()).append("=").append(Arrays.toString(entry.getValue())).append("   ");
		}
		logger.error(buf.toString());
		chain.doFilter(request, response);
	}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		// Nothing to do
	}

	@Override
	public void destroy() {
		// Nothing to do
	}
}
