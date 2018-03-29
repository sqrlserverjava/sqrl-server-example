package com.github.sqrlserverjava.example.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

import com.github.sqrlserverjava.util.VersionExtractor;
import com.github.sqrlserverjava.util.VersionExtractor.Module;

@WebFilter(filterName = "NoCacheServletFilter", urlPatterns = { "/*" }, asyncSupported = true)
public class NoCacheServletFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(NoCacheServletFilter.class);

	private static String		buildNumber	= "x";

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		logger.info(VersionExtractor.extractDetailedBuildInfo(Module.EXAMPLE_APP));
		// Load our simple build number for display on the web app
		try (InputStream is = filterConfig.getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF")) {
			Properties properties = new Properties();
			properties.load(is);
			String tempBuild = properties.getProperty("Implementation-Build");
			if (tempBuild != null) {
				buildNumber = tempBuild;
			}
		} catch (RuntimeException | IOException e) {
			logger.error("Caught Exception during build number check", e);
		}
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
		// Set our build number as well
		request.setAttribute("build", buildNumber);
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}
}
