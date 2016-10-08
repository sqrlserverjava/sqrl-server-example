package com.github.dbadia.sqrl.server.example.ui;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(filterName = "NoCacheServletFilter", urlPatterns = { "/*" }, asyncSupported = true)
public class NoCacheServletFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(NoCacheServletFilter.class);

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		// Now perform no cache stuff
		final String path = ((HttpServletRequest) request).getRequestURI();
		if (!path.endsWith("/sqrlauto") && !path.endsWith(".js")) {
			final HttpServletResponse httpResponse = (HttpServletResponse) response;
			// http://stackoverflow.com/questions/3413036/http-response-caching
			httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
			httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			httpResponse.setDateHeader("Expires", 0); // Proxies.
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}
}
