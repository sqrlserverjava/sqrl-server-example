package com.github.dbadia.sqrl.server.example;

import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;

/**
 * To prevent XSS attacks, errors are passed as codes in the query string.
 * We then map those codes to messages here
 */
public enum ErrorId {
	// @formatter:off
	GENERIC(""),
	SQRL_BAD_REQUEST("Invalid SQRL request"),
	MISSING_PARAM_FOR_NEW_USER("Missing paramater for new user"),
	ATTRIBUTES_NOT_FOUND("Attributes not found"),
	/**
	 * This matches {@link SqrlAuthenticationStatus#ERROR_SQRL_INTERNAL}
	 */
	ERROR_SQRL_INTERNAL("Error processing SQRL request"),
	/**
	 * This matches {@link SqrlAuthenticationStatus#ERROR_BAD_REQUEST}
	 */
	ERROR_BAD_REQUEST("SQRL client sent invalid request"),
	SYSTEM_ERROR("System error"),
	INVALID_USERNAME_OR_PASSWORD("Invalid username or password"),
	;
	// @formatter:on

	private final String errorMessage;

	private ErrorId(final String errorMessage) {
		String toSet = errorMessage;
		if(Util.isNotBlank(errorMessage)) {
			toSet = ": "+errorMessage;
		}
		this.errorMessage = toSet;
	}

	public int getId() {
		// Normally it's not safe to use ordinal, but in this case, if it
		// changes the whole app will get recompiled so it's ok
		return this.ordinal();
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public static ErrorId byId(final int id) {
		if(id < 0 || id >= ErrorId.values().length) {
			return ErrorId.GENERIC;
		}
		return ErrorId.values()[id];
	}
}
