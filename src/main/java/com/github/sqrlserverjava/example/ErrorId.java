package com.github.sqrlserverjava.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;

/**
 * To prevent XSS attacks, errors are passed as codes in the query string. We then map those codes to messages here
 */
public enum ErrorId {
	// @formatter:off
	GENERIC(1, ""),
	SQRL_BAD_REQUEST(2, "Invalid SQRL request"),
	MISSING_PARAM_FOR_NEW_USER(3, "Missing paramater for new user"),
	ATTRIBUTES_NOT_FOUND(4, "Attributes not found"),
	/**
	 * This matches {@link SqrlAuthenticationStatus#ERROR_SQRL_INTERNAL}
	 */
	ERROR_SQRL_INTERNAL(5, "SQRL error"),
	/**
	 * This matches {@link SqrlAuthenticationStatus#ERROR_BAD_REQUEST}
	 */
	ERROR_BAD_REQUEST(6, "SQRL client sent invalid request"),
	/**
	 * This matches {@link SqrlAuthenticationStatus#ERROR_SQRL_USER_DISABLED}
	 */
	SQRL_USER_DISABLED(7, "SQRL user is disabled", false, false),
	/**
	 * When CPS is enabled, this will be passed via the "can" param 
	 */
	SQRL_AUTH_CANCELLED(8, "SQRL authentication cancelled by user", false, false),
	SYSTEM_ERROR(9, "System error"),
	INVALID_USERNAME_OR_PASSWORD(10, "Invalid username or password", false, true),
	;
	// @formatter:on

	private static final Logger					logger			= LoggerFactory.getLogger(ErrorId.class);
	private static final Map<Integer, ErrorId>	NUMBER_TABLE	= new ConcurrentHashMap<>();
	private static final Map<String, ErrorId>	MESSAGE_TABLE	= new ConcurrentHashMap<>();
	private final String						errorMessage;
	private final int							errorId;
	private final boolean						showCorrelatorIfAvailable;
	private final boolean						displayInRed;

	private ErrorId(int errorId, final String errorMessage) {
		this(errorId, errorMessage, true, true);
	}

	private ErrorId(int errorId, final String errorMessage, boolean showCorrelatorIfAvailable, boolean displayInRed) {
		String toSet = errorMessage;
		if (Util.isNotBlank(errorMessage)) {
			toSet = errorMessage;
		}
		this.errorId = errorId;
		this.errorMessage = toSet;
		this.showCorrelatorIfAvailable = showCorrelatorIfAvailable;
		this.displayInRed = displayInRed;
	}

	public int getId() {
		return errorId;
	}

	public String buildErrorMessage(String correlatorString) {
		final StringBuilder buf = new StringBuilder(100);
		if (displayInRed) {
			buf.append("Error: ");
		}
		buf.append(errorMessage).append(".");
		if (showCorrelatorIfAvailable && Util.isNotBlank(correlatorString)) {
			buf.append("    code=" + correlatorString.substring(0, 5));
		}
		return errorMessage;
	}

	public boolean isDisplayInRed() {
		return displayInRed;
	}

	/**
	 * @param errorParam
	 *            can be an error string (starting with ERROR_) or an error ID
	 * @return the error message to be displayed
	 */
	public static ErrorId lookup(String errorParam) {
		if (errorParam.startsWith("ERROR_")) {
			return ErrorId.byIdString(errorParam);
		} else {
			return ErrorId.byIdNumber(errorParam);
		}
	}

	private static ErrorId byIdNumber(final String idString) {
		if (NUMBER_TABLE.isEmpty()) {
			for (ErrorId errorId : ErrorId.values()) {
				NUMBER_TABLE.put(errorId.getId(), errorId);
			}
		}
		ErrorId result = ErrorId.GENERIC;
		try {
			int id = Integer.parseInt(idString);
			result = NUMBER_TABLE.get(id);
		} catch (NumberFormatException e) {
			logger.info("Error converting error ID to number: " + idString);
		}
		if (result == null) {
			return ErrorId.GENERIC;
		}
		return result;
	}

	private static ErrorId byIdString(String errorParam) {
		if (MESSAGE_TABLE.isEmpty()) {
			for (ErrorId errorId : ErrorId.values()) {
				MESSAGE_TABLE.put(errorId.errorMessage, errorId);
			}
		}
		ErrorId result = MESSAGE_TABLE.get(errorParam);
		if (result == null) {
			result = ErrorId.GENERIC;
		}
		return result;
	}

}
