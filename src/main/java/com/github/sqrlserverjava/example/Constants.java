package com.github.sqrlserverjava.example;

public class Constants {
	private Constants() {
	}

	private static final String					PACKAGE_NAME		= Constants.class.getPackage().getName();

	public static final String	SESSION_NATIVE_APP_USER	= PACKAGE_NAME + ".nativeAppUser";
	public static final String	SESSION_SQRL_IDENTITY	= PACKAGE_NAME + ".sqrlIdentity";

	public static final int		MAX_LENGTH_GIVEN_NAME			= 10;
	public static final int		MAX_LENGTH_WELCOME_PHRASE		= 40;
	public static final int		SQRL_LAST_LOGIN_WINDOW_SECONDS	= 90;
	public static final String	REGISTRATION_IN_PROGRESS		= "rip";
	public static final String	JSP_SUBTITLE					= "subtitle";
	public static final String	JSP_PAGE_REFRESH_SECONDS		= "pageRefreshSeconds";
	public static final String	PASSWORD_FOR_ALL_USERS			= "sqrl";
	public static final String	APP_PERSISTENCE_UNIT_NAME		= "exampleapp-persistence";

}
