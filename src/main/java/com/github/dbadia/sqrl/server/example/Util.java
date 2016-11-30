package com.github.dbadia.sqrl.server.example;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	public static final Logger logger = LoggerFactory.getLogger(Util.class);

	private Util() {
	}

	public static String wrapErrorInRed(final String errorMessage) {
		final StringBuilder buf = new StringBuilder("<font color='red'>");
		buf.append(errorMessage);
		buf.append("</font></p>");
		return buf.toString();
	}

	public static String sanitizeString(final String rawString, final int maxLength) {
		if (rawString == null) {
			return "<not set>";
		}
		// We accept letters and numbers only, strip anything else out
		String finalPrase = rawString.replaceAll("[^a-zA-Z0-9]", "");
		// Truncate to 80 characters
		if (finalPrase.length() > maxLength) {
			finalPrase = finalPrase.substring(0, maxLength);
		}
		return finalPrase;
	}

	public static boolean isBlank(final String string) {
		return (string == null || string.trim().length() == 0);
	}

	public static boolean isNotBlank(final String string) {
		return !isBlank(string);
	}

	/**
	 * @deprecated read this isn't safe
	 */
	@Deprecated
	public static Date convertDate(final LocalDateTime deleteAfter) {
		return Date.from(deleteAfter.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static Cookie createCookie(final HttpServletRequest request, final String name, final String value) {
		final Cookie cookie = new Cookie(name, value);
		if (request.getScheme().equals("https")) {
			cookie.setSecure(true);
		}
		cookie.setHttpOnly(true);
		cookie.setMaxAge(-1);
		return cookie;
	}

	public static String getCookieValue(final HttpServletRequest request, final String toFind) {
		if (request.getCookies() == null) {
			return null;
		}
		for (final Cookie cookie : request.getCookies()) {
			if (toFind.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static void deleteAllCookies(final HttpServletRequest request, final HttpServletResponse response) {
		if (request == null || request.getCookies() == null) {
			return;
		}
		for (final Cookie cookie : request.getCookies()) {
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		}
	}

	public static void deleteCookies(final HttpServletRequest request, final HttpServletResponse response,
			final String... cookiesToDelete) {
		final List<String> cookieToDeleteList = Arrays.asList(cookiesToDelete);
		for (final Cookie cookie : request.getCookies()) {
			if (cookieToDeleteList.contains(cookie.getName())) {
				cookie.setMaxAge(0);
				response.addCookie(cookie);
			}
		}
	}

	public static String cookiesToString(final Cookie[] cookieArray) {
		final StringBuilder buf = new StringBuilder();
		if (cookieArray != null) {
			for (final Cookie cookie : cookieArray) {
				buf.append(cookie.getName()).append("=").append(cookie.getValue()).append("  ");
			}
		}
		return buf.toString();
	}
}
